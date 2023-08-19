package io.github.kscripting.kscript.integration

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SimpleTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Providing source code works`() {
        verify("kscript \"println(1+1)\"", 0, "2[nl]")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Debugging information is printed`() {
        verify("kscript -d \"println(1+1)\"", 0, "2[nl]", contains("Debugging information for KScript"))
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Help is printed`() {
        verify("kscript --help", 0, "", startsWith("kscript - Enhanced scripting support for Kotlin"))
    }
}
