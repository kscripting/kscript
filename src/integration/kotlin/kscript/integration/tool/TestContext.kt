package kscript.integration.tool

import kscript.app.model.OsType
import kscript.app.util.OsPath
import kscript.app.util.ProcessResult
import kscript.app.util.ShellUtils
import java.io.File

object TestContext {
    val nl: String = System.getProperty("line.separator")

    private val osType: OsType = OsType.findOrThrow(System.getProperty("os.type"))
    private val projectDir: String
    private val systemPath: String
    private val executionDir: String
    private val executionPath: String
    private val classPathSeparator: String

    init {
        val nativeType = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType
        val projectPath = OsPath.createOrThrow(nativeType, System.getProperty("project.path"))

        println("os.type        : $osType")
        println("native.type    : $nativeType")
        println("project.path   : ${projectPath.stringPath()}")

        projectDir = projectPath.convert(osType).stringPath()
        systemPath = System.getenv()["PATH"]!!
        classPathSeparator = if (osType.isWindowsLike() || osType.isPosixHostedOnWindows()) ";" else ":"
        executionDir = "$projectDir/build/libs"
        executionPath = "$executionDir$classPathSeparator$systemPath"
    }

    fun resolvePath(path: String): String = path

    fun resolveKscript(): String = "$projectDir/build/libs/kscript"

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

    fun resolveProjectDir(): String = projectDir

    fun copyToExecutionPath(source: String) {
        val sourceFile = File(projectDir + source)
        val targetFile = File(executionDir + "/" + sourceFile.name)

        sourceFile.copyTo(targetFile, overwrite = true)
    }

    fun printKscriptPath() {
        val kscriptPath = ShellUtils.commandPath(osType, "kscript", mapOf("PATH" to executionPath))
        println("kscript path: $kscriptPath")
    }

    fun clearCache() {
        print("Clearing kscript cache... ")
        ShellUtils.commandPath(osType, "kscript --clear-cache", mapOf("PATH" to executionPath))
        println("done.")
    }
}
