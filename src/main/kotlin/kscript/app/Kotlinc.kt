package kscript.app

import java.io.File

class Kotlinc(val KOTLIN_HOME: String) {
    val cl = JarFileLoader()
    val preloaderJar = File("${KOTLIN_HOME}${File.separatorChar}lib${File.separatorChar}kotlin-preloader.jar")
    init {
        cl.addFile(preloaderJar)
    }
    val baseArgs = listOf<String>("-cp", "${KOTLIN_HOME}${File.separatorChar}lib${File.separatorChar}kotlin-compiler.jar",
            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
    val preloaderMain = cl.loadClass("org.jetbrains.kotlin.preloading.Preloader").getDeclaredMethod("main", Array<String>::class.java)

    fun classPathArgs(vararg paths: String?): List<String> {
        val combinedClasspath = listOfNotNull(*paths).filter { it.isNotEmpty() }.joinToString(CP_SEPARATOR_CHAR)
        return if (combinedClasspath.isNotEmpty()) {
            listOf("-cp", combinedClasspath)
        } else listOf()
    }

    fun runKotlinc(args: Collection<String>) {
        preloaderMain.invoke(cl, (baseArgs + args).toTypedArray())
    }

    fun compile(jarFile: File, scriptFile: File, wrapperFile: File?, classpath: String?) {
        val baseArgs = listOf<String>("-Xskip-runtime-version-check",
                "-d", jarFile.absolutePath, scriptFile.absolutePath)
        val wrapperArgs =  listOfNotNull(wrapperFile?.absolutePath)
        val cpArgs = classPathArgs(classpath)
        runKotlinc(baseArgs + wrapperArgs + cpArgs)
    }

    fun interactiveShell(jarFile: File, classpath: String?) {
        val jarPath = if (jarFile.isFile) jarFile.absolutePath else null
        val args = classPathArgs(classpath, jarPath)
        runKotlinc(args)
    }
}
