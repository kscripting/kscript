package io.github.kscripting.kscript.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.kscripting.kscript.model.OsConfig
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType
import org.junit.jupiter.api.Test

class VersionCheckerTest {
    private val somePath = OsPath.createOrThrow(OsType.LINUX, "/home/kscript/")
    private val osConfig =
        OsConfig(OsType.LINUX, "kscript", "intellij", "gradle", somePath, somePath, somePath, somePath)
    private val versionChecker = VersionChecker(osConfig)

    @Test
    fun `Assert that parsing remote kscript version works correctly`() {
        assertThat(versionChecker.parseRemoteKscriptVersion("asdfasadfas")).isEqualTo("")
        assertThat(versionChecker.parseRemoteKscriptVersion("")).isEqualTo("")
        assertThat(
            versionChecker.parseRemoteKscriptVersion(
                """
            something else 0
            "tag":"345",
            "tag_name":"v4.1.1"
            something else 1
            something else 2
        """.trimIndent()
            )
        ).isEqualTo("4.1.1")
    }

    @Test
    fun `Assert that parsing local kotlin version works correctly`() {
        assertThat(versionChecker.parseLocalKotlinAndJreVersion("asdfasadfas")).isEqualTo(Pair("-", "-"))
        assertThat(versionChecker.parseLocalKotlinAndJreVersion("")).isEqualTo(Pair("-", "-"))
        assertThat(versionChecker.parseLocalKotlinAndJreVersion("Kotlin version 1.7.20-release-201 (JRE 11.0.2+9)")).isEqualTo(
            Pair("1.7.20-release-201", "JRE 11.0.2+9")
        )
    }
}
