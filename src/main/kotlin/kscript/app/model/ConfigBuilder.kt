package kscript.app.model

import kscript.app.util.ShellUtils.guessKotlinHome
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class ConfigBuilder internal constructor() {
    var selfName: String = System.getenv("CUSTOM_KSCRIPT_NAME") ?: "kscript"
    var kscriptDir: Path = Paths.get(System.getenv("KSCRIPT_DIR") ?: (System.getProperty("user.home")!! + "/.kscript"))
    var customPreamble: String = System.getenv("CUSTOM_KSCRIPT_PREAMBLE") ?: ""
    var intellijCommand: String = System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea"
    var gradleCommand: String = System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle"
    var kotlinHome: Path? = (System.getenv("KOTLIN_HOME") ?: guessKotlinHome())?.let { Paths.get(it).absolute() }
    var osName: String = System.getProperty("os.name")
    var homeDir: Path = Paths.get(System.getProperty("user.home")!!)
    var classPathSeparator: String = if (osName.lowercase().contains("windows")) ";" else ":"
    var separatorChar: Char = File.separatorChar
    var kotlinOptsEnvVariable = System.getenv("KSCRIPT_KOTLIN_OPTS") ?: ""
    var repositoryUrlEnvVariable = System.getenv("KSCRIPT_REPOSITORY_URL") ?: ""
    var repositoryUserEnvVariable = System.getenv("KSCRIPT_REPOSITORY_USER") ?: ""
    var repositoryPasswordEnvVariable = System.getenv("KSCRIPT_REPOSITORY_PASSWORD") ?: ""

    fun build(): Config {
        return Config(
            selfName,
            kscriptDir,
            customPreamble,
            intellijCommand,
            gradleCommand,
            kotlinHome,
            osName,
            classPathSeparator,
            separatorChar,
            homeDir,
            kotlinOptsEnvVariable,
            repositoryUrlEnvVariable,
            repositoryUserEnvVariable,
            repositoryPasswordEnvVariable
        )
    }
}
