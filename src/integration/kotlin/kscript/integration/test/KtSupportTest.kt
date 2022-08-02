package kscript.integration.test

import kscript.integration.tool.TestAssertion.any
import kscript.integration.tool.TestAssertion.verify
import kscript.integration.tool.TestContext.projectDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class KtSupportTest : TestBase {
    @Test
    @Tag("posix")
    fun `Run kt via interpreter mode`() {
        verify("$projectDir/test/resources/kt_tests/simple_app.kt", 0, "main was called\n", any())
    }

    @Test
    @Tag("posix")
    fun `Run kt via interpreter mode with dependencies`() {
        verify("kscript $projectDir/test/resources/kt_tests/main_with_deps.kt", 0, "made it!\n", "[kscript] Resolving log4j:log4j:1.2.14...\n")
    }

    @Test
    @Tag("posix")
    fun `Test misc entry point with or without package configurations`() {
        verify("kscript $projectDir/test/resources/kt_tests/custom_entry_nopckg.kt", 0, "foo companion was called\n")
        verify("kscript $projectDir/test/resources/kt_tests/custom_entry_withpckg.kt", 0, "foo companion was called\n")
        verify("kscript $projectDir/test/resources/kt_tests/default_entry_nopckg.kt", 0, "main was called\n")
        verify("kscript $projectDir/test/resources/kt_tests/default_entry_withpckg.kt", 0, "main was called\n")
    }

    @Test
    @Tag("posix")
    fun `Also make sure that kts in package can be run via kscript`() {
        verify("$projectDir/test/resources/script_in_pckg.kts", 0, "I live in a package!\n")
    }
}
