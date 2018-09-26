package kscript.app

import kscript.app.ShellUtils.requireInPath
import java.io.File


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
            return cache.get(depsHash)
        }
    }


    if (loggingEnabled) System.err.print("[kscript] Resolving dependencies...")
    var hasLoggedDownload = false

    fun runMaven(pom: String, goal: String): Iterable<String> {
        val temp = File.createTempFile("__resdeps__temp__", "_pom.xml")
        temp.writeText(pom)

        requireInPath("mvn")

        val mavenCmd = if (System.getenv("PATH").run { this != null && contains("cygwin") }) {
            // when running with cygwin we need to map the pom path into windows space to work
            "mvn -f $(cygpath -w '${temp.absolutePath}') ${goal}"
        } else {
            "mvn -f ${temp.absolutePath} ${goal}"
        }

        return evalBash(mavenCmd, stdoutConsumer = object : StringBuilderConsumer() {
            override fun accept(t: String) {
                super.accept(t)


                // log artifact downloading (see https://github.com/holgerbrandl/kscript/issues/23)
                if (loggingEnabled && t.startsWith("Downloading: ")) {
                    if (!hasLoggedDownload) System.err.println()
                    hasLoggedDownload = true

                    System.err.println("[kscript] " + t)
                }
            }
        }).stdout.lines()
    }

    val pom = buildPom(depIds, customRepos)
    val mavenResult = runMaven(pom, "dependency:build-classpath")


    // The following artifacts could not be resolved: log4ja:log4ja:jar:9.8.87, log4j:log4j:jar:9.8.105: Could not

    // Check for errors (e.g. when using non-existing deps resdeps.kts log4j:log4j:1.2.14 org.org.docopt:org.docopt:22.3-MISSING)
    mavenResult.filter { it.startsWith("[ERROR]") }.find { it.contains("Could not resolve dependencie") }?.let {
        System.err.println("Failed to lookup dependencies. Maven reported the following error:")
        System.err.println(it)

        quit(1)
    }


    // Extract the classpath from the maven output
    val classPath = mavenResult.dropWhile { !it.contains("Dependencies classpath:") }.drop(1).firstOrNull()

    if (classPath == null) {
        errorMsg("Failed to lookup dependencies. Check dependency locators or file a bug on https://github.com/holgerbrandl/kscript")
        System.err.println("[kscript] The error reported by maven was:")
        mavenResult.map { it.prependIndent("[kscript] [mvn] ") }.forEach { System.err.println(it) }

        System.err.println("[kscript] Generated pom file was:")
        pom.lines()
                //            .map{it.prependIndent("[kscript] [pom] ")}
                .forEach { System.err.println(it) }
        quit(1)
    }


    // Add classpath to cache
    if (loggingEnabled && !hasLoggedDownload) {
        System.err.println("Done")
    }

    DEP_LOOKUP_CACHE_FILE.appendText(depsHash + " " + classPath + "\n")

    // Print the classpath
    return classPath
}


internal fun buildPom(depIds: List<String>, customRepos: List<MavenRepo>): String {
    val depTags = depIds.map {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        val matchResult = regex.find(it)

        if (matchResult == null) {
            System.err.println("[ERROR] Invalid dependency locator: '${it}'.  Expected format is groupId:artifactId:version[:classifier][@type]")
            quit(1)
        }

        // replace + with open version range for maven
        val version = matchResult.groupValues[3].let {
            if(it.endsWith("+")) {
                "[${it.dropLast(1)},)"
            } else {
                it
            }
        }

        """
    <dependency>
            <groupId>${matchResult.groupValues[1]}</groupId>
            <artifactId>${matchResult.groupValues[2]}</artifactId>
            <version>$version</version>
            ${matchResult.groups[5]?.let { "<classifier>" + it.value + "</classifier>" } ?: ""}
            ${matchResult.groups[7]?.let { "<type>" + it.value + "</type>" } ?: ""}
    </dependency>
    """
    }

    // see https://github.com/holgerbrandl/kscript/issues/22
    val repoTags = customRepos.map {
        """
    <repository>
            <id>${it.id}</id>
            <url>${it.url}</url>
    </repository>
    """

    }

    return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>kscript</groupId>
    <artifactId>maven_template</artifactId>
    <version>1.0</version>

     <repositories>
        <repository>
            <id>jcenter</id>
            <url>http://jcenter.bintray.com/</url>
        </repository>
        ${repoTags.joinToString("\n")}
    </repositories>

    <dependencies>
    ${depTags.joinToString("\n")}
    </dependencies>
</project>
"""
}


// called by unit tests
object DependencyUtil {
    @JvmStatic
    fun main(args: Array<String>) {
        System.err.println(resolveDependencies(args.toList(), loggingEnabled = false))
    }
}