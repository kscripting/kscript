package kscript.app.model

import kscript.app.util.ShellUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

class ConfigBuilder internal constructor() {
    var osType: String? = null
    var classPathSeparator: Char? = null
    var hostPathSeparatorChar: Char? = null
    var shellPathSeparatorChar: Char? = null
    var selfName: String? = null
    var cacheDir: Path? = null
    var customPreamble: String? = null
    var intellijCommand: String? = null
    var gradleCommand: String? = null
    var kotlinHome: Path? = null
    var homeDir: Path? = null
    var kotlinOptsEnvVariable: String? = null
    var repositoryUrlEnvVariable: String? = null
    var repositoryUserEnvVariable: String? = null
    var repositoryPasswordEnvVariable: String? = null

    fun build(): Config {
        //Java resolved env variables paths are always in native format; All paths should be stored in Config as native,
        //and then converted as needed to shell format.

        val osType = OsType.findOrThrow(requireNotNull(osType))
        val classPathSeparator = classPathSeparator ?: if (osType.isWindowsLike() || osType.isUnixHostedOnWindows()) ';' else ':'
        val hostPathSeparatorChar = hostPathSeparatorChar ?: File.separatorChar
        val shellPathSeparatorChar = shellPathSeparatorChar ?: if (osType.isUnixHostedOnWindows()) '/' else hostPathSeparatorChar
        val selfName = selfName ?: System.getenv("KSCRIPT_NAME") ?: "kscript"
        val home = System.getProperty("user.home")
        val cacheDir = cacheDir
            // from previous versions
            ?: System.getenv("KSCRIPT_DIR")?.let { Paths.get(it).resolve("cache") }
            // https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
            ?: Paths.get(System.getenv("XDG_CACHE_HOME") ?: "$home/.cache").resolve("kscript")
        val customPreamble = customPreamble ?: System.getenv("CUSTOM_KSCRIPT_PREAMBLE") ?: ""
        val intellijCommand = intellijCommand ?: System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea"
        val gradleCommand = gradleCommand ?: System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle"
        val kotlinHome = kotlinHome ?: (System.getenv("KOTLIN_HOME") ?: ShellUtils.guessKotlinHome(osType))?.let { Paths.get(it).absolute() }
        val homeDir = homeDir ?: Paths.get(home)
        val kotlinOptsEnvVariable = kotlinOptsEnvVariable ?: System.getenv("KSCRIPT_KOTLIN_OPTS") ?: ""
        val repositoryUrlEnvVariable = repositoryUrlEnvVariable ?: System.getenv("KSCRIPT_REPOSITORY_URL") ?: ""
        val repositoryUserEnvVariable = repositoryUserEnvVariable ?: System.getenv("KSCRIPT_REPOSITORY_USER") ?: ""
        val repositoryPasswordEnvVariable = repositoryPasswordEnvVariable ?: System.getenv("KSCRIPT_REPOSITORY_PASSWORD") ?: ""

        return Config(
            osType,
            selfName,
            cacheDir,
            customPreamble,
            intellijCommand,
            gradleCommand,
            kotlinHome,
            classPathSeparator,
            hostPathSeparatorChar,
            shellPathSeparatorChar,
            homeDir,
            kotlinOptsEnvVariable,
            repositoryUrlEnvVariable,
            repositoryUserEnvVariable,
            repositoryPasswordEnvVariable
        )
    }
}
