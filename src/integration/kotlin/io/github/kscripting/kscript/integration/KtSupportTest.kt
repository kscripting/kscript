package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestContext.projectPath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class KtSupportTest : TestBase {
    @Test
    @Tag("posix")
    fun `Run kt via interpreter mode`() {
        verify((projectPath / "test/resources/kt_tests/simple_app.kt").stringPath(), 0, "main was called[nl]", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Run kt via interpreter mode with dependencies`() {
        verify(
            "kscript ${projectPath / "test/resources/kt_tests/main_with_deps.kt"}",
            0,
            "made it![nl]",
            "[kscript] Resolving log4j:log4j:1.2.14...[nl]"
        )
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    @Tag("msys")
    @Tag("windows")
    //TODO: Additional new lines are in stdout for cygwin
    fun `Test misc entry point with or without package configurations (no cygwin)`() {
        verify(
            "kscript ${projectPath / "test/resources/kt_tests/default_entry_nopckg.kt"}",
            0,
            "main was called[nl]",
            any()
        )
        verify(
            "kscript ${projectPath / "test/resources/kt_tests/default_entry_withpckg.kt"}",
            0,
            "main was called[nl]",
            any()
        )
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Test misc entry point with or without package configurations`() {
        verify(
            "kscript ${projectPath / "test/resources/kt_tests/custom_entry_nopckg.kt"}",
            0,
            "foo companion was called[nl]"
        )
        verify(
            "kscript ${projectPath / "test/resources/kt_tests/custom_entry_withpckg.kt"}",
            0,
            "foo companion was called[nl]"
        )
    }

    @Test
    @Tag("posix")
    fun `Also make sure that kts in package can be run via kscript`() {
        verify((projectPath / "test/resources/script_in_pckg.kts").stringPath(), 0, "I live in a package![nl]", any())
    }
}
