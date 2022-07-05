package kscript.integration.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import kscript.integration.Tools
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SimpleTest {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Providing source code works`() {
        val kscriptProcess = Tools.runProcess("$kscript \"println(1+1)\"")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(kscriptProcess.stdout.trim()).isEqualTo("2")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Help is printed`() {
        val kscriptProcess = Tools.runProcess("$kscript --help")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(kscriptProcess.stderr.trim()).startsWith("kscript - Enhanced scripting support for Kotlin on *nix-based systems.")
    }

    companion object {
        private val projectDir = Tools.resolveProjectDir()
        private val kscript = Tools.resolveKscript()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            Tools.runProcess("$kscript --clear-cache")
        }
    }
}
