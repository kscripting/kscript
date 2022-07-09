package kscript.app.model

import java.nio.file.Path

data class Config(
    val osType: OsType,
    val selfName: String,
    val cacheDir: Path,
    val customPreamble: String,
    val intellijCommand: String,
    val gradleCommand: String,
    val kotlinHome: Path?,
    val classPathSeparator: Char,
    val hostPathSeparatorChar: Char,
    val shellPathSeparatorChar: Char,
    val homeDir: Path,
    val kotlinOptsEnvVariable: String,
    val repositoryUrlEnvVariable: String,
    val repositoryUserEnvVariable: String,
    val repositoryPasswordEnvVariable: String,
) {
    companion object {
        fun builder() = ConfigBuilder()
    }
}
