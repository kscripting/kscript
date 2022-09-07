package kscript.app.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.shell.OsPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class ConfigBuilderTest {
    private val currentNativeOsType = OsType.native
    private val systemProperties = Properties()
    private val systemEnvironment = mutableMapOf<String, String>()

    @BeforeEach
    fun setUp() {
        systemProperties.put("user.home", "/home/vagrant")
        systemProperties.put("java.io.tmpdir", "/home/vagrant/tmp")
    }

    @Test
    fun `Reads all properties of ScriptingConfig from given file`() {

        val config = ConfigBuilder(currentNativeOsType, systemProperties, systemEnvironment).apply {
            configFile = OsPath.createOrThrow(currentNativeOsType, "test", "resources", "config", "kscript.properties")
            kotlinHomeDir = OsPath.createOrThrow(currentNativeOsType, ".")
        }.build()

        assertThat(config.scriptingConfig).isEqualTo(ScriptingConfig(
            customPreamble = """
                // declare dependencies
                @file:DependsOn("com.github.holgerbrandl:kutils:0.12")
                
                // make sure to also support includes in here
                // @file:Import("util.kt")
                @file:Import("https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/custom_dsl/test_dsl_include.kt")
                
                
                // define some important variables to be used throughout the dsl
                val foo = "bar"
            """.trimIndent(),
            providedKotlinOpts = "-J-Xmx4g",
            providedRepositoryUrl = "https://repository.example",
            providedRepositoryUser = "user",
            providedRepositoryPassword = "password",
        ))
    }
}
