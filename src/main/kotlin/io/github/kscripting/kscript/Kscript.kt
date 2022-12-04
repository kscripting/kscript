package io.github.kscripting.kscript

import io.github.kscripting.kscript.code.Templates.createUsageOptions
import io.github.kscripting.kscript.model.ConfigBuilder
import io.github.kscripting.kscript.util.DocoptParser
import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.shell.model.OsType
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

        // note: first argument has to be OSTYPE
        val remainingArgs = args.drop(1)

        // note: with current implementation we still don't support `kscript -1` where "-1" is a valid kotlin expression
        val userArgs = remainingArgs.dropWhile { it.startsWith("-") && it != "-" }.drop(1)
        val kscriptArgs = remainingArgs.take(remainingArgs.size - userArgs.size)

        val options = DocoptParser.parse(
            kscriptArgs,
            createUsageOptions(config.osConfig.selfName, BuildConfig.APP_BUILD_TIME, BuildConfig.APP_VERSION)
        )

        KscriptHandler(config, options).handle(kscriptArgs, userArgs)
    } catch (e: Exception) {
        errorMsg(e)
        exitProcess(1)
    }
}
