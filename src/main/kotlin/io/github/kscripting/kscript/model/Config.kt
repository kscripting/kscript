package io.github.kscripting.kscript.model

import io.github.kscripting.kscript.shell.OsPath
import io.github.kscripting.kscript.shell.OsType

data class ScriptingConfig(
    val customPreamble: String,
    val providedKotlinOpts: String,
    val providedRepositoryUrl: String,
    val providedRepositoryUser: String,
    val providedRepositoryPassword: String
) {
    override fun toString(): String {
        return """|ScriptingConfig {
                  |  customPreamble:                $customPreamble
                  |  providedKotlinOpts:            $providedKotlinOpts
                  |  providedRepositoryUrl:         $providedRepositoryUrl
                  |  providedRepositoryUser:        $providedRepositoryUser
                  |  providedRepositoryPassword:    $providedRepositoryPassword
                  |}
               """.trimMargin()
    }
}

data class OsConfig(
    val osType: OsType,
    val selfName: String,
    val intellijCommand: String,
    val gradleCommand: String,
    val userHomeDir: OsPath,
    val configFile: OsPath,
    val cacheDir: OsPath,
    val kotlinHomeDir: OsPath,
) {
    override fun toString(): String {
        return """|OsConfig {
                  |  osType:                $osType
                  |  selfName:              $selfName
                  |  intellijCommand:       $intellijCommand
                  |  gradleCommand:         $gradleCommand
                  |  userHomeDir:           $userHomeDir
                  |  configFile:            $configFile
                  |  cacheDir:              $cacheDir
                  |  kotlinHomeDir:         $kotlinHomeDir
                  |}
               """.trimMargin()
    }
}

data class Config(val osConfig: OsConfig, val scriptingConfig: ScriptingConfig) {
    override fun toString(): String {
        return """|$osConfig
                  |$scriptingConfig 
               """.trimMargin()
    }
}
