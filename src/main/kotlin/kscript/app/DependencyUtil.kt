package kscript.app

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenRepositoryCoordinates


val DEP_LOOKUP_CACHE_FILE = File(KSCRIPT_CACHE_DIR, "dependency_cache.txt")

val CP_SEPARATOR_CHAR = if (System.getProperty("os.name").toLowerCase().contains("windows")) ";" else ":"


fun resolveDependencies(depIds: List<String>, customRepos: List<MavenRepo> = emptyList(), loggingEnabled: Boolean): String? {

    // if no dependencies were provided we stop here
    if (depIds.isEmpty()) {
        return null
    }

    val depsHash = depIds.joinToString(CP_SEPARATOR_CHAR)


    // Use cached classpath from previous run if present
    if (DEP_LOOKUP_CACHE_FILE.isFile()) {
        val cache = DEP_LOOKUP_CACHE_FILE
                .readLines()
                .filter { it.isNotBlank() }
                .associateBy({ it.split(" ")[0] }, { it.split(" ")[1] })

        if (cache.containsKey(depsHash)) {
            val cachedCP = cache.get(depsHash)!!


            // Make sure that local dependencies have not been wiped since resolving them (like by deleting .m2) (see #146)
            if (cachedCP.split(CP_SEPARATOR_CHAR).all { File(it).exists() }) {
                return cachedCP
            } else {
                System.err.println("[kscript] Detected missing dependencies in cache.")
            }
        }
    }


    if (loggingEnabled) infoMsg("Resolving dependencies...")

    try {
        val artifacts = resolveDependenciesViaKotlin(depIds, customRepos, loggingEnabled)
        val classPath = artifacts.map { it.absolutePath }.joinToString(CP_SEPARATOR_CHAR)

        if (loggingEnabled) infoMsg("Dependencies resolved")

        // Add classpath to cache
        DEP_LOOKUP_CACHE_FILE.appendText(depsHash + " " + classPath + "\n")

        // Print the classpath
        return classPath
    } catch (e: Exception) {
        // Probably a wrapped Nullpointer from 'DefaultRepositorySystem.resolveDependencies()', this however is probably a connection problem.
        errorMsg("Failed while connecting to the server. Check the connection (http/https, port, proxy, credentials, etc.) of your maven dependency locators. If you suspect this is a bug, you can create an issue on https://github.com/holgerbrandl/kscript")
        errorMsg("Exception: $e")
        quit(1)
    }
}

fun decodeEnv(value: String): String {
    return if (value.startsWith("{{") && value.endsWith("}}")) {
        val envKey = value.substring(2, value.length - 2)
        val envValue = System.getenv()[envKey]
        if (null == envValue) {
            errorMsg("Could not resolve environment variable {{$envKey}} in maven repository credentials")
            quit(1)
        }
        envValue
    } else {
        value
    }
}

fun resolveDependenciesViaKotlin(depIds: List<String>, customRepos: List<MavenRepo>, loggingEnabled: Boolean): List<File> {

    // validate dependencies
    depIds.map { depIdToArtifact(it) }

    val extRepos = customRepos //+ MavenRepo("jcenter", "https://jcenter.bintray.com")

    val repoCoords = extRepos.map { MavenRepositoryCoordinates(it.url, it.user, it.password, null, null) }

    val mvnResolver = MavenDependenciesResolver().apply {
        repoCoords.map { addRepository(it)}
    }

    val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), mvnResolver)

    val resolvedDependencies = runBlocking {
        depIds.map {
            if (loggingEnabled) System.err.println("[kscript] Resolving $it...")
            resolver.resolve(it)
        }.map { it.valueOrThrow() }
    }.flatten()

    return resolvedDependencies
}


fun depIdToArtifact(depId: String) {
    val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
    val matchResult = regex.find(depId)

    if (matchResult == null) {
        System.err.println("[ERROR] Invalid dependency locator: '${depId}'.  Expected format is groupId:artifactId:version[:classifier][@type]")
        quit(1)
    }

//    val groupId = matchResult.groupValues[1]
//    val artifactId = matchResult.groupValues[2]
//    val version = formatVersion(matchResult.groupValues[3])
//    val classifier = matchResult.groups[5]?.value
//    val type = matchResult.groups[7]?.value ?: "jar"

//    return DefaultArtifact(groupId, artifactId, classifier, type, version)
}

fun formatVersion(version: String): String {
    // replace + with open version range for maven
    return version.let { it ->
        if (it.endsWith("+")) {
            "[${it.dropLast(1)},)"
        } else {
            it
        }
    }
}

// called by unit tests
object DependencyUtil {
    @JvmStatic
    fun main(args: Array<String>) {
        System.err.println(resolveDependencies(args.toList(), loggingEnabled = false))
    }
}
