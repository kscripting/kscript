package kscript.app

import java.io.File

class KotlinRunner(private val kotlinHome: String) {
    companion object {
        private const val PRELOADER_CLASS = "org.jetbrains.kotlin.preloading.Preloader"
        private const val RUNNER_MAIN_CLASS = "org.jetbrains.kotlin.runner.Main"
    }

    private val jarFileLoader by lazy { JarFileLoader() }

    private val compilerBaseArgs = listOf("-cp", joinToPathString(kotlinHome, "lib", "kotlin-compiler.jar"),
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")


    private val preloaderMainMethod by lazy {
        val preloaderJar = File(joinToPathString(kotlinHome, "lib", "kotlin-preloader.jar"))
        jarFileLoader.addFile(preloaderJar)
        jarFileLoader.loadClass(PRELOADER_CLASS).getDeclaredMethod("main", Array<String>::class.java)
    }
    private val runnerMainMethod by lazy {
        System.getProperties().setProperty("kotlin.home", kotlinHome)
        jarFileLoader.addFile(File(joinToPathString(kotlinHome, "lib", "kotlin-runner.jar")))
        jarFileLoader.loadClass(RUNNER_MAIN_CLASS).getDeclaredMethod("main", Array<String>::class.java)
    }

    fun compile(compilerOpts: List<String>, targetJarFile: File, sourceFiles: List<File>, classpath: String?) {
        val baseArgs = listOf("-Xskip-runtime-version-check") +
                compilerOpts + listOf("-d", targetJarFile.absolutePath)
        val cpArgs = classPathArgs(classpath)
        runKotlinc(baseArgs + sourceFiles.map { it.absolutePath } + cpArgs)
    }

    fun runScript(scriptClassPath: String, execClassName: String, userArgs: List<String>, kotlinOpts: List<String>): Int {
        val runnerArgs = listOf("-cp", scriptClassPath, execClassName) + userArgs
        return if (kotlinOpts.isNotEmpty()) {
            execKotlin(kotlinOpts, runnerArgs)
        } else {
            runKotlin(runnerArgs)
            // it would be unreachable if the script calls exitProcess()
            0
        }
    }

    fun interactiveShell(jarFile: File, classpath: String?, compilerOpts: List<String>, kotlinOpts: List<String>) {
        val jarPath = if (jarFile.isFile) jarFile.absolutePath else null
        val args = compilerOpts + classPathArgs(classpath, jarPath)
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

    private fun execKotlinc(kotlinOpts: List<String>, args: List<String>): Int {
        return if (IS_WINDOWS) {
            execJava(kotlinOpts, compilerBaseArgs, args)
        } else {
            exec(listOf("kotlinc") + kotlinOpts + args)
        }
    }

    private fun runKotlin(args: Collection<String>) {
        runnerMainMethod.invoke(jarFileLoader, args.toTypedArray())
    }

    private fun execKotlin(kotlinOpts: List<String>, args: List<String>): Int {
        return if (IS_WINDOWS) {
            val runnerJar = joinToPathString(kotlinHome, "lib", "kotlin-runner.jar")
            val kotlinAppArgs = listOf("-Dkotlin.home=$kotlinHome", "-cp", runnerJar, RUNNER_MAIN_CLASS)
            execJava(kotlinOpts, kotlinAppArgs, args)
        } else {
            exec(listOf("kotlin") + kotlinOpts + args)
        }
    }

    private fun execJava(kotlinOpts: List<String>, kotlinAppArgs: List<String>, args: List<String>): Int {
        val (javaOpts, pureKotlinOpts) = kotlinOpts.partition { it.startsWith("-J") || it.startsWith("-D") }
        val pureJavaOpts = javaOpts.map { if (it.startsWith("-J")) it.substring(2) else it }
        val javaCmd = System.getenv("JAVACMD") ?: "java"
        return exec(listOf(javaCmd) + pureJavaOpts + kotlinAppArgs + pureKotlinOpts + args)
    }

    private fun exec(commandList: List<String>) =
            ProcessBuilder(commandList)
                    .inheritIO()
                    .apply { environment()["KOTLIN_RUNNER"] = "" }
                    .start()
                    .waitFor()
}

