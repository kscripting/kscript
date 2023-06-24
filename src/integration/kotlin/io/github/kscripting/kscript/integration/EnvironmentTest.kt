package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestAssertion.verify
import io.github.kscripting.shell.integration.tools.TestContext.projectPath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class EnvironmentTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that KOTLIN_HOME can be guessed from kotlinc correctly`() {
        verify("kscript \"println(99)\"", 0, "99\n") { env -> env.remove("KOTLIN_HOME") }
    }

    //TODO: test what happens if kotlin/kotlinc/java/gradle/idea is not in PATH

    @Test
    @Tag("posix")
    fun `Run script that tries to find out its own filename via environment variable`() {
        val path = (projectPath / "test/resources/uses_self_file_name.kts").toString()
        verify(path, 0, "Usage: uses_self_file_name.kts [-ae] [--foo] file+\n")
    }
}
