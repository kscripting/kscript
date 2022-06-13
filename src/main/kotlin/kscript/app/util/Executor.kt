package kscript.app.util

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.KotlinOpt
import kscript.app.model.OsConfig
import kscript.app.resolver.CommandResolver
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.util.Logger.warnMsg
import java.nio.file.Path

class Executor(private val commandResolver: CommandResolver, private val osConfig: OsConfig) {
    fun compileKotlin(jar: OsPath, dependencies: Set<OsPath>, filePaths: Set<OsPath>, compilerOpts: Set<CompilerOpt>) {
        val command = commandResolver.compileKotlin(jar, dependencies, filePaths, compilerOpts)
        devMsg("JAR compile command: $command")

        val scriptCompileResult = ShellUtils.evalBash(osConfig.osType, command)

        if (scriptCompileResult.exitCode != 0) {
            throw IllegalStateException("Compilation of scriplet failed:\n$scriptCompileResult")
        }
    }

    fun executeKotlin(
        jarArtifact: JarArtifact,
        dependencies: Set<OsPath>,
        userArgs: List<String>,
        kotlinOpts: Set<KotlinOpt>
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
        if (ShellUtils.isInPath(osConfig.osType, osConfig.gradleCommand)) {
            // Create gradle wrapper
            ProcessRunner.runProcess("${osConfig.gradleCommand} wrapper", wd = projectPath.toNativeFile())
        } else {
            warnMsg("Could not find '${osConfig.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_COMMAND_GRADLE' env property")
        }

        if (ShellUtils.isInPath(osConfig.osType, osConfig.intellijCommand)) {
            val command = commandResolver.executeIdea(projectPath)
            devMsg("Idea execute command: $command")
            println(command)
        } else {
            warnMsg("Could not find '${osConfig.intellijCommand}' in your PATH. You should set the command used to launch your intellij as 'KSCRIPT_COMMAND_IDEA' env property")
        }
    }

    fun createPackage(projectPath: OsPath) {
        if (!ShellUtils.isInPath(osConfig.osType, osConfig.gradleCommand)) {
            throw IllegalStateException("gradle is required to package scripts")
        }

        val command = commandResolver.createPackage(projectPath)
        devMsg("Create package command: $command")

        val result = ShellUtils.evalBash(osConfig.osType, command)
        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
