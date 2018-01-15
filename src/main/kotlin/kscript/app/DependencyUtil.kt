package kscript.app

import com.jcabi.aether.Aether
import org.sonatype.aether.RepositoryException
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.util.artifact.JavaScopes.COMPILE
import java.io.File


val DEP_LOOKUP_CACHE_FILE = File(KSCRIPT_CACHE_DIR, "dependency_cache.txt")

val CP_SEPARATOR_CHAR = if (IS_WINDOWS) ";" else ":"


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
        val artifacts = resolveDependenciesViaAether(depIds, customRepos, loggingEnabled)
        val classPath = artifacts.map { it.file.absolutePath }.joinToString(CP_SEPARATOR_CHAR)

        if (loggingEnabled) infoMsg("Dependencies resolved")

        // Add classpath to cache
        DEP_LOOKUP_CACHE_FILE.appendText(depsHash + " " + classPath + "\n")

        // Print the classpath
        return classPath
    } catch (e: RepositoryException) {
        errorMsg("Failed to lookup dependencies. Check dependency locators or file a bug on https://github.com/holgerbrandl/kscript")
        errorMsg("Exception: $e")
        quit(1)
    }
}

fun resolveDependenciesViaAether(depIds: List<String>, customRepos: List<MavenRepo>, loggingEnabled: Boolean): List<Artifact> {
    val jcenter = RemoteRepository("jcenter", "default", "http://jcenter.bintray.com/")
    val customRemoteRepos = customRepos.map { it -> RemoteRepository(it.id, "default", it.url) }
    val remoteRepos = customRemoteRepos + jcenter

    val aether = Aether(remoteRepos, File(System.getProperty("user.home") + "/.m2/repository"))
    return depIds.flatMap {
        if (loggingEnabled) System.err.print("[kscript]     Resolving $it...")

        val artifacts = aether.resolve(depIdToArtifact(it), COMPILE)

        if (loggingEnabled) System.err.println("Done")

        artifacts
    }
}

fun depIdToArtifact(depId: String): Artifact {
    val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
    val matchResult = regex.find(depId)

    if (matchResult == null) {
        System.err.println("[ERROR] Invalid dependency locator: '${depId}'.  Expected format is groupId:artifactId:version[:classifier][@type]")
        quit(1)
    }

    val groupId = matchResult.groupValues[1]
    val artifactId = matchResult.groupValues[2]
    val version = formatVersion(matchResult.groupValues[3])
    val classifier = matchResult.groups[5]?.value
    val type = matchResult.groups[7]?.value ?: "jar"

    return DefaultArtifact(groupId, artifactId, classifier, type, version)
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