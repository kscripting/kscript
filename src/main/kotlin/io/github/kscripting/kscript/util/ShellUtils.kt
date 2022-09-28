package io.github.kscripting.kscript.util

import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.model.OsType
import io.github.kscripting.shell.process.EnvAdjuster

object ShellUtils {
    fun which(osType: OsType, command: String, envAdjuster: EnvAdjuster = {}): List<String> = ShellExecutor.eval(
        osType, "${if (osType == OsType.WINDOWS) "where" else "which"} $command", null, envAdjuster
    ).stdout.trim().lines()

    fun isInPath(osType: OsType, command: String, envAdjuster: EnvAdjuster = {}): Boolean {
        val paths = which(osType, command, envAdjuster)
        return paths.isNotEmpty() && paths[0].isNotBlank()
    }

    fun environmentAdjuster(environment: MutableMap<String, String>) {
        // see https://youtrack.jetbrains.com/issue/KT-20785
        // on Windows also other env variables (like KOTLIN_OPTS) interfere with executed command, so they have to be cleaned

        //NOTE: It would be better to prepare minimal env only with environment variables that are required,
        //but it means that we should track, what are default env variables in different OSes

        //Env variables set by Unix scripts (from kscript and Kotlin)
        environment.remove("KOTLIN_RUNNER")

        //Env variables set by Windows scripts (from kscript and Kotlin)
        environment.remove("_KOTLIN_RUNNER")
        environment.remove("KOTLIN_OPTS")
        environment.remove("JAVA_OPTS")
        environment.remove("_version")
        environment.remove("_KOTLIN_HOME")
        environment.remove("_BIN_DIR")
        environment.remove("_KOTLIN_COMPILER")
        environment.remove("JAR_PATH")
        environment.remove("COMMAND")
        environment.remove("_java_major_version")
        environment.remove("ABS_KSCRIPT_PATH")
    }
}
