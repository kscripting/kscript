package kscript.integration

import kscript.integration.tools.TestAssertion.any
import kscript.integration.tools.TestAssertion.verify
import kscript.integration.tools.TestContext.copyToExecutablePath
import kscript.integration.tools.TestContext.projectDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class IdeaTest : TestBase {
    @Test
    @Tag("posix")
    fun `Temp projects with include symlinks`() {
        val result = verify("kscript --idea $projectDir/test/resources/includes/include_variations.kts", 0, any(), any())
        val ideaDir = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("cd $ideaDir && gradle build", 0, any(), any())
    }

    @Test
    @Tag("posix")
    fun `Support diamond-shaped include schemes (see #133)`() {
        val result = verify("kscript --idea $projectDir/test/resources/includes/diamond.kts", 0, any(), any())
        val ideaDir = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("cd $ideaDir && gradle build", 0, any(), any())
    }

    companion object {
        init {
            copyToExecutablePath("test/resources/idea")
        }
    }
}
