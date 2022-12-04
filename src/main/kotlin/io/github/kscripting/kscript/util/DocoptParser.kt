package io.github.kscripting.kscript.util

import org.docopt.Docopt
import org.docopt.DocoptExitException

object DocoptParser {
    fun parse(args: Iterable<String>, usage: String): Map<String, String> {
        // note: DocOpt has very strange behavior for --help: it throws exception with exitCode=0.
        // It is very unfortunate as it should be possible to provide also --development option, even with help.
        // Because of that, the --help is replaced with --version in arguments, which has the same behaviour in kscript,
        // but does not throw exception from DocOpt.

        val help = (args.contains("--help") || args.contains("-h") || (args.find {
            !it.startsWith("--") && it.startsWith("-") && it.substring(1).contains("h")
        } != null))

        val development = (args.contains("--development") || args.contains("-d") || (args.find {
            !it.startsWith("--") && it.startsWith("-") && it.substring(1).contains("d")
        } != null))

        val parsedArgs = try {
            if (help) {
                mapOf("help" to "true", "development" to "$development")
            } else {
                Docopt(usage).withExit(false).parse(args.toList())
            }
        } catch (e: DocoptExitException) {
            val message = "Invalid usage pattern"
            mapOf("help" to "true", "message" to message)
        }

        return parsedArgs.map {
            it.key.removePrefix("--").replace("[<>]".toRegex(), "") to (it.value?.toString() ?: "")
        }.toMap()
    }
}
