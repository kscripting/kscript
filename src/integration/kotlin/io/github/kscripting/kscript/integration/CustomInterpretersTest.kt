package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestContext.copyFile
import io.github.kscripting.shell.integration.tools.TestContext.execPath
import io.github.kscripting.shell.integration.tools.TestContext.projectPath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class CustomInterpretersTest : TestBase {
    @Test
    @Tag("posix")
    fun `Execute mydsl as interpreter`() {
        verify("mydsl \"println(foo)\"", 0, "bar[nl]", any())
    }

    @Test
    @Tag("posix")
    fun `Execute mydsl test with deps`() {
        verify((projectPath / "test/resources/custom_dsl/mydsl_test_with_deps.kts").stringPath(), 0, "foobar[nl]", any())
    }

    companion object {
        init {
            copyFile("test/resources/custom_dsl/mydsl", execPath)
        }
    }
}
