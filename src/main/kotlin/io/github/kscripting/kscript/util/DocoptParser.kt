//NOTE: org.docopt package is needed because printUsage property is internal,
//and not accessible from kscript package
@file:Suppress("PackageDirectoryMismatch")

package org.docopt

import io.github.kscripting.kscript.shell.ShellUtils.quit

object DocoptParser {
    fun parse(args: Iterable<String>, usage: String): Map<String, String> {
        val parsedArgs = try {
            Docopt(usage).withExit(false).parse(args.toList())
        } catch (e: DocoptExitException) {
            // mimic docopts exit behavior but using process-substituion handler at the end
            if (e.message != null) {
                System.err.println(e.message)
            }

            if (e.printUsage) {
                System.err.println(usage)
            }

            quit(e.exitCode)
        }

        return parsedArgs.map {
            it.key.removePrefix("--").replace("[<>]".toRegex(), "") to it.value.toString()
        }.toMap()
    }
}
