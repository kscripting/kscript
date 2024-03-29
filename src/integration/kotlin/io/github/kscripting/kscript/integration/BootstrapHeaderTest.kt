package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.contains
import io.github.kscripting.kscript.integration.tools.TestAssertion.startsWith
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.copyToTestPath
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import io.github.kscripting.kscript.integration.tools.TestContext.testDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class BootstrapHeaderTest : TestBase {
    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: Doesn't work on msys and cygwin for some reason
    fun `Test adding bootstrap header`() {
        // ensure script works as is
        val testFile = resolvePath("$testDir/echo_stdin_args.kts")
        verify("echo stdin | '$testFile' --foo bar", 0, "stdin | script --foo bar\n")

        // add bootstrap header
        verify("kscript --add-bootstrap-header '$testFile'", 0, "", contains("echo_stdin_args.kts updated"))

        // ensure adding it again raises an error
        verify("kscript --add-bootstrap-header '$testFile'", 1, "", startsWith("[kscript] [ERROR] Bootstrap header already detected:"))

        // ensure scripts works with header, including stdin
        verify("echo stdin | '$testFile' --foo bar", 0, "stdin | script --foo bar\n")

        // ensure scripts works with header invoked with explicit `kscript`
        verify("echo stdin | kscript '$testFile' --foo bar", 0, "stdin | script --foo bar\n")
    }

    companion object {
        init {
            copyToTestPath("test/resources/echo_stdin_args.kts")
        }
    }
}
