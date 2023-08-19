package io.github.kscripting.kscript.util

import io.github.kscripting.kscript.creator.JarArtifact
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.KotlinOpt
import io.github.kscripting.kscript.resolver.CommandResolver
import io.github.kscripting.kscript.util.Logger.devMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.kscript.util.Logger.warnMsg
import io.github.kscripting.kscript.util.ShellUtils.isInPath
import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.model.OsPath

class Executor(private val commandResolver: CommandResolver) {

    fun retrieveLocalKotlinAndJreVersion(): String {
        val command = commandResolver.getKotlinJreVersion()

        return ShellExecutor.evalAndGobble(
            command, commandResolver.osConfig.osType, null, envAdjuster = ShellUtils::environmentAdjuster
        ).stdout
    }

    //NOTE: for direct execution from Kotlin jars:
    //# Kotlin compilation / running dependencies
    //#CLASS_PATH="$KOTLIN_HOME/lib/kotlin-runner.jar:$KOTLIN_HOME/lib/kotlin-preloader.jar:$KOTLIN_HOME/lib/kotlin-compiler.jar:$KOTLIN_HOME/lib/kotlin-script-runtime.jar"

    fun compileKotlin(jar: OsPath, dependencies: Set<OsPath>, filePaths: Set<OsPath>, compilerOpts: Set<CompilerOpt>) {
        val command = commandResolver.compileKotlin(jar, dependencies, filePaths, compilerOpts)
        devMsg("JAR compile command: $command")

        val processResult = ShellExecutor.evalAndGobble(
            command,
            commandResolver.osConfig.osType,
            envAdjuster = ShellUtils::environmentAdjuster,
            waitTimeMinutes = 30
        )

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

        val processResult = ShellExecutor.eval(
            command,
            commandResolver.osConfig.osType,
            envAdjuster = ShellUtils::environmentAdjuster,
            waitTimeMinutes = Int.MAX_VALUE,
            inheritInput = true
        )

        devMsg("Script execution result:\n$processResult")

        if (processResult != 0) {
            throw IllegalStateException("Execution of scriplet failed:\n$processResult")
        }
    }

    fun runInteractiveRepl(
        jarArtifact: JarArtifact, dependencies: Set<OsPath>, compilerOpts: Set<CompilerOpt>, kotlinOpts: Set<KotlinOpt>
    ) {
        infoMsg("Creating REPL")
        val command =
            commandResolver.interactiveKotlinRepl(dependencies + setOf(jarArtifact.path), compilerOpts, kotlinOpts)
        devMsg("REPL Kotlin command: $command")

        val processResult = ShellExecutor.eval(
            command,
            commandResolver.osConfig.osType,
            envAdjuster = ShellUtils::environmentAdjuster,
            waitTimeMinutes = Int.MAX_VALUE,
            inheritInput = true
        )

        devMsg("Repl execution result:\n$processResult")
    }

    fun runGradleInIdeaProject(projectPath: OsPath) {
        if (isInPath(commandResolver.osConfig.osType, commandResolver.osConfig.gradleCommand)) {
            // Create gradle wrapper
            ShellExecutor.evalAndGobble(
                "gradle wrapper", commandResolver.osConfig.osType, workingDirectory = projectPath
            )
        } else {
            warnMsg("Could not find '${commandResolver.osConfig.gradleCommand}' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_COMMAND_GRADLE' env property")
        }

        if (isInPath(commandResolver.osConfig.osType, commandResolver.osConfig.intellijCommand)) {
            val command = commandResolver.executeIdea(projectPath)
            devMsg("Idea execute command: $command")

            val processResult = ShellExecutor.evalAndGobble(command, commandResolver.osConfig.osType)

            devMsg("Script execution result:\n$processResult")

            if (processResult.exitCode != 0) {
                throw IllegalStateException("Execution of idea command failed:\n$processResult")
            }
        } else {
            warnMsg("Could not find '${commandResolver.osConfig.intellijCommand}' in your PATH. You should set the command used to launch your intellij as 'KSCRIPT_COMMAND_IDEA' env property")
        }
    }

    fun createPackage(projectPath: OsPath) {
        if (!isInPath(commandResolver.osConfig.osType, commandResolver.osConfig.gradleCommand)) {
            throw IllegalStateException("Gradle is required to package scripts.")
        }

        val command = commandResolver.createPackage()
        devMsg("Create package command: $command")

        val result =
            ShellExecutor.evalAndGobble(command, commandResolver.osConfig.osType, workingDirectory = projectPath)

        if (result.exitCode != 0) {
            throw IllegalStateException("Packaging for path: '$projectPath' failed:$result")
        }
    }
}
