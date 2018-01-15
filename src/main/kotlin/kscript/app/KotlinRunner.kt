package kscript.app

import java.io.File
import kotlin.system.exitProcess

class KotlinRunner(val kotlinHome: String) {
    companion object {
        const val PRELOADER_CLASS = "org.jetbrains.kotlin.preloading.Preloader"
        const val RUNNER_MAIN_CLASS = "org.jetbrains.kotlin.runner.Main"
    }
    private val jarFileLoader by lazy { JarFileLoader() }

    private val compilerBaseArgs = listOf("-cp", joinToPathString(kotlinHome, "lib", "kotlin-compiler.jar"),
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

    private val preloaderJar = File(joinToPathString(kotlinHome, "lib", "kotlin-preloader.jar"))

    private val preloaderMainMethod by lazy {
        jarFileLoader.addFile(preloaderJar)
        jarFileLoader.loadClass(PRELOADER_CLASS).getDeclaredMethod("main", Array<String>::class.java)
    }

    fun compile(jarFile: File, scriptFile: File, wrapperFile: File?, classpath: String?, compilerOpts: String) {
        val compilerArgs = if (compilerOpts.isNotEmpty()) compilerOpts.split("\\s+".toRegex()) else listOf()
        val baseArgs = listOf<String>("-Xskip-runtime-version-check") +
                compilerArgs +
                listOf("-d", jarFile.absolutePath, scriptFile.absolutePath)
        val wrapperArgs =  listOfNotNull(wrapperFile?.absolutePath)
        val cpArgs = classPathArgs(classpath)
        runKotlinc(baseArgs + wrapperArgs + cpArgs)
    }

    fun runScriptAndExit(scriptClassPath: String, execClassName: String, userArgs: List<String>, kotlinOpts: String) {
        val runnerArgs = listOf("-cp", scriptClassPath, execClassName) + userArgs
        if (kotlinOpts.isNotEmpty()) {
            exitProcess(execKotlin(kotlinOpts, runnerArgs))
        } else {
            runKotlin(runnerArgs)
            exitProcess(0)
        }
    }

    fun interactiveShell(jarFile: File, classpath: String?, compilerOpts: String, kotlinOpts: String) {
        val compilerArgs = if (compilerOpts.isNotEmpty()) compilerOpts.split("\\s+".toRegex()) else listOf()
        val jarPath = if (jarFile.isFile) jarFile.absolutePath else null
        val args = compilerArgs + classPathArgs(classpath, jarPath)
        if (kotlinOpts.isNotEmpty()) {
            execKotlinc(kotlinOpts, args)
        } else {
            runKotlinc(args)
        }
    }

    private fun classPathArgs(vararg paths: String?): List<String> {
        val combinedClasspath = listOfNotNull(*paths).filter { it.isNotEmpty() }.joinToString(CP_SEPARATOR_CHAR)
        return if (combinedClasspath.isNotEmpty()) {
            listOf("-cp", combinedClasspath)
        } else listOf()
    }

    private fun runKotlinc(args: Collection<String>) {
        preloaderMainMethod.invoke(jarFileLoader, (compilerBaseArgs + args).toTypedArray())
    }

    private fun execKotlinc(kotlinOpts: String, args: List<String>): Int {
        val kotlinOptsList = kotlinOpts.split("\\s+".toRegex())
        return if (IS_WINDOWS) {
            val (javaOpts, pureKotlinOpts) = kotlinOptsList.partition { it.startsWith("-J") || it.startsWith("-D") }
            val pureJavaOpts = javaOpts.map { if (it.startsWith("-J")) it.substring(2) else it }
            val javaCmd = System.getenv("JAVACMD") ?: "java"
            exec(listOf(javaCmd) + pureJavaOpts + compilerBaseArgs + pureKotlinOpts + args)
        } else {
            exec(listOf("kotlinc") + kotlinOptsList + args)
        }
    }


    private fun runKotlin(args: Collection<String>) {
        val runnerMain = jarFileLoader.loadClass(RUNNER_MAIN_CLASS).getDeclaredMethod("main", Array<String>::class.java)
        runnerMain.invoke(jarFileLoader, args.toTypedArray())
    }

    private fun execKotlin(kotlinOpts: String, args: List<String>): Int {
        val kotlinOptsList = kotlinOpts.split("\\s+".toRegex())
        return if (IS_WINDOWS) {
            val runnerJar = joinToPathString(kotlinHome, "lib", "kotlin-runner.jar")
            val (javaOpts, pureKotlinOpts) = kotlinOptsList.partition { it.startsWith("-J") || it.startsWith("-D") }
            val pureJavaOpts = javaOpts.map { if (it.startsWith("-J")) it.substring(2) else it }
            val javaCmd = System.getenv("JAVACMD") ?: "java"
            exec(listOf(javaCmd) + pureJavaOpts + listOf("-Dkotlin.home=${kotlinHome}", "-cp", runnerJar, RUNNER_MAIN_CLASS)
                    + pureKotlinOpts + args)
        } else {
            exec(listOf("kotlin") + kotlinOptsList + args)
        }
    }

    private fun exec(commandList: List<String>) =
            ProcessBuilder(commandList)
                    .inheritIO()
                    .apply { environment()["KOTLIN_RUNNER"] = "" }
                    .start()
                    .waitFor()
}
