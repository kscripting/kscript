package io.github.kscripting.kscript.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class VersionCheckerTest {
    @Test
    fun `Assert that padding works for different versions`() {
        assertThat(VersionChecker.padVersion("4.1.1")).isEqualTo("004001001")
        assertThat(VersionChecker.padVersion("4.2.0-SNAPSHOT")).isEqualTo("004002000")
    }
}
