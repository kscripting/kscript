package kscript.integration.tool

import kscript.app.model.OsType
import kscript.app.util.OsPath
import kscript.app.util.ProcessResult
import kscript.app.util.ShellUtils
import kscript.app.util.createDirectories
import java.io.File

object TestContext {
    private val osType: OsType = OsType.findOrThrow(System.getProperty("os.type"))
    private val nativeType = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType

    val nl: String = System.getProperty("line.separator")
    val projectDir: String =
        OsPath.createOrThrow(nativeType, System.getProperty("project.path")).convert(osType).stringPath()
    val testDir: String = "$projectDir/build/tmp/test"

    private val systemPath: String
    private val executionDir: String
    private val executionPath: String
    private val classPathSeparator: String

    init {
        println("os.type        : $osType")
        println("native.type    : $nativeType")
        println("project.path   : $projectDir")

        OsPath.createOrThrow(osType, testDir).createDirectories()

        systemPath = System.getenv()["PATH"]!!
        classPathSeparator = if (osType.isWindowsLike() || osType.isPosixHostedOnWindows()) ";" else ":"
        executionDir = "$projectDir/build/libs"
        executionPath = "$executionDir$classPathSeparator$systemPath"
    }

    fun resolvePath(path: String): String = path

    fun runProcess(command: String): ProcessResult {
        //In MSYS all quotes should be single quotes, otherwise content is interpreted e.g. backslashes.
        //Default MSYS bash interpreter is also replacing double quotes into the single quotes.
        //(see: bash -xc 'kscript "println(1+1)"')
        val newCommand = when {
            osType.isPosixHostedOnWindows() -> command.replace('"', '\'')
            else -> command
        }

        val result = ShellUtils.evalBash(osType, newCommand, null, mapOf("PATH" to executionPath))

        println(result)
        return result
    }

    fun copyToExecutionPath(source: String) {
        val sourceFile = File(projectDir + source)
        val targetFile = File(executionDir + "/" + sourceFile.name)

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setExecutable(true)
    }

    fun printKscriptPath() {
        val kscriptPath = ShellUtils.commandPath(osType, "kscript", mapOf("PATH" to executionPath))
        println("kscript path: $kscriptPath")
    }

    fun clearCache() {
        print("Clearing kscript cache... ")
        ShellUtils.evalBash(osType, "kscript --clear-cache", null, mapOf("PATH" to executionPath))
        println("done.")
    }
}
