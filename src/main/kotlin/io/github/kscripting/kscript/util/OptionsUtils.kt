package io.github.kscripting.kscript.util

import io.github.kscripting.kscript.code.Templates.createFooterInfo
import io.github.kscripting.kscript.code.Templates.createHeaderInfo
import io.github.kscripting.kscript.code.Templates.createTitleInfo
import io.github.kscripting.kscript.code.Templates.createUsageInfo
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.PrintWriter
import java.io.StringWriter

object OptionsUtils {
    fun createOptions(): Options {
        return Options()
            .addOption("i", "interactive", false, "Create interactive shell with dependencies as declared in script")
            .addOption("t", "text", false, "Enable stdin support API for more streamlined text processing")
            .addOption("e", "idea", false, "Open script in temporary Intellij session")
            .addOption("s", "silent", false, "Suppress status logging")
            .addOption("d", "development", false, "Logging of exception stack traces and additional log messages")
            .addOption("r", "report", false, "Prints script's deprecated features report")
            .addOption("p", "package", false, "Package script and dependencies into self-dependent binary")
            .addOption("a", "add-bootstrap-header", false, "Prepend bash header that installs kscript if necessary")
            .addOption("h", "help", false, "Prints help information")
            .addOption("v", "version", false, "Prints version information")
            .addOption("c", "clear-cache", false, "Wipes out cached script jars and urls")
            .addOption("n", "dependencies", false, "Prints list of dependencies")
    }

    fun createHelpText(selfName: String, options: Options): String {
        val out = StringWriter()
        HelpFormatter().printHelp(
            PrintWriter(out), 120, createUsageInfo(selfName), createHeaderInfo(), options, 2, 2, createFooterInfo()
        )
        return createTitleInfo(selfName) + out.toString().drop(7)
    }
}
