package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestAssertion.any
import io.github.kscripting.shell.integration.tools.TestAssertion.verify
import io.github.kscripting.shell.integration.tools.TestContext.copyFile
import io.github.kscripting.shell.integration.tools.TestContext.execPath
import io.github.kscripting.shell.integration.tools.TestContext.projectPath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class IdeaTest : TestBase {
    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: On MSys and Cygwin test doesn't work, and is accomplished with timeout
    fun `Temp projects with include symlinks`() {
        val result = verify(
            "kscript --idea ${projectPath / "test/resources/includes/include_variations.kts"}", 0, any(), any()
        )
        val ideaDir = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("cd $ideaDir && gradle build", 0, any(), any())
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: On MSys and Cygwin test doesn't work, and is accomplished with timeout
    fun `Support diamond-shaped include schemes (see #133)`() {
        val result =
            verify("kscript --idea ${projectPath / "test/resources/includes/diamond.kts"}", 0, any(), any())
        val ideaDir = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("cd $ideaDir && gradle build", 0, any(), any())
    }

    companion object {
        init {
            copyFile("test/resources/idea", execPath)
        }
    }
}
