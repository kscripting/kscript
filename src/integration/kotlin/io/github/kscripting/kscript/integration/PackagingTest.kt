package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.any
import io.github.kscripting.kscript.integration.tools.TestAssertion.startsWith
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.projectDir
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class PackagingTest : TestBase {
    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: doesn't work on msys, cygwin, windows
    fun `Packaged script is cached`() {
        //@formatter:off
        verify("kscript --package \"println(1+1)\"", 0, "", startsWith("[kscript] Packaging script 'scriplet' into standalone executable..."))
        verify("kscript --package \"println(1+1)\"", 0, "", startsWith("[kscript] Packaged script 'scriplet' available at path:"))
        //@formatter:on
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: doesn't work on msys, cygwin, windows
    fun `Packaging of simple script`() {
        val result =
            verify("kscript --package ${resolvePath("$projectDir/test/resources/package_example.kts")}", 0, "", any())
        val command = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("$command argument", 0, "package_me_args_1_mem_536870912\n")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    //TODO: doesn't work on msys, cygwin, windows
    fun `Packaging provided source code and execution with arguments`() {
        val result = verify("""kscript --package "println(args.size)"""", 0, "", any())
        val command = result.stderr.trim().lines().last().removePrefix("[kscript] ")
        verify("$command three arg uments", 0, "3\n")
    }
}
