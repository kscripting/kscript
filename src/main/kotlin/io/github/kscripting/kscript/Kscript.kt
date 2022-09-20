package io.github.kscripting.kscript

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.ConfigBuilder
import io.github.kscripting.kscript.shell.OsType
import io.github.kscripting.kscript.shell.ShellUtils.evalBash
import io.github.kscripting.kscript.shell.ShellUtils.quit
import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.kscript.util.Logger.info
import io.github.kscripting.kscript.util.VersionChecker
import org.docopt.DocoptParser

/**
 * A kscript - Scripting enhancements for Kotlin
 *
 * For details and license see https://github.com/holgerbrandl/kscript
 *
 * @author Holger Brandl
 * @author Marcin Kuszczak
 */

fun main(args: Array<String>) {
    try {
        val config = ConfigBuilder(OsType.findOrThrow(args[0]), System.getProperties(), System.getenv()).build()
        val remainingArgs = args.drop(1)

        // skip org.docopt for version and help to allow for lazy version-check
        val usage = Templates.createUsageOptions(config.osConfig.selfName, BuildConfig.APP_VERSION)

        if (remainingArgs.size == 1 && listOf("--help", "-h", "--version", "-v").contains(remainingArgs[0])) {
            info(usage)
            VersionChecker.versionCheck(BuildConfig.APP_VERSION)
            val systemInfo = evalBash(config.osConfig.osType, "kotlin -version").stdout.split('(')
            info("Kotlin    : " + systemInfo[0].removePrefix("Kotlin version").trim())
            info("Java      : " + systemInfo[1].split('-', ')')[0].trim())
            return
        }

        // note: with current implementation we still don't support `kscript -1` where "-1" is a valid kotlin expression
        val userArgs = remainingArgs.dropWhile { it.startsWith("-") && it != "-" }.drop(1)
        val kscriptArgs = remainingArgs.take(remainingArgs.size - userArgs.size)

        KscriptHandler(config, DocoptParser.parse(kscriptArgs, usage))
            .handle(kscriptArgs, userArgs)
    } catch (e: Exception) {
        errorMsg(e)
        quit(1)
    }
}
