package io.github.kscripting.kscript.integration.tools

import io.github.kscripting.kscript.util.ShellUtils
import io.github.kscripting.kscript.util.ShellUtils.which
import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.model.*

object TestContext {
    val osType: OsType = OsType.findOrThrow(System.getProperty("osType"))
    private val nativeType = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType

    private val projectPath: OsPath = OsPath.createOrThrow(nativeType, System.getProperty("projectPath"))
    private val execPath: OsPath = projectPath.resolve("build/kscript/bin")
    private val testPath: OsPath = projectPath.resolve("build/tmp/test")
    private val pathEnvName = if (osType.isWindowsLike()) "Path" else "PATH"
    private val systemPath: String = System.getenv()[pathEnvName]!!

    private val pathSeparator: String = if (osType.isWindowsLike() || osType.isPosixHostedOnWindows()) ";" else ":"
    private val envPath: String = "${execPath.convert(osType)}$pathSeparator$systemPath"

    val nl: String = System.getProperty("line.separator")
    val projectDir: String = projectPath.convert(osType).stringPath()
    val testDir: String = testPath.convert(osType).stringPath()

    init {
        println("osType         : $osType")
        println("nativeType     : $nativeType")
        println("projectDir     : $projectDir")
        println("testDir        : $testDir")
        println("execDir        : ${execPath.convert(osType)}")
        println("Kotlin version : ${ShellExecutor.evalAndGobble(osType, "kotlin -version", null, ::adjustEnv)}")

        testPath.createDirectories()
    }

    fun resolvePath(path: String): OsPath {
        return OsPath.createOrThrow(osType, path)
    }

    fun runProcess(command: String): GobbledProcessResult {
        //In MSYS all quotes should be single quotes, otherwise content is interpreted e.g. backslashes.
        //(MSYS bash interpreter is also replacing double quotes into the single quotes: see: bash -xc 'kscript "println(1+1)"')
        val newCommand = when {
            osType.isPosixHostedOnWindows() -> command.replace('"', '\'')
            else -> command
        }

        val result = ShellExecutor.evalAndGobble(osType, newCommand, null, ::adjustEnv)
        println(result)

        return result
    }

    fun copyToExecutablePath(source: String) {
        val sourceFile = projectPath.resolve(source).toNativeFile()
        val targetFile = execPath.resolve(sourceFile.name).toNativeFile()

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setExecutable(true)
    }

    fun copyToTestPath(source: String) {
        val sourceFile = projectPath.resolve(source).toNativeFile()
        val targetFile = testPath.resolve(sourceFile.name).toNativeFile()

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setExecutable(true) //Needed if the file is kotlin script
    }

    fun printPaths() {
        val kscriptPath = which(osType, "kscript", ::adjustEnv)
        println("kscript path: $kscriptPath")
        val kotlincPath = which(osType, "kotlinc", ::adjustEnv)
        println("kotlinc path: $kotlincPath")
    }

    fun clearCache() {
        print("Clearing kscript cache... ")
        ShellExecutor.eval(osType, "kscript --clear-cache", null, ::adjustEnv)
        println("done.")
    }

    private fun adjustEnv(map: MutableMap<String, String>) {
        map[pathEnvName] = envPath
        ShellUtils.environmentAdjuster(map)
    }
}
