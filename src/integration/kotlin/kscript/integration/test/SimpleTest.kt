package kscript.integration.test

import kscript.integration.tool.TestAssertion.startsWith
import kscript.integration.tool.TestAssertion.verify
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
    fun `Help is printed`() {
        //@formatter:off
        verify("kscript --help", 0, "", startsWith("kscript - Enhanced scripting support for Kotlin on *nix-based systems."))
        //@formatter:on
    }
}
