package kscript.app.shell

import kscript.app.model.CompilerOpt
import kscript.app.model.KotlinOpt
import kscript.app.model.OsConfig
import kscript.app.resolver.CommandResolver
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.util.Logger.warnMsg

class Executor(private val commandResolver: CommandResolver, private val osConfig: OsConfig) {
    fun runInteractiveRepl(dependencies: Set<OsPath>, compilerOpts: Set<CompilerOpt>, kotlinOpts: Set<KotlinOpt>) {
        infoMsg("Creating REPL")
        val command = commandResolver.interactiveKotlinRepl(dependencies, compilerOpts, kotlinOpts)
        devMsg("REPL Kotlin command: $command")

        println(command)
    }

    fun runIdea(projectPath: OsPath) {
        if (ShellUtils.isCommandInPath(osConfig.osType, osConfig.gradleCommand)) {
            // Create gradle wrapper
            ShellUtils.evalBash(osConfig.osType, "gradle wrapper", workingDirectory = projectPath)
        } else {
            warnMsg("Could not find '${osConfig.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_COMMAND_GRADLE' env property")
        }

        if (ShellUtils.isCommandInPath(osConfig.osType, osConfig.intellijCommand)) {
            val command = commandResolver.executeIdea(projectPath)
            devMsg("Idea execute command: $command")
            println(command)
        } else {
            warnMsg("Could not find '${osConfig.intellijCommand}' in your PATH. You should set the command used to launch your intellij as 'KSCRIPT_COMMAND_IDEA' env property")
        }
    }

    fun createPackage(projectPath: OsPath) {
        if (!ShellUtils.isCommandInPath(osConfig.osType, osConfig.gradleCommand)) {
            throw IllegalStateException("Gradle is required to package scripts.")
        }

        val command = commandResolver.createPackage()
        devMsg("Create package command: $command")

        val result = ShellUtils.evalBash(osConfig.osType, command, workingDirectory = projectPath)

        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
