package kscript.integration

import kscript.integration.tools.TestAssertion.contains
import kscript.integration.tools.TestAssertion.startsWith
import kscript.integration.tools.TestAssertion.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SimpleTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Providing source code works`() {
        verify("kscript \"println(1+1)\"", 0, "2\n")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Debugging information is printed`() {
        verify("kscript -d \"println(1+1)\"", 0, "2\n", contains("Debugging information for KScript"))
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Help is printed`() {
        //@formatter:off
        verify("kscript --help", 0, "", startsWith("kscript - Enhanced scripting support for Kotlin on *nix-based systems."))
        //@formatter:on
    }
}
