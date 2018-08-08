package kscript.app

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.net.URL

/**
 * @author Holger Brandl
 * @author Ilan Pillemer
 * @author Casey Brooks
 */

const val PACKAGE_STATEMENT_PREFIX = "package "
const val IMPORT_STATMENT_PREFIX = "import " // todo make more solid by using operator including regex

data class IncludeResult(val scriptFile: File, val includes: List<URL> = emptyList())

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(template: File, includeContext: URI = template.parentFile.toURI()): IncludeResult {
    var script = Script(template)

    val includeables = listOf(IncludeDirective(), IncludeDirDirective(), IncludeDirsDirective())

    // just rewrite user scripts if there are no includes
    if (!script.any { line -> includeables.any { includeable -> includeable.lineMatches(line) }}) {
        return IncludeResult(template)
    }

    val includes = emptyList<URL>().toMutableList()

    // resolve as long as it takes. YAGNI but we do because we can!
    while (script.any { line -> includeables.any { includeable -> includeable.lineMatches(line) }}) {
        script = script.flatMap { line ->
            val includable: Includable? = includeables.find { it.lineMatches(line) }

            val result: Pair<List<URL>, List<String>>
            if(includable != null) {
                result = includable.handleTarget(includable.extractTarget(line), includeContext)
            }
            else {
                result = Pair(emptyList(), listOf(line))
            }

            includes.addAll(result.first)
            result.second
        }.let { script.copy(it) }
    }

    return IncludeResult(script.consolidateStructure().createTmpScript(), includes)
}

/**
 * Base class used for detecting and handling include directives
 */
private abstract class Includable(
        protected val commentPrefix: String,
        protected val annotationPrefix: String
) {

    fun lineMatches(line: String): Boolean {
        return line.startsWith(commentPrefix) || line.startsWith(annotationPrefix)
    }

    fun extractTarget(incDirective: String): String {
        return if (incDirective.startsWith(annotationPrefix)) {
            incDirective
                    .replaceFirst(annotationPrefix, "")
                    .split(")")[0]
                    .trim(' ', '"')
        }
        else {
            incDirective.replaceFirst(commentPrefix, "").trim()
        }
    }

    abstract fun handleTarget(include: String, includeContext: URI): Pair<List<URL>, List<String>>
}

/**
 * Includes a single file. The target can be a relative or absolute file path, or an HTTP URL
 */
private class IncludeDirective : Includable("//INCLUDE ", "@file:Include(") {
    override fun handleTarget(include: String, includeContext: URI): Pair<List<URL>, List<String>> {
        val includeURL = when {
            isUrl(include)          -> URL(include)
            include.startsWith("/") -> File(include).toURI().toURL()
            else                    -> includeContext.resolve(URI(include.removePrefix("./"))).toURL()
        }

        try {
            return Pair(listOf(includeURL), includeURL.readText().lines())
        }
        catch (e: FileNotFoundException) {
            errorMsg("Failed to resolve $commentPrefix'${include}'")
            System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
            quit(1)
        }
    }

    fun isUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")
}

/**
 * Includes all files in a local directory, by replacing the INCLUDE_DIR directive with INCLUDE directives for each file
 * in the target directory. Target must be a relative or absolute file path to a directory. Included files are sorted by
 * absolute path and included in that order.
 */
private class IncludeDirDirective : Includable("//INCLUDE_DIR ", "@file:IncludeDir(") {
    override fun handleTarget(include: String, includeContext: URI): Pair<List<URL>, List<String>> {
        val includeURL = when {
            include.startsWith("/") -> File(include).toURI().toURL()
            else                    -> includeContext.resolve(URI(include.removePrefix("./"))).toURL()
        }

        try {
            val dir = File(includeURL.toURI())
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                        .filter { fileInDir -> fileInDir.isFile }
                        .sortedBy { fileInDir -> fileInDir.absolutePath }

                return Pair(
                        emptyList(),
                        files.map { fileInDir -> "//INCLUDE ${fileInDir.absolutePath}" }
                )
            }
            else {
                errorMsg("Failed to resolve $commentPrefix'$include'")
                System.err.println("'$include' does not exist or it is not a directory".prependIndent("[kscript] [ERROR] "))
                quit(1)
            }
        }
        catch (e: FileNotFoundException) {
            errorMsg("Failed to resolve $commentPrefix'$include'")
            System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
            quit(1)
        }
    }
}

/**
 * Recursively includes all files in a local directory, by replacing the INCLUDE_DIRS directive with INCLUDE_DIR
 * directives for each directory in the target directory. Target must be a relative or absolute file path to a
 * directory. Included directories are sorted by absolute path and included in that order.
 */
private class IncludeDirsDirective : Includable("//INCLUDE_DIRS ", "@file:IncludeDirs(") {
    override fun handleTarget(include: String, includeContext: URI): Pair<List<URL>, List<String>> {
        val includeURL = when {
            include.startsWith("/") -> File(include).toURI().toURL()
            else                    -> includeContext.resolve(URI(include.removePrefix("./"))).toURL()
        }

        try {
            val dir = File(includeURL.toURI())
            if (dir.exists() && dir.isDirectory) {
                val files = dir.walkTopDown().toSortedSet().toList()
                        .filter { fileInDir -> fileInDir.isDirectory }
                        .sortedBy { fileInDir -> fileInDir.absolutePath }

                return Pair(
                        emptyList(),
                        files.map { fileInDir -> "//INCLUDE_DIR ${fileInDir.absolutePath}" }
                )
            }
            else {
                errorMsg("Failed to resolve $commentPrefix'$include'")
                System.err.println("'$include' does not exist or it is not a directory".prependIndent("[kscript] [ERROR] "))
                quit(1)
            }
        }
        catch (e: FileNotFoundException) {
            errorMsg("Failed to resolve $commentPrefix'$include'")
            System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
            quit(1)
        }
    }
}

/**
 * Basic launcher used for testing
 *
 *
 * Usage Example:
 * ```
 * cd $KSCRIPT_HOME
 * ./gradlew assemble
 * resolve_inc() { kotlin -classpath build/libs/kscript.jar kscript.app.ResolveIncludes "$@";}
 * resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts
 * cat $(resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts 2>&1)
 * ```
 */
object ResolveIncludes {
    @JvmStatic
    fun main(args: Array<String>) {
        System.err.println(resolveIncludes(File(args[0])).scriptFile.readText())
    }
}
