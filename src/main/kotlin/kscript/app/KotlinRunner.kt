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

    fun runScript(scriptClassPath: String, execClassName: String, userArgs: List<String>): Int {
        val runnerArgs = listOf("-cp", scriptClassPath, execClassName) + userArgs
        runKotlin(runnerArgs)
        return 0
    }

    fun interactiveShell(jarFile: File, classpath: String?, compilerOpts: List<String>) {
        val jarPath = if (jarFile.isFile) jarFile.absolutePath else null
        val args = compilerOpts + classPathArgs(classpath, jarPath)
        runKotlinc(args)
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

    private fun runKotlin(args: Collection<String>) {
        runnerMainMethod.invoke(jarFileLoader, args.toTypedArray())
    }
}

