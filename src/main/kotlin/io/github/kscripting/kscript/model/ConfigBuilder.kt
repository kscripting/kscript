package io.github.kscripting.kscript.model

import io.github.kscripting.kscript.util.ShellUtils
import io.github.kscripting.shell.model.*
import java.util.*
import kotlin.io.path.reader

@Suppress("MemberVisibilityCanBePrivate")
class ConfigBuilder(
    private val osType: OsType,
    private val systemProperties: Properties,
    private val environment: Map<String, String?>
) {
    var userHomeDir: OsPath? = null
    var tempDir: OsPath? = null
    var selfName: String? = null
    var kscriptDir: OsPath? = null
    var cacheDir: OsPath? = null
    var kotlinHomeDir: OsPath? = null
    var configFile: OsPath? = null
    var intellijCommand: String? = null
    var gradleCommand: String? = null
    var customPreamble: String? = null
    var providedKotlinOpts: String? = null
    var repositoryUrl: String? = null
    var repositoryUser: String? = null
    var repositoryPassword: String? = null
    var artifactsDir: OsPath? = null

    //Env variables paths read by Java are always in native format; All paths should be stored in Config as native,
    //and then converted to shell format as needed.
    //private fun path(path: String) = OsPath.createOrThrow(OsType.native, path)
    private fun String.toOsPathFromNative() = OsPath.createOrThrow(OsType.native, this)
    private fun String.toOsPathFromOsSpecific(osType: OsType) = OsPath.createOrThrow(osType, this).toNativeOsPath()
    private fun Properties.getPropertyOrNull(name: String) = this.getProperty(name).nullIfBlank()
    private fun Map<String, String?>.getEnvVariableOrNull(name: String) = this[name].nullIfBlank()
    private fun String?.nullIfBlank() = if (this.isNullOrBlank()) null else this

    fun build(): Config {
        val userHomeDir: OsPath = userHomeDir ?: systemProperties.getPropertyOrNull("user.home")?.toOsPathFromNative()
        ?: throw IllegalStateException("Undefined 'user.home' property")

        val tempDir: OsPath = tempDir ?: systemProperties.getPropertyOrNull("java.io.tmpdir")?.toOsPathFromNative()
        ?: throw IllegalStateException("Undefined 'java.io.tmpdir' property")

        val selfName: String = selfName ?: environment.getEnvVariableOrNull("KSCRIPT_NAME") ?: "kscript"

        val kscriptDir: OsPath? =
            kscriptDir ?: environment.getEnvVariableOrNull("KSCRIPT_DIRECTORY")?.toOsPathFromNative()

        val cacheDir: OsPath = cacheDir ?: kscriptDir?.resolve("cache") ?: when {
            osType.isWindowsLike() -> environment.getEnvVariableOrNull("LOCALAPPDATA")?.toOsPathFromNative() ?: tempDir
            osType == OsType.MACOS -> userHomeDir.resolve("Library", "Caches")
            else -> environment.getEnvVariableOrNull("XDG_CACHE_DIR")?.toOsPathFromNative()
                ?: userHomeDir.resolve(".cache")
        }.resolve("kscript")

        val kotlinHomeDir: OsPath =
            kotlinHomeDir ?: environment.getEnvVariableOrNull("KOTLIN_HOME")?.toOsPathFromNative()
            ?: ShellUtils.guessKotlinHome(osType)?.toOsPathFromNative()
            ?: throw IllegalStateException("KOTLIN_HOME is not set and could not be inferred from context.")

        val configFile: OsPath = configFile ?: kscriptDir?.resolve("kscript.properties") ?: when {
            osType.isWindowsLike() -> environment.getEnvVariableOrNull("LOCALAPPDATA")?.toOsPathFromNative()
                ?: userHomeDir.resolve(".config")

            osType == OsType.MACOS -> userHomeDir.resolve("Library", "Application Support")
            else -> environment.getEnvVariableOrNull("XDG_CONFIG_DIR")?.toOsPathFromNative()
                ?: userHomeDir.resolve(".config")
        }.resolve("kscript", "kscript.properties")

        val intellijCommand: String =
            intellijCommand ?: environment.getEnvVariableOrNull("KSCRIPT_COMMAND_IDEA") ?: "idea"

        val gradleCommand: String =
            gradleCommand ?: environment.getEnvVariableOrNull("KSCRIPT_COMMAND_GRADLE") ?: "gradle"

        val osConfig = OsConfig(
            osType,
            selfName,
            intellijCommand,
            gradleCommand,
            userHomeDir,
            configFile,
            cacheDir,
            kotlinHomeDir,
        )

        val configProperties = Properties().apply {
            if (configFile.exists()) {
                load(configFile.toNativePath().reader())
            }
        }

        val customPreamble = customPreamble ?: environment.getEnvVariableOrNull("KSCRIPT_PREAMBLE")
        ?: configProperties.getPropertyOrNull("scripting.preamble") ?: ""

        val providedKotlinOpts = providedKotlinOpts ?: environment.getEnvVariableOrNull("KSCRIPT_KOTLIN_OPTS")
        ?: configProperties.getPropertyOrNull("scripting.kotlin.opts") ?: ""

        val repositoryUrl = repositoryUrl ?: environment.getEnvVariableOrNull("KSCRIPT_REPOSITORY_URL")
        ?: configProperties.getPropertyOrNull(
            "scripting.repository.url"
        ) ?: ""

        val repositoryUser = repositoryUser ?: environment.getEnvVariableOrNull("KSCRIPT_REPOSITORY_USER")
        ?: configProperties.getPropertyOrNull("scripting.repository.user") ?: ""

        val repositoryPassword = repositoryPassword ?: environment.getEnvVariableOrNull("KSCRIPT_REPOSITORY_PASSWORD")
        ?: configProperties.getPropertyOrNull("scripting.repository.password") ?: ""

        val artifactsDir: OsPath? =
            artifactsDir ?: environment.getEnvVariableOrNull("KSCRIPT_DIRECTORY_ARTIFACTS")?.toOsPathFromNative()
            ?: configProperties.getPropertyOrNull("scripting.directory.artifacts")?.toOsPathFromOsSpecific(osType)

        val scriptingConfig = ScriptingConfig(
            customPreamble,
            providedKotlinOpts,
            repositoryUrl,
            repositoryUser,
            repositoryPassword,
            artifactsDir
        )

        return Config(osConfig, scriptingConfig)
    }
}
