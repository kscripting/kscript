package io.github.kscripting.kscript.shell

import io.github.kscripting.kscript.creator.JarArtifact
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.KotlinOpt
import io.github.kscripting.kscript.model.OsConfig
import io.github.kscripting.kscript.resolver.CommandResolver
import io.github.kscripting.kscript.shell.ShellUtils.isInPath
import io.github.kscripting.kscript.util.Logger.devMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.kscript.util.Logger.warnMsg
import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.model.OsPath

class Executor(private val commandResolver: CommandResolver, private val osConfig: OsConfig) {
    fun compileKotlin(jar: OsPath, dependencies: Set<OsPath>, filePaths: Set<OsPath>, compilerOpts: Set<CompilerOpt>) {
        val command = commandResolver.compileKotlin(jar, dependencies, filePaths, compilerOpts)
        devMsg("JAR compile command: $command")

        val processResult = ShellExecutor.eval(osConfig.osType, command, envAdjuster = ShellUtils::environmentAdjuster)

        devMsg("Script compilation result:\n$processResult")

        if (processResult.exitCode != 0) {
            throw IllegalStateException("Compilation of scriplet failed:\n$processResult")
        }
    }

    fun executeKotlin(
        jarArtifact: JarArtifact, dependencies: Set<OsPath>, userArgs: List<String>, kotlinOpts: Set<KotlinOpt>
    ) {
        val command = commandResolver.executeKotlin(jarArtifact, dependencies, userArgs, kotlinOpts)
        devMsg("Kotlin execute command: $command")

        println(command)
    }

    fun runInteractiveRepl(dependencies: Set<OsPath>, compilerOpts: Set<CompilerOpt>, kotlinOpts: Set<KotlinOpt>) {
        infoMsg("Creating REPL")
        val command = commandResolver.interactiveKotlinRepl(dependencies, compilerOpts, kotlinOpts)
        devMsg("REPL Kotlin command: $command")

        println(command)
    }

    fun runIdea(projectPath: OsPath) {
        if (isInPath(osConfig.osType, osConfig.gradleCommand)) {
            // Create gradle wrapper
            ShellExecutor.eval(osConfig.osType, "gradle wrapper", workingDirectory = projectPath)
        } else {
            warnMsg("Could not find '${osConfig.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_COMMAND_GRADLE' env property")
        }

        if (isInPath(osConfig.osType, osConfig.intellijCommand)) {
            val command = commandResolver.executeIdea(projectPath)
            devMsg("Idea execute command: $command")
            println(command)
        } else {
            warnMsg("Could not find '${osConfig.intellijCommand}' in your PATH. You should set the command used to launch your intellij as 'KSCRIPT_COMMAND_IDEA' env property")
        }
    }

    fun createPackage(projectPath: OsPath) {
        if (!isInPath(osConfig.osType, osConfig.gradleCommand)) {
            throw IllegalStateException("Gradle is required to package scripts.")
        }

        val command = commandResolver.createPackage()
        devMsg("Create package command: $command")

        val result = ShellExecutor.eval(osConfig.osType, command, workingDirectory = projectPath)

        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
