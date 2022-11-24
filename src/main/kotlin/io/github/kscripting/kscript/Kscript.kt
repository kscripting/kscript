package io.github.kscripting.kscript

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.ConfigBuilder
import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.kscript.util.Logger.info
import io.github.kscripting.kscript.util.VersionChecker
import io.github.kscripting.shell.model.OsType
import org.docopt.DocoptParser
import kotlin.system.exitProcess

/**
 * A kscript - Scripting enhancements for Kotlin
 *
 * For details and license see https://github.com/kscripting/kscript
 *
 * @author Holger Brandl
 * @author Marcin Kuszczak
 */

fun main(args: Array<String>) {
    try {
        val config = ConfigBuilder(
            OsType.findOrThrow(args[0]), System.getProperties(), System.getenv()
        ).build()

        val remainingArgs = args.drop(1)

        // skip DocOpt for version and help to allow lazy version-check
        if (remainingArgs.size == 1 && listOf("--help", "-h", "--version", "-v").contains(remainingArgs[0])) {
            val versionChecker = VersionChecker(config.osConfig)

            val newVersion =
                if (versionChecker.isThereANewKscriptVersion()) versionChecker.remoteKscriptVersion else ""

            info(Templates.createUsageOptions(config.osConfig.selfName, BuildConfig.APP_VERSION, newVersion))

            info("Kotlin    : ${versionChecker.localKotlinVersion}")
            info("Java      : ${versionChecker.localJreVersion}")
            return
        }

        // note: with current implementation we still don't support `kscript -1` where "-1" is a valid kotlin expression
        val userArgs = remainingArgs.dropWhile { it.startsWith("-") && it != "-" }.drop(1)
        val kscriptArgs = remainingArgs.take(remainingArgs.size - userArgs.size)

        val usage = Templates.createUsageOptions(config.osConfig.selfName, BuildConfig.APP_VERSION)
        KscriptHandler(config, DocoptParser.parse(kscriptArgs, usage)).handle(kscriptArgs, userArgs)
    } catch (e: Exception) {
        errorMsg(e)
        exitProcess(1)
    }
}
