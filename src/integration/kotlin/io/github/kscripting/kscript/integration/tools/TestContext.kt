package io.github.kscripting.kscript.integration.tools

import io.github.kscripting.kscript.util.ShellUtils
import io.github.kscripting.kscript.util.ShellUtils.which
import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.model.*
import java.util.*

object TestContext {
    private val osType: OsType = OsType.findOrThrow(System.getProperty("osType"))
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

        testPath.createDirectories()
    }

    fun resolvePath(path: String): String {
        return OsPath.createOrThrow(osType, path).stringPath()
    }

    private fun processDetails(process: ProcessHandle): String {
        return String.format(
            "%8d %8s %10s %26s %-40s",
            process.pid(),
            text(process.parent().map { obj: ProcessHandle -> obj.pid() }),
            text(process.info().user()),
            text(process.info().startInstant()),
            text(process.info().command()),
            //text(process.info().commandLine())
        )
    }

    private fun text(optional: Optional<*>): String? {
        return optional.map { obj: Any -> obj.toString() }.orElse("-")
    }

    fun runProcess(command: String): GobbledProcessResult {
        //In MSYS all quotes should be single quotes, otherwise content is interpreted e.g. backslashes.
        //(MSYS bash interpreter is also replacing double quotes into the single quotes: see: bash -xc 'kscript "println(1+1)"')

        println("Starting test (command: $command)")

        ProcessHandle.allProcesses().filter {
            val cmd = it.info().command().orElseGet { "<no command>" }
            cmd.contains("bash") || cmd.contains("java")
        }.forEach { process: ProcessHandle ->
            println(processDetails(process))
        }

        val newCommand = when {
            osType.isPosixHostedOnWindows() -> command.replace('"', '\'')
            else -> command
        }

        val result = ShellExecutor.evalAndGobble(osType, newCommand, null, ::adjustEnv)

        println(result)

        println("Finishing test (command: $command)")
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
