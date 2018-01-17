package kscript.app

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.net.URL

/**
 * @author Holger Brandl
 * @author Ilan Pillemer
 */

const val PACKAGE_STATEMENT_PREFIX = "package "
const val IMPORT_STATEMENT_PREFIX = "import " // todo make more solid by using operator including regex

data class IncludeResult(val scriptFile: File, val includes: List<URL> = emptyList())

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(template: File, includeContext: URI = template.parentFile.toURI()): IncludeResult {
    var script = Script(template)

    // just rewrite user scripts if includes a
    if (!script.any { isIncludeDirective(it) }) {
        return IncludeResult(template)
    }

    val includes = emptyList<URL>().toMutableList()

    // resolve as long as it takes. YAGNI but we do because we can!
    while (script.any { isIncludeDirective(it) }) {
        script = script.flatMap { line ->
            if (isIncludeDirective(line)) {
                val include = extractIncludeTarget(line)

                val includeURL = when {
                    isUrl(include) -> URL(include)
                    include.startsWith("/") -> File(include).toURI().toURL()
                    include.startsWith("~/") -> File(System.getenv("HOME")!! + include.substring(1)).toURI().toURL()
                    else -> includeContext.resolve(URI(include.removePrefix("./"))).toURL()
                }

                // test if include was processed already (aka include duplication, see #151)
                if (includes.map { it.path }.contains(includeURL.path)) {
                    // include was already resolved, so we return an emtpy result here to avoid duplication errors
                    emptyList()
                } else {
                    includes.add(includeURL)

                    try {
                        includeURL.readText().lines()
                    } catch (e: FileNotFoundException) {
                        errorMsg("Failed to resolve //INCLUDE '${include}'")
                        System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
                        quit(1)
                    }
                }
            } else {
                listOf(line)
            }
        }.let { script.copy(it) }
    }

    return IncludeResult(script.consolidateStructure().createTmpScript(), includes)
}

internal fun isUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")

private const val INCLUDE_ANNOT_PREFIX = "@file:Include("

internal fun isIncludeDirective(line: String) = line.startsWith("//INCLUDE") || line.startsWith(INCLUDE_ANNOT_PREFIX)


internal fun extractIncludeTarget(incDirective: String) = when {
    incDirective.startsWith(INCLUDE_ANNOT_PREFIX) -> incDirective
        .replaceFirst(INCLUDE_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"')
    else -> incDirective.split("[ ]+".toRegex()).last()
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
