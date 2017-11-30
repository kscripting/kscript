package kscript.app

import java.io.File
import java.io.FileNotFoundException
import java.net.URL

/**
 * @author Holger Brandl
 */

/** Resolve include declarations in a script file. Resolved script will be put into another temporary script */
fun resolveIncludes(template: File): File = resolveIncludesInternal(template)

private fun resolveIncludesInternal(template: File): File {
    val scriptLines = template.readText().lines()

    if (scriptLines.find { it.startsWith("//INCLUDE ") } == null) {
        return template
    }

    val sb = StringBuilder()

    // collect up the set of imports in this
    val imports : MutableSet<String> = emptySet<String>().toMutableSet()

    scriptLines.map {
        if (it.startsWith("//INCLUDE")) {
            val include = it.split("[ ]+".toRegex()).last()

            val includeURL = when {
                include.startsWith("http://") -> URL(include)
                include.startsWith("https://") -> URL(include)
                include.startsWith("./") || include.startsWith("../") -> File(template.parentFile, include).toURI().toURL()
                include.startsWith("/") -> File(include).toURI().toURL()
                else -> File(template.parentFile, include).toURI().toURL()
            }

            try {
                // collect the import or emit
                includeURL.readText().lines().forEach {
                    if (it.startsWith("import")) {
                        imports.add(it)
                    } else {
                        sb.appendln(it)
                    }
                }
            } catch (e: FileNotFoundException) {
                errorMsg("Failed to resolve //INCLUDE '${include}'")
                System.err.println(e.message?.lines()!!.map { it.prependIndent("[ERROR] ") })
                quit(1)
            }
        } else if (it.startsWith("import")) {
            imports.add(it)
        } else {
            // if its not an include directive or an import or a bang line, emit as is
            if (it.startsWith("#")) {  } else { sb.appendln(it) }
        }
    }

    val impsb = StringBuilder()
    imports.map { impsb.appendln(it) }

    val final = impsb.appendln(sb.toString())
    return createTmpScript(final.toString())
}


// basic launcher for testing
fun main(args: Array<String>) {
    System.err.println(resolveIncludes(File(args[0])))
}

/**
# Usage Example
cd $KSCRIPT_HOME
gradle shadowJar
resolve_inc() { kotlin -classpath build/libs/kscript-0.1-SNAPSHOT-all.jar kscript.app.ResolveIncludesKt "$@";}
resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts
cat $(resolve_inc /Users/brandl/projects/kotlin/kscript/test/resources/includes/include_variations.kts 2>&1)
 */