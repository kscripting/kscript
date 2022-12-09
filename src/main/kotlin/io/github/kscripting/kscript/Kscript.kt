package io.github.kscripting.kscript

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.creator.DebugInfoCreator
import io.github.kscripting.kscript.model.ConfigBuilder
import io.github.kscripting.kscript.resolver.CommandResolver
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.Logger
import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.kscript.util.Logger.info
import io.github.kscripting.kscript.util.OptionsUtils
import io.github.kscripting.kscript.util.VersionChecker
import io.github.kscripting.shell.model.OsType
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.ParseException
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

        val parser: CommandLineParser = DefaultParser()
        val options = OptionsUtils.createOptions()

        val parsedLine = try {
            parser.parse(options, kscriptArgs.toTypedArray())
        } catch (e: ParseException) {
            info(OptionsUtils.createHelpText(config.osConfig.selfName, options))
            throw IllegalArgumentException(e.message)
        }

        val parsedOptions = parsedLine.options.associate { it.longOpt to it.value }.toMutableMap()

        if (parsedLine.argList.isNotEmpty()) {
            parsedOptions["script"] = parsedLine.argList[0]
        }

        // Constraints
        if (parsedOptions.isEmpty() || (parsedOptions.containsKey("interactive") && !parsedOptions.containsKey("script"))) {
            info(OptionsUtils.createHelpText(config.osConfig.selfName, options))
            throw IllegalArgumentException("KScript requires 'script' as an argument")
        }

        Logger.silentMode = parsedOptions.containsKey("silent")
        Logger.devMode = parsedOptions.containsKey("development")

        if (Logger.devMode) {
            info(DebugInfoCreator().create(config, kscriptArgs, userArgs))
        }

        val executor = Executor(CommandResolver(config.osConfig))

        if (parsedOptions.containsKey("help") || parsedOptions.containsKey("version")) {
            val versionChecker = VersionChecker(executor)

            val newVersion = if (versionChecker.isThereANewKscriptVersion()) versionChecker.remoteKscriptVersion else ""

            if (parsedOptions.containsKey("help")) {
                info(OptionsUtils.createHelpText(config.osConfig.selfName, options).trimEnd())
            }

            info(
                Templates.createVersionInfo(
                    BuildConfig.APP_BUILD_TIME,
                    BuildConfig.APP_VERSION,
                    newVersion,
                    versionChecker.localKotlinVersion,
                    versionChecker.localJreVersion
                )
            )
            info()

            return
        }

        KscriptHandler(executor, config, parsedOptions).handle(userArgs)
    } catch (e: Exception) {
        errorMsg(e)
        info()
        exitProcess(1)
    }
}
