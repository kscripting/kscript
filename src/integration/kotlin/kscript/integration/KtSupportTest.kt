package kscript.integration

import kscript.integration.tools.TestAssertion.any
import kscript.integration.tools.TestAssertion.verify
import kscript.integration.tools.TestContext.projectDir
import kscript.integration.tools.TestContext.resolvePath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class KtSupportTest : TestBase {
    @Test
    @Tag("posix")
    fun `Run kt via interpreter mode`() {
        verify(resolvePath("$projectDir/test/resources/kt_tests/simple_app.kt"), 0, "main was called\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Run kt via interpreter mode with dependencies`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/kt_tests/main_with_deps.kt")}", 0, "made it!\n", "[kscript] Resolving log4j:log4j:1.2.14...\n")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    @Tag("msys")
    @Tag("windows")
    //TODO: Additional new lines are in stdout for cygwin
    fun `Test misc entry point with or without package configurations (no cygwin)`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/kt_tests/default_entry_nopckg.kt")}", 0, "main was called\n")
        verify("kscript ${resolvePath("$projectDir/test/resources/kt_tests/default_entry_withpckg.kt")}", 0, "main was called\n")
    }

    @Test
    @Tag("cygwin")
    fun `Test misc entry point with or without package configurations`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/kt_tests/custom_entry_nopckg.kt")}", 0, "foo companion was called\n")
        verify("kscript ${resolvePath("$projectDir/test/resources/kt_tests/custom_entry_withpckg.kt")}", 0, "foo companion was called\n")
    }

    @Test
    @Tag("posix")
    fun `Also make sure that kts in package can be run via kscript`() {
        verify(resolvePath("$projectDir/test/resources/script_in_pckg.kts"), 0, "I live in a package!\n")
    }
}
