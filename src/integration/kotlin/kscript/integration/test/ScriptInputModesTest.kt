package kscript.integration.test

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kscript.integration.Tools
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ScriptInputModesTest : TestBase {
    @Test
    @Tag("posix")
    fun `Make sure that scripts can be piped into kscript`() {
        val kscriptProcess = Tools.runProcess("source ${projectDir}/test/resources/direct_script_arg.sh")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(kscriptProcess.stdout.trim()).isEqualTo("kotlin rocks")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Also allow for empty programs`() {
        val kscriptProcess = Tools.runProcess("$kscript ''")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(kscriptProcess.stdout.trim()).isEmpty()
    }
}
