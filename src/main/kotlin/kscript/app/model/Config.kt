package kscript.app.model

import kscript.app.util.OsPath

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
    val kscriptConfigDir: OsPath,
    val kotlinHomeDir: OsPath,
) {
    override fun toString(): String {
        return """|OsConfig {
                  |  osType:                $osType
                  |  selfName:              $selfName
                  |  intellijCommand:       $intellijCommand
                  |  gradleCommand:         $gradleCommand
                  |  userHomeDir:           $userHomeDir
                  |  kscriptConfigDir:      $kscriptConfigDir
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

    companion object {
        fun builder() = ConfigBuilder()
    }
}
