package kscript.integration.tools

import kscript.app.model.OsType
import kscript.app.util.*

object TestContext {
    private val osType: OsType = OsType.findOrThrow(System.getProperty("os.type"))
    private val nativeType = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType

    private val projectPath: OsPath = OsPath.createOrThrow(nativeType, System.getProperty("project.path"))
    private val execPath: OsPath = projectPath.resolve("build/libs")
    private val testPath: OsPath = projectPath.resolve("build/tmp/test")

    private val systemPath: String = System.getenv()["PATH"]!!
    private val classPathSeparator: String = if (osType.isWindowsLike() || osType.isPosixHostedOnWindows()) ";" else ":"
    private val envPath: String = "${execPath.convert(osType)}$classPathSeparator$systemPath"
    private val envMap = mapOf("PATH" to envPath)

    val nl: String = System.getProperty("line.separator")
    val projectDir: String = projectPath.convert(osType).stringPath()
    val testDir: String = testPath.convert(osType).stringPath()

    init {
        println("os.type        : $osType")
        println("native.type    : $nativeType")
        println("projectDir     : $projectDir")
        println("testDir        : $testDir")
        println("execDir        : ${execPath.convert(osType)}")

        testPath.createDirectories()
    }

    fun resolvePath(path: String): String = path

    fun runProcess(command: String): ProcessResult {
        //In MSYS all quotes should be single quotes, otherwise content is interpreted e.g. backslashes.
        //(MSYS bash interpreter is also replacing double quotes into the single quotes: see: bash -xc 'kscript "println(1+1)"')
        val newCommand = when {
            osType.isPosixHostedOnWindows() -> command.replace('"', '\'')
            else -> command
        }

        val result = ShellUtils.evalBash(osType, newCommand, null, envMap)

        println(result)
        return result
    }

    fun copyToExecutablePath(source: String) {
        val sourceFile = projectPath.resolve(source).toNativeFile()
        val targetFile = execPath.resolve(sourceFile.name).toNativeFile()

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setExecutable(true)
    }

    fun printKscriptPath() {
        val kscriptPath = ShellUtils.commandPath(osType, "kscript", envMap)
        println("kscript path: $kscriptPath")
    }

    fun clearCache() {
        print("Clearing kscript cache... ")
        ShellUtils.evalBash(osType, "kscript --clear-cache", null, envMap)
        println("done.")
    }
}
