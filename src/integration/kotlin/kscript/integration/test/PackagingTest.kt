package kscript.integration.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import kscript.integration.Tools
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class PackagingTest : TestBase {
    @Test
    @Tag("posix")
    fun `Packaged script is cached`() {
        var kscriptProcess = Tools.runProcess("$kscript --package \"println(1+1)\"")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(
            kscriptProcess.stderr.trim().lines().first()
        ).isEqualTo("[kscript] Packaging script 'scriplet' into standalone executable...")


        kscriptProcess = Tools.runProcess("$kscript --package \"println(1+1)\"")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)
        assertThat(
            kscriptProcess.stderr.trim().lines().first()
        ).isEqualTo("[kscript] Packaged script 'scriplet' available at path:")
    }

    @Test
    @Tag("posix")
    fun `Packaging of simple script`() {
        val kscriptProcess =
            Tools.runProcess("$kscript --package ${Tools.resolvePath("$projectDir/test/resources/package_example.kts")}")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)

        val command = kscriptProcess.stderr.trim().lines().last().removePrefix("[kscript] ")

        val packagingExecutionProcess = Tools.runProcess("$command argument")
        assertThat(packagingExecutionProcess.exitCode).isEqualTo(0)
        assertThat(packagingExecutionProcess.stdout.trim()).isEqualTo("package_me_args_1_mem_536870912")
    }

    @Test
    @Tag("posix")
    fun `Packaging provided source code and execution with arguments`() {
        val kscriptProcess = Tools.runProcess("$kscript --package \"println(args.size)\"")
        assertThat(kscriptProcess.exitCode).isEqualTo(0)

        val command = kscriptProcess.stderr.trim().lines().last().removePrefix("[kscript] ")

        val packagingExecutionProcess = Tools.runProcess("$command three arg uments")
        assertThat(packagingExecutionProcess.exitCode).isEqualTo(0)
        assertThat(packagingExecutionProcess.stdout.trim()).isEqualTo("3")
    }
}
