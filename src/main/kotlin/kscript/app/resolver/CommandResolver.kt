package kscript.app.resolver

import kscript.app.creator.JarArtifact
import kscript.app.model.CompilerOpt
import kscript.app.model.Config
import kscript.app.model.KotlinOpt
import kscript.app.model.Script
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class CommandResolver(private val config: Config, private val script: Script) {
    //Syntax for different OS-es:
    //LINUX:    kotlin  -classpath "/home/vagrant/workspace/Kod/Repos/kscript/test:/home/vagrant/.kscript/cache/jar_2ccd53e06b0355d3573a4ae8698398fe/scriplet.jar:/usr/local/sdkman/candidates/kotlin/1.6.21/lib/kotlin-script-runtime.jar" Main_Scriplet
    //GIT-BASH: kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //CYGWIN:   kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //WINDOWS:  kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet

    //Path conversion (Cygwin/mingw): cygpath -u "c:\Users\Admin"; /cygdrive/c/ - Cygwin; /c/ - Mingw
    //uname --> CYGWIN_NT-10.0 or MINGW64_NT-10.0-19043
    //How to find if mingw/cyg/win (second part): https://stackoverflow.com/questions/40877323/quickly-find-if-java-was-launched-from-windows-cmd-or-cygwin-terminal

    fun compileKotlin(jar: Path, dependencies: Set<Path>, filePaths: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val classpath = resolveClasspath(dependencies)
        val files = filePaths.joinToString(" ") { "'${it.absolutePathString()}'" }
        val kotlinc = resolveKotlinBinary("kotlinc")

        return "'$kotlinc' $compilerOptsStr $classpath -d '${jar.absolutePathString()}' $files"
    }

    fun executeKotlin(jarArtifact: JarArtifact, dependencies: Set<Path>, userArgs: List<String>): String {
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime =
            Paths.get("${config.kotlinHome}${config.separatorChar}lib${config.separatorChar}kotlin-script-runtime.jar")

        val dependenciesSet = buildSet<Path> {
            addAll(dependencies)
            add(jarArtifact.path)
            add(scriptRuntime)
        }

        val classpath = resolveClasspath(dependenciesSet)
        val kotlin = resolveKotlinBinary("kotlin")

        return "$kotlin $kotlinOptsStr $classpath ${jarArtifact.execClassName} $userArgsStr"
    }

    fun interactiveKotlinRepl(dependencies: Set<Path>): String {
        val compilerOptsStr = resolveCompilerOpts(script.compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(script.kotlinOpts)
        val classpath = resolveClasspath(dependencies)
        val kotlinc = resolveKotlinBinary("kotlinc")

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

    private fun resolveClasspath(dependencies: Set<Path>) = if (dependencies.isEmpty()) ""
    else "-classpath \"" + dependencies.joinToString(config.classPathSeparator) { it.absolutePathString() } + "\""

    private fun resolveKotlinBinary(binary: String) =
        if (config.kotlinHome != null) (config.kotlinHome / "bin" / binary).absolutePathString() else binary
}
