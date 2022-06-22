package kscript.app.resolver

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.creator.JarArtifact
import kscript.app.model.*
import kscript.app.util.OsPath
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
            """/usr/local/kotlin/bin/kotlinc -abc -def --experimental -classpath "/home/vagrant/.m2/somepath/dep1.jar:/home/vagrant/.m2/somepath/dep2.jar:/home/vagrant/.m2/somepath/dep3.jar" -d '/home/vagrant/.kscript/cache/somefile.jar' '/home/vagrant/source/somepath/dep1.kt' '/home/vagrant/source/somepath/dep2.kts'"""
        )

        assertThat(commandResolver.executeKotlin(jarArtifact, depPaths, userArgs, kotlinOpts)).isEqualTo(
            """/usr/local/kotlin/bin/kotlin -k1 -k2 --disable -classpath "/home/vagrant/.m2/somepath/dep1.jar:/home/vagrant/.m2/somepath/dep2.jar:/home/vagrant/.m2/somepath/dep3.jar:/home/vagrant/.kscript/cache/somefile.jar:/usr/local/kotlin/lib/kotlin-script-runtime.jar" mainClass "arg" "u" "ments""""
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

//        when (osType) {
//            OsType.LINUX, OsType.MAC, OsType.FREEBSD -> {
//                homeDir = OsPath.createOrThrow(osType, "/My Workspace/Code")
//                kotlinDir = OsPath.createOrThrow(osType, "/usr/local/sdkman/candidates/kotlin/1.6.21")
//            }
//            OsType.WINDOWS -> {
//                homeDir = OsPath.createOrThrow(osType, "C:\\My Workspace\\Code")
//                kotlinDir = OsPath.createOrThrow(osType, "/usr/local/sdkman/candidates/kotlin/1.6.21")
//            }
//            OsType.CYGWIN -> {
//                homeDir = OsPath.createOrThrow(osType, "/cygdrive/c/My Workspace/Code")
//                kotlinDir = OsPath.createOrThrow(osType, "/usr/local/sdkman/candidates/kotlin/1.6.21")
//            }
//            OsType.MSYS -> {
//                homeDir = OsPath.createOrThrow(osType, "/c/My Workspace/Code")
//                kotlinDir = OsPath.createOrThrow(osType, "/usr/local/sdkman/candidates/kotlin/1.6.21")
//            }
//        }

        val osConfig = OsConfig(
            osType,
            "kscript",
            "idea",
            "gradle",
            homeDir,
            homeDir.resolve("./.kscript/"),
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
