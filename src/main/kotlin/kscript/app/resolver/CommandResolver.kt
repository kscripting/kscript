package kscript.app.resolver

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Config
import kscript.app.model.KotlinOpt
import kscript.app.model.Script
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class CommandResolver(private val config: Config, private val script: Script) {
    fun compileKotlin(jar: Path, dependencies: Set<Path>, filePaths: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val classpath = resolveClasspath(dependencies)
        val files = filePaths.joinToString(" ") { it.absolute().toString() }

        val kotlinc = if (config.kotlinHome != null) (config.kotlinHome / "bin" / "kotlinc").toString() else "kotlinc"

        return "$kotlinc $compilerOptsStr $classpath -d '${jar.absolute()}' $files"
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>): String {
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime =
            Paths.get("${config.kotlinHome}${config.separatorChar}lib${config.separatorChar}kotlin-script-runtime.jar")

//        val dependenciesSet = buildSet<Path> {
//            addAll(dependencies)
//            add(jarArtifact.path)
//            add(scriptRuntime)
//        }

        val dependenciesSet = mutableSetOf<Path>()
        dependenciesSet.addAll(dependencies)
        dependenciesSet.add(jarArtifact.path)
        dependenciesSet.add(scriptRuntime)

        val classpath = resolveClasspath(dependenciesSet)

        val kotlin = if (config.kotlinHome != null) (config.kotlinHome / "bin" / "kotlin").toString() else "kotlin"

        return "$kotlin $kotlinOptsStr $classpath ${jarArtifact.execClassName} $userArgsStr"
    }

    fun interactiveKotlinRepl(dependencies: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val classpath = resolveClasspath(dependencies)

        val kotlinc = if (config.kotlinHome != null) (config.kotlinHome / "bin" / "kotlinc").toString() else "kotlinc"

        return "$kotlinc $compilerOptsStr $kotlinOptsStr $classpath"
    }

    fun executeIdea(projectPath: Path): String {
        return "${config.intellijCommand} \"$projectPath\""
    }

    fun createPackage(projectPath: Path): String {
        return "cd '${projectPath}' && ${config.gradleCommand} simpleCapsule"
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }
    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }
    private fun resolveUserArgs(userArgs: List<String>) =
        userArgs.joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }

    private fun resolveClasspath(dependencies: Set<Path>) =
        if (dependencies.isEmpty()) "" else "-classpath " + dependencies.joinToString(config.classPathSeparator) { it.absolutePathString() }
}
