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
const val IMPORT_STATMENT_PREFIX = "import " // todo make more solid by using operator including regex

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(template: File, includeContext: URI = template.parentFile.toURI()): File {
    var script = Script(template)

    // just rewrite user scripts if includes a
    if (!script.any { isIncludeDirective(it) }) {
        return template
    }

    // resolve as long as it takes. YAGNI but we do because we can!
    while (script.any { isIncludeDirective(it) }) {
        script = script.flatMap {
            if (isIncludeDirective(it)) {
                val include = extractIncludeTarget(it)

                val includeURL = when {
                    isUrl(include) -> URL(include)
                    include.startsWith("/") -> File(include).toURI().toURL()
                    else -> includeContext.resolve(URI(include.removePrefix("./"))).toURL()
                }

                try {
                    includeURL.readText().lines()
                } catch (e: FileNotFoundException) {
                    errorMsg("Failed to resolve //INCLUDE '${include}'")
                    System.err.println(e.message?.lines()!!.map { it.prependIndent("[kscript] [ERROR] ") })
                    quit(1)
                }
            } else {
                listOf(it)
            }
        }.let { script.copy(it) }
    }

    return script.consolidateStructure().createTmpScript()
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
 * ./gradlew shadowJar
 * resolve_inc() { kotlin -classpath build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.ResolveIncludesKt "$@";}
 * resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts
 * cat $(resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts 2>&1)
 * ```
 */
fun main(args: Array<String>) {
    System.err.println(resolveIncludes(File(args[0])))
}
