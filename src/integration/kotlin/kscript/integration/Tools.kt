package kscript.integration

import kscript.app.model.OsType
import kscript.app.util.*

object Tools {
    //./gradlew -Dtest.single=LinuxSuite -Dos.type=msys -Dshell.path=C:\Programy\Programowanie\Git\usr\bin\bash.exe integration

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
    fun resolvePath(path: String): String = path
    fun runProcess(command: String): ProcessResult {
        //TODO: in case of Cygwin/Msys/Windows there might be different interpreters: e.g. bash in Windows directory or
        //in Cygwin or in Msys - shouldn't we provide shell command path here?
        //val shellPath = OsPath.createOrThrow(nativeType, System.getProperty("shell.path")).toNativeOsPath()
        val result = ShellUtils.evalBash(osType, command)
        println(result)
        return result
    }

    fun resolveProjectDir(): String = projectDir
}
