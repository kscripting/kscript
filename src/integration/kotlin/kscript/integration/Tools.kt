package kscript.integration

import kscript.app.model.OsType
import kscript.app.util.*

object Tools {
    private val osType: OsType = OsType.findOrThrow(System.getProperty("os.type"))
    private val projectDir: String

    init {
        val nativeType = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType
        val projectPath = OsPath.createOrThrow(nativeType, System.getProperty("project.path"))

        println("os.type        : $osType")
        println("native.type    : $nativeType")
        println("project.path   : ${projectPath.stringPath()}")

        projectDir = projectPath.convert(osType).stringPath()
    }

    fun resolveKscript(): String = "$projectDir/build/libs/kscript"

    fun runProcess(command: String): ProcessResult {
        //In MSYS all quotes should be single quotes, otherwise content is interpreted e.g. backslashes.
        //Default MSYS bash interpreter is also replacing double quotes into the single quotes.
        //(see: bash -xc 'kscript "println(1+1)"')
        val newCommand = when {
            osType.isPosixHostedOnWindows() -> command.replace('"', '\'')
            else -> command
        }

        val result = ShellUtils.evalBash(osType, newCommand)

        println(result)
        return result
    }

    fun resolveProjectDir(): String = projectDir
}
