package kscript.app.model

import kscript.app.shell.OsPath
import kscript.app.shell.ShellUtils
import kscript.app.shell.exists
import kscript.app.shell.toNativePath
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

    //Env variables paths read by Java are always in native format; All paths should be stored in Config as native,
    //and then converted to shell format as needed.
    private fun path(path: String) = OsPath.createOrThrow(OsType.native, path)

    fun build(): Config {
        val osType = OsType.findOrThrow(requireNotNull(osType))

        val selfName = selfName ?: System.getenv("KSCRIPT_NAME") ?: "kscript"
        val intellijCommand = intellijCommand ?: System.getenv("KSCRIPT_COMMAND_IDEA") ?: "idea"
        val gradleCommand = gradleCommand ?: System.getenv("KSCRIPT_COMMAND_GRADLE") ?: "gradle"

        val kotlinHome = kotlinHome ?: resolveKotlinHome(osType)
        val homeDir = homeDir ?: path(System.getProperty("user.home")!!)
        val kscriptDir = System.getenv("KSCRIPT_DIR")?.let { path(it) }
        val configFile =
            configFile ?: kscriptDir?.resolve("kscript.properties")
            ?: resolveBaseConfigsDir(osType).resolve("kscript.properties")
        val cacheDir = cacheDir ?: kscriptDir?.resolve("cache") ?: resolveBaseCachesPath(osType).resolve("kscript")

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
        val customPreamble =
            customPreamble ?: System.getenv("KSCRIPT_PREAMBLE") ?: properties.getProperty("scripting.preamble") ?: ""
        val providedKotlinOpts =
            providedKotlinOpts ?: System.getenv("KSCRIPT_KOTLIN_OPTS")
            ?: properties.getProperty("scripting.kotlin.opts") ?: ""
        val repositoryUrl =
            repositoryUrl ?: System.getenv("KSCRIPT_REPOSITORY_URL")
            ?: properties.getProperty("scripting.repository.url") ?: ""
        val repositoryUser =
            repositoryUser ?: System.getenv("KSCRIPT_REPOSITORY_USER")
            ?: properties.getProperty("scripting.repository.user") ?: ""
        val repositoryPassword =
            repositoryPassword ?: System.getenv("KSCRIPT_REPOSITORY_PASSWORD")
            ?: properties.getProperty("scripting.repository.password") ?: ""

        val scriptingConfig = ScriptingConfig(
            customPreamble,
            providedKotlinOpts,
            repositoryUrl,
            repositoryUser,
            repositoryPassword,
        )

        return Config(osConfig, scriptingConfig)
    }

    private fun resolveKotlinHome(osType: OsType): OsPath = path(
        System.getenv("KOTLIN_HOME") ?: ShellUtils.guessKotlinHome(osType)
        ?: throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context.")
    )

    private fun resolveBaseConfigsDir(osType: OsType): OsPath = path(
        when {
            osType.isWindowsLike() -> System.getenv("LOCALAPPDATA")!!
            else -> System.getenv("XDG_CONFIG_DIR") ?: "${System.getProperty("user.home")}/.config"
        }
    )

    private fun resolveBaseCachesPath(osType: OsType): OsPath = path(
        when {
            osType.isWindowsLike() -> System.getenv("TEMP")!!
            else -> System.getenv("XDG_CACHE_DIR") ?: "${System.getProperty("user.home")}/.cache"
        }
    )
}
