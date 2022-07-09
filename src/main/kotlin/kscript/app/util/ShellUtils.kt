package kscript.app.util

import kscript.app.model.OsType
import kotlin.system.exitProcess

object ShellUtils {

    fun evalBash(osType: OsType, cmd: String, workingDirectory: OsPath? = null): ProcessResult {
        //NOTE: cmd is an argument to shell (bash/cmd), so it should stay not split by whitespace as a single string

        if (osType == OsType.WINDOWS) {
            return ProcessRunner.runProcess("cmd", "/c", cmd, wd = workingDirectory)
        }

        return ProcessRunner.runProcess("bash", "-c", cmd, wd = workingDirectory)
    }

    fun isInPath(osType: OsType, tool: String) =
        evalBash(osType, "${if (osType == OsType.WINDOWS) "where" else "which"} $tool").stdout.trim().isNotBlank()

    // see discussion on https://github.com/holgerbrandl/kscript/issues/15
    fun guessKotlinHome(osType: OsType): String? {
        if (osType.isWindowsLike()) {
            val whereKotlinOutputString = evalBash(osType, "where kotlinc.bat").stdout
            val whereKotlinOutput = whereKotlinOutputString.lines()

            //Default case - Kotlin is installed manually and added to path
            whereKotlinOutput.forEach {
                val path = it.substringBefore("kotlinc.bat").ifBlank { null }

                if (path != null) {
                    return path
                }
            }

            //Scoop installer
            if (whereKotlinOutputString.contains("scoop")) {
                whereKotlinOutput.forEach {
                    val path = it.substringBefore("kotlinc.cmd").ifBlank { null }
                    if (path != null) {
                        val scoopPath = OsPath.createOrThrow(OsType.WINDOWS, path)
                        val scoopScript = scoopPath.readText().lines()

                        scoopScript.forEach {
                            val scoopPath = it.substringBefore("kotlinc.bat").ifBlank { null }

                            if (scoopPath != null) {
                                return scoopPath.substringAfter("@rem ").ifEmpty { null }
                            }
                        }
                    }
                }
            }

            return null
        }

        val kotlinHome = evalBash(osType, "KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
            "kotlin.home=([^\\s]*)".toRegex().find(this)?.groups?.get(1)?.value
        } ?: return null

        if (osType == OsType.MSYS) {
            return OsPath.createOrThrow(OsType.MSYS, kotlinHome).convert(OsType.WINDOWS).stringPath()
        }

        return kotlinHome
    }

    fun quit(status: Int): Nothing {
        exitProcess(status)
    }
}
