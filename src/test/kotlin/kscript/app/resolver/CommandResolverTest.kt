package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.creator.JarArtifact
import kscript.app.model.*
import kscript.app.shell.OsPath
import org.junit.jupiter.api.Test

class CommandResolverTest {
    private val compilerOpts = setOf(CompilerOpt("-abc"), CompilerOpt("-def"), CompilerOpt("--experimental"))
    private val kotlinOpts = setOf(KotlinOpt("-k1"), KotlinOpt("-k2"), KotlinOpt("--disable"))
    private val userArgs = listOf("arg", "u", "ments")

    private data class TestData(
        val config: Config,
        val jarPath: OsPath,
        val jarArtifact: JarArtifact,
        val depPaths: Set<OsPath>,
        val filePaths: Set<OsPath>
    )

    private fun createTestData(osType: OsType, homeDirString: String, kotlinDirString: String): TestData {
        val homeDir: OsPath = OsPath.createOrThrow(osType, homeDirString)
        val kotlinDir: OsPath = OsPath.createOrThrow(osType, kotlinDirString)

        val osConfig = OsConfig(
            osType,
            "kscript",
            "idea",
            "gradle",
            homeDir,
            homeDir.resolve("./.config/"),
            homeDir.resolve("./.cache/"),
            kotlinDir,
        )

        val scriptingConfig = ScriptingConfig("", "", "", "", "")

        val jarPath = osConfig.userHomeDir.resolve(".kscript/cache/somefile.jar")
        val depPaths = sortedSetOf(
            compareBy { it.stringPath() },
            osConfig.userHomeDir.resolve(".m2/somepath/dep1.jar"),
            osConfig.userHomeDir.resolve(".m2/somepath/dep2.jar"),
            osConfig.userHomeDir.resolve(".m2/somepath/dep3.jar")
        )
        val filePaths = sortedSetOf(
            compareBy { it.stringPath() },
            osConfig.userHomeDir.resolve("source/somepath/dep1.kt"),
            osConfig.userHomeDir.resolve("source/somepath/dep2.kts")
        )

        return TestData(
            Config(osConfig, scriptingConfig), jarPath, JarArtifact(jarPath, "mainClass"), depPaths, filePaths
        )
    }
}
