package kscript.app.model

import kscript.app.util.OsPath
import kscript.app.util.ShellUtils
import kscript.app.util.exists
import kscript.app.util.toNativePath
import java.util.*
import kotlin.io.path.reader

class ConfigBuilder internal constructor() {
    var osType: String? = null
    var selfName: String? = null
    var configFile: OsPath? = null
    var cacheDir: OsPath? = null
    var customPreamble: String? = null
    var intellijCommand: String? = null
    var gradleCommand: String? = null
    var kotlinHome: OsPath? = null
    var homeDir: OsPath? = null
    var providedKotlinOpts: String? = null
    var repositoryUrl: String? = null
    var repositoryUser: String? = null
    var repositoryPassword: String? = null

    fun build(): Config {
        //Java resolved env variables paths are always in native format; All paths should be stored in Config as native,
        //and then converted as needed to shell format.

        val osType = OsType.findOrThrow(requireNotNull(osType))

        val selfName = selfName ?: System.getenv("KSCRIPT_NAME") ?: "kscript"
        val intellijCommand = intellijCommand ?: System.getenv("KSCRIPT_COMMAND_IDEA") ?: "idea"
        val gradleCommand = gradleCommand ?: System.getenv("KSCRIPT_COMMAND_GRADLE") ?: "gradle"

        val kotlinHome = kotlinHome
            ?: (System.getenv("KOTLIN_HOME") ?: ShellUtils.guessKotlinHome(osType))?.let {
                OsPath.createOrThrow(OsType.native, it)
            }
            ?: throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context.")

        val homeDir = homeDir ?: OsPath.createOrThrow(osType, System.getProperty("user.home"))
        val kscriptDir = System.getenv("KSCRIPT_DIR")?.let { OsPath.createOrThrow(osType, it) }
        val configFile = configFile
            ?: kscriptDir?.resolve("kscript.properties")
            ?: osType.configsDir.resolve("kscript.properties")
        val cacheDir = cacheDir
            ?: kscriptDir?.resolve("cache")
            ?: osType.cachesDir.resolve("kscript")

        val osConfig = OsConfig(
            osType,
            selfName,
            intellijCommand,
            gradleCommand,
            homeDir,
            configFile,
            cacheDir,
            kotlinHome,
        )

        val properties = Properties().apply {
            if (configFile.exists()) {
                load(configFile.toNativePath().reader())
            }
        }
        val customPreamble = customPreamble
            ?: System.getenv("KSCRIPT_PREAMBLE")
            ?: properties.getProperty("scripting.preamble")
            ?: ""
        val providedKotlinOpts = providedKotlinOpts
            ?: System.getenv("KSCRIPT_KOTLIN_OPTS")
            ?: properties.getProperty("scripting.kotlin.opts")
            ?: ""
        val repositoryUrl = repositoryUrl
            ?: System.getenv("KSCRIPT_REPOSITORY_URL")
            ?: properties.getProperty("scripting.repository.url")
            ?: ""
        val repositoryUser = repositoryUser
            ?: System.getenv("KSCRIPT_REPOSITORY_USER")
            ?: properties.getProperty("scripting.repository.user")
            ?: ""
        val repositoryPassword = repositoryPassword
            ?: System.getenv("KSCRIPT_REPOSITORY_PASSWORD")
            ?: properties.getProperty("scripting.repository.password")
            ?: ""

        val scriptingConfig = ScriptingConfig(
            customPreamble,
            providedKotlinOpts,
            repositoryUrl,
            repositoryUser,
            repositoryPassword,
        )

        return Config(osConfig, scriptingConfig)
    }

    companion object {
        private val OsType.configsDir
            get() = when {
                isWindowsLike() -> path(System.getenv("LOCALAPPDATA"))
                else -> path(System.getenv("XDG_CONFIG_DIR") ?: "${System.getProperty("user.home")}/.config")
            }

        private val OsType.cachesDir
            get() = when {
                isWindowsLike() -> path(System.getenv("TEMP"))
                else -> path(System.getenv("XDG_CACHE_DIR") ?: "${System.getProperty("user.home")}/.cache")
            }

        private fun OsType.path(path: String) = OsPath.createOrThrow(this, path)
    }
}
