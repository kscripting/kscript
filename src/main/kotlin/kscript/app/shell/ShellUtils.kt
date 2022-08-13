package kscript.app.shell

import kscript.app.model.OsType
import kotlin.system.exitProcess

object ShellUtils {

    fun evalBash(
        osType: OsType, cmd: String, workingDirectory: OsPath? = null, environment: Map<String, String> = emptyMap()
    ): ProcessResult {
        //NOTE: cmd is an argument to shell (bash/cmd), so it should stay not split by whitespace as a single string

        if (osType == OsType.WINDOWS) {
            return ProcessRunner.runProcess("cmd", "/c", cmd, wd = workingDirectory, env = environment)
        }

        return ProcessRunner.runProcess("bash", "-c", cmd, wd = workingDirectory, env = environment)
    }

    fun commandPaths(osType: OsType, cmd: String, environment: Map<String, String> = emptyMap()): List<String> =
        evalBash(osType, "${if (osType == OsType.WINDOWS) "where" else "which"} $cmd", null, environment).stdout.trim()
            .lines()

    fun isCommandInPath(osType: OsType, cmd: String, environment: Map<String, String> = emptyMap()): Boolean {
        val paths = commandPaths(osType, cmd, environment)
        return paths.isNotEmpty() && paths[0].isNotBlank()
    }

    // see discussion on https://github.com/holgerbrandl/kscript/issues/15
    fun guessKotlinHome(osType: OsType): String? {
        if (osType.isWindowsLike()) {
            return guessWindowsKotlinHome(osType)
        }
        return guessPosixKotlinHome(osType)
    }

    fun guessPosixKotlinHome(osType: OsType): String? {
        val kotlinHome = evalBash(osType, "KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
            "kotlin.home=([^\\s]*)".toRegex().find(this)?.groups?.get(1)?.value
        } ?: return null

        if (osType == OsType.MSYS) {
            return OsPath.createOrThrow(OsType.MSYS, kotlinHome).convert(OsType.WINDOWS).stringPath()
        }

        return kotlinHome
    }

    fun guessWindowsKotlinHome(osType: OsType): String? {
        val whereKotlinOutput = commandPaths(osType, "kotlinc.bat")

        //Default case - Kotlin is installed manually and added to path
        whereKotlinOutput.forEach {
            val path = it.substringBefore("\\bin\\kotlinc.bat", "").ifBlank { null }

            if (path != null) {
                return path
            }
        }

        //Scoop installer
        whereKotlinOutput.forEach {
            val path = it.substringBefore("kotlinc.cmd", "").ifBlank { null }

            if (path != null) {
                val outerScoopPath = OsPath.createOrThrow(osType, "$path\\kotlin.cmd")
                val scoopScript = outerScoopPath.readText().lines()

                scoopScript.forEach {
                    val scoopPath = it.substringBefore("\\bin\\kotlinc.bat", "").ifBlank { null }

                    if (scoopPath != null) {
                        return scoopPath.substringAfter("@rem ").ifEmpty { null }
                    }
                }
            }
        }

        return null
    }

    fun quit(status: Int): Nothing {
        exitProcess(status)
    }

    fun whitespaceCharsToSymbols(string: String): String = string.replace("\\", "[bs]").lines().joinToString("[nl]")
}
