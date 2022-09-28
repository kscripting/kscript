package io.github.kscripting.kscript.resolver

import io.github.kscripting.kscript.creator.JarArtifact
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.KotlinOpt
import io.github.kscripting.kscript.model.OsConfig
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType
import io.github.kscripting.shell.model.toNativeOsPath

class CommandResolver(private val osConfig: OsConfig) {
    private val classPathSeparator =
        if (osConfig.osType.isWindowsLike() || osConfig.osType.isPosixHostedOnWindows()) ";" else ":"

    //Syntax for different OS-es:
    //LINUX:    /usr/local/sdkman/..../kotlin  -classpath "/home/vagrant/workspace/Kod/Repos/kscript/test:/home/vagrant/.kscript/cache/jar_2ccd53e06b0355d3573a4ae8698398fe/scriplet.jar:/usr/local/sdkman/candidates/kotlin/1.6.21/lib/kotlin-script-runtime.jar" Main_Scriplet
    //GIT-BASH: /c/Users/Admin/.sdkman/candidates/kotlin/current/bin/kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //CYGWIN:   /home/Admin/.sdkman/candidates/kotlin/current/bin/kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //WINDOWS:  C:\Users\Admin\.sdkman\candidates\kotlin\current\bin\kotlin  -classpath "C:\Users\Admin;C:\Users\Admin\.kscript\cache\jar_2ccd53e06b0355d3573a4ae8698398fe\scriplet.jar;C:\Users\Admin\.sdkman\candidates\kotlin\current\lib\kotlin-script-runtime.jar" Main_Scriplet
    //MACOS:

    //<command_path>kotlinc -classpath "p1:p2"
    //OS Conversion matrix
    //              command_path    command_quoting     classpath_path  classpath_separator     classpath_quoting       files_path      files_quoting   main_class_quoting   @arg_file
    //LINUX         native          no                  native          :                       "                       ?                                                    no
    //GIT-BASH      shell           no                  native          ;                       "                       ?                                                    no
    //CYGWIN        shell           no                  native          ;                       "                                                                            no
    //WINDOWS       native          no                  native          ;                       "                                                                            yes
    //MACOS         ?               ?                   ?               ?                       ?                                                                            no

    //Path conversion (Cygwin/mingw): cygpath -u "c:\Users\Admin"; /cygdrive/c/ - Cygwin; /c/ - Mingw
    //uname --> CYGWIN_NT-10.0 or MINGW64_NT-10.0-19043
    //How to find if mingw/cyg/win (second part): https://stackoverflow.com/questions/40877323/quickly-find-if-java-was-launched-from-windows-cmd-or-cygwin-terminal

    fun compileKotlin(
        jar: OsPath, dependencies: Set<OsPath>, filePaths: Set<OsPath>, compilerOpts: Set<CompilerOpt>
    ): String {
        val compilerOptsStr = resolveCompilerOpts(compilerOpts)
        val classpath = resolveClasspath(dependencies)
        val jarFile = resolveJarFile(jar)
        val files = resolveFiles(filePaths)
        val kotlinc = resolveKotlinBinary("kotlinc")

        return "$kotlinc $compilerOptsStr $classpath -d $jarFile $files"
    }

    fun executeKotlin(
        jarArtifact: JarArtifact, dependencies: Set<OsPath>, userArgs: List<String>, kotlinOpts: Set<KotlinOpt>
    ): String {
        val kotlinOptsStr = resolveKotlinOpts(kotlinOpts)
        val userArgsStr = resolveUserArgs(userArgs)
        val scriptRuntime = osConfig.kotlinHomeDir.resolve("lib/kotlin-script-runtime.jar")

        val dependenciesSet = buildSet {
            addAll(dependencies)
            add(jarArtifact.path)
            add(scriptRuntime)
        }

        val classpath = resolveClasspath(dependenciesSet)
        val kotlin = resolveKotlinBinary("kotlin")

        return "$kotlin $kotlinOptsStr $classpath ${jarArtifact.execClassName} $userArgsStr"
    }

    fun interactiveKotlinRepl(
        dependencies: Set<OsPath>, compilerOpts: Set<CompilerOpt>, kotlinOpts: Set<KotlinOpt>
    ): String {
        val compilerOptsStr = resolveCompilerOpts(compilerOpts)
        val kotlinOptsStr = resolveKotlinOpts(kotlinOpts)
        val classpath = resolveClasspath(dependencies)
        val kotlinc = resolveKotlinBinary("kotlinc")

        return "$kotlinc $compilerOptsStr $kotlinOptsStr $classpath"
    }

    fun executeIdea(projectPath: OsPath): String {
        return "${osConfig.intellijCommand} \"$projectPath\""
    }

    fun createPackage(): String {
        return "${osConfig.gradleCommand} makeScript"
    }

    private fun resolveKotlinOpts(kotlinOpts: Set<KotlinOpt>) = kotlinOpts.joinToString(" ") { it.value }

    private fun resolveCompilerOpts(compilerOpts: Set<CompilerOpt>) = compilerOpts.joinToString(" ") { it.value }

    private fun resolveJarFile(jar: OsPath): String {
        val jarFileQuotationMark: Char = when (osConfig.osType) {
            OsType.WINDOWS -> '"'
            else -> '\''
        }

        return "${jarFileQuotationMark}${resolveQuotedPath(jar)}${jarFileQuotationMark}"
    }

    private fun resolveFiles(filePaths: Set<OsPath>): String {
        val filePathQuotationMark: Char = when (osConfig.osType) {
            OsType.WINDOWS -> '"'
            else -> '\''
        }

        return filePaths.joinToString(" ") {
            "${filePathQuotationMark}${
                resolveQuotedPath(it)
            }${filePathQuotationMark}"
        }
    }

    private fun resolveUserArgs(userArgs: List<String>): String {
        val userArgQuotationMark: Char = when (osConfig.osType) {
            OsType.WINDOWS -> '"'
            else -> '\''
        }

        return userArgs.joinToString(" ") {
            "${userArgQuotationMark}${
                it.replace(
                    "\"", "\\\""
                )
            }${userArgQuotationMark}"
        }
    }

    private fun resolveClasspath(dependencies: Set<OsPath>): String {
        if (dependencies.isEmpty()) {
            return ""
        }

        val classpathParameterQuotationMark: Char = when (osConfig.osType) {
            OsType.WINDOWS -> '"'
            else -> '\''
        }

        val classpath = classpathParameterQuotationMark + dependencies.joinToString(classPathSeparator) {
            resolveQuotedPath(it)
        } + classpathParameterQuotationMark

        return "-classpath $classpath"
    }

    private fun resolveQuotedPath(osPath: OsPath): String = osPath.toNativeOsPath().stringPath()

    private fun resolveKotlinBinary(binary: String): String {
        return if (osConfig.osType.isWindowsLike()) "$binary.bat" else binary
    }
}
