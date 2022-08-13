package kscript.app.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.app.util.OsPath
import org.junit.jupiter.api.Test

internal class ConfigBuilderTest {
    @Test
    fun `Reads all properties of ScriptingConfig from given file`() {
        val linux = OsType.LINUX
        val config = ConfigBuilder().apply {
            osType = linux.osName
            configFile = OsPath.createOrThrow(linux, "test", "resources", "config", "kscript.properties")
        }.build()

        assertThat(config.scriptingConfig).isEqualTo(ScriptingConfig(
            customPreamble = """
                // declare dependencies
                @file:DependsOn("com.github.holgerbrandl:kutils:0.12")
                
                // make sure to also support includes in here
                // @file:Include("util.kt")
                @file:Include("https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/custom_dsl/test_dsl_include.kt")
                
                
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
