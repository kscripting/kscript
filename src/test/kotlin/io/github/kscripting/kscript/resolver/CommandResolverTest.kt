package io.github.kscripting.kscript.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.kscripting.kscript.creator.JarArtifact
import io.github.kscripting.kscript.model.*
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType
import org.junit.jupiter.api.Test

class CommandResolverTest {
    private val compilerOpts = setOf(CompilerOpt("-abc"), CompilerOpt("-def"), CompilerOpt("--experimental"))
    private val kotlinOpts = setOf(KotlinOpt("-k1"), KotlinOpt("-k2"), KotlinOpt("--disable"))
    private val userArgs = listOf("arg", "u", "ments")

    @Test
    fun `Windows commands`() {
        val (config, jarPath, jarArtifact, depPaths, filePaths) = createTestData(
            OsType.WINDOWS,
            "C:\\My Workspace\\Code",
            "C:\\Users\\Admin\\scoop\\apps\\kotlin\\current"
        )
        val commandResolver = CommandResolver(config.osConfig)

        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
            """C:\Users\Admin\scoop\apps\kotlin\current\bin\kotlinc.bat -abc -def --experimental -classpath "C:\My Workspace\Code\.m2\somepath\dep1.jar;C:\My Workspace\Code\.m2\somepath\dep2.jar;C:\My Workspace\Code\.m2\somepath\dep3.jar" -d "C:\My Workspace\Code\.kscript\cache\somefile.jar" "C:\My Workspace\Code\source\somepath\dep1.kt" "C:\My Workspace\Code\source\somepath\dep2.kts""""
        )

        assertThat(commandResolver.executeKotlin(jarArtifact, depPaths, userArgs, kotlinOpts)).isEqualTo(
            """C:\Users\Admin\scoop\apps\kotlin\current\bin\kotlin.bat -k1 -k2 --disable -classpath "C:\My Workspace\Code\.m2\somepath\dep1.jar;C:\My Workspace\Code\.m2\somepath\dep2.jar;C:\My Workspace\Code\.m2\somepath\dep3.jar;C:\My Workspace\Code\.kscript\cache\somefile.jar;C:\Users\Admin\scoop\apps\kotlin\current\lib\kotlin-script-runtime.jar" mainClass "arg" "u" "ments""""
        )
    }

    @Test
    fun `Linux commands`() {
        val (config, jarPath, jarArtifact, depPaths, filePaths) = createTestData(
            OsType.LINUX,
            "/home/vagrant/",
            "/usr/local/kotlin/"
        )
        val commandResolver = CommandResolver(config.osConfig)

        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
            """/usr/local/kotlin/bin/kotlinc -abc -def --experimental -classpath '/home/vagrant/.m2/somepath/dep1.jar:/home/vagrant/.m2/somepath/dep2.jar:/home/vagrant/.m2/somepath/dep3.jar' -d '/home/vagrant/.kscript/cache/somefile.jar' '/home/vagrant/source/somepath/dep1.kt' '/home/vagrant/source/somepath/dep2.kts'"""
        )

        assertThat(commandResolver.executeKotlin(jarArtifact, depPaths, userArgs, kotlinOpts)).isEqualTo(
            """/usr/local/kotlin/bin/kotlin -k1 -k2 --disable -classpath '/home/vagrant/.m2/somepath/dep1.jar:/home/vagrant/.m2/somepath/dep2.jar:/home/vagrant/.m2/somepath/dep3.jar:/home/vagrant/.kscript/cache/somefile.jar:/usr/local/kotlin/lib/kotlin-script-runtime.jar' mainClass 'arg' 'u' 'ments'"""
        )
    }

    @Test
    fun `Msys commands`() {
        val (config, jarPath, jarArtifact, depPaths, filePaths) = createTestData(
            OsType.MSYS,
            "/c/My Workspace/",
            "/c/My Home/kotlin/"
        )
        val commandResolver = CommandResolver(config.osConfig)

        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
            """/c/My Home/kotlin/bin/kotlinc -abc -def --experimental -classpath 'c:\My Workspace\.m2\somepath\dep1.jar;c:\My Workspace\.m2\somepath\dep2.jar;c:\My Workspace\.m2\somepath\dep3.jar' -d 'c:\My Workspace\.kscript\cache\somefile.jar' 'c:\My Workspace\source\somepath\dep1.kt' 'c:\My Workspace\source\somepath\dep2.kts'"""
        )

        assertThat(commandResolver.executeKotlin(jarArtifact, depPaths, userArgs, kotlinOpts)).isEqualTo(
            """/c/My Home/kotlin/bin/kotlin -k1 -k2 --disable -classpath 'c:\My Workspace\.m2\somepath\dep1.jar;c:\My Workspace\.m2\somepath\dep2.jar;c:\My Workspace\.m2\somepath\dep3.jar;c:\My Workspace\.kscript\cache\somefile.jar;c:\My Home\kotlin\lib\kotlin-script-runtime.jar' mainClass 'arg' 'u' 'ments'"""
        )
    }

    @Test
    fun `Cygwin commands`() {
        val (config, jarPath, jarArtifact, depPaths, filePaths) = createTestData(
            OsType.CYGWIN,
            "/cygdrive/c/My Workspace/",
            "/cygdrive/c/My Home/kotlin/"
        )
        val commandResolver = CommandResolver(config.osConfig)

        assertThat(commandResolver.compileKotlin(jarPath, depPaths, filePaths, compilerOpts)).isEqualTo(
            """/cygdrive/c/My Home/kotlin/bin/kotlinc -abc -def --experimental -classpath 'c:\My Workspace\.m2\somepath\dep1.jar;c:\My Workspace\.m2\somepath\dep2.jar;c:\My Workspace\.m2\somepath\dep3.jar' -d 'c:\My Workspace\.kscript\cache\somefile.jar' 'c:\My Workspace\source\somepath\dep1.kt' 'c:\My Workspace\source\somepath\dep2.kts'"""
        )

        assertThat(commandResolver.executeKotlin(jarArtifact, depPaths, userArgs, kotlinOpts)).isEqualTo(
            """/cygdrive/c/My Home/kotlin/bin/kotlin -k1 -k2 --disable -classpath 'c:\My Workspace\.m2\somepath\dep1.jar;c:\My Workspace\.m2\somepath\dep2.jar;c:\My Workspace\.m2\somepath\dep3.jar;c:\My Workspace\.kscript\cache\somefile.jar;c:\My Home\kotlin\lib\kotlin-script-runtime.jar' mainClass 'arg' 'u' 'ments'"""
        )
    }

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

        val scriptingConfig = ScriptingConfig("", "", "", "", "", null)

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
