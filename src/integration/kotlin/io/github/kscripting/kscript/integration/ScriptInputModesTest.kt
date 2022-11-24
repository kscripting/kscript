package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.any
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.projectDir
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import io.github.kscripting.kscript.integration.tools.TestContext.testDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ScriptInputModesTest : TestBase {
    @Test
    @Tag("posix")
    fun `Make sure that scripts can be piped into kscript`() {
        verify("source $projectDir/test/resources/direct_script_arg.sh", 0, "kotlin rocks\n", "")
    }

    @Test
    @Tag("posix")
    //it doesn't work on Windows
    fun `Also allow for empty programs`() {
        verify("kscript ''", 0, "", "")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Provide script as direct argument`() {
        verify("""kscript "println(1+1)"""", 0, "2\n", "")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    @Tag("windows")
    //TODO: Doesn't work on msys, cygwin as during test execution " is replaced with '. It causes syntax error in Kotlin.
    fun `Use dashed arguments`() {
        verify("""kscript "println(args.joinToString(\"\"))" --arg u ments""", 0, "--arguments\n", "")
        verify("""kscript -s "print(args.joinToString(\"\"))" --arg u ments""", 0, "--arguments", "")
    }

    @Test
    @Tag("posix")
    fun `Provide script via stidin`() {
        verify("echo 'println(1+1)' | kscript -", 0, "2\n")
        //stdin and further switch (to avoid regressions of #94)
        verify("echo 'println(1+3)' | kscript - --foo", 0, "4\n")
    }

    @Test
    @Tag("windows")
    fun `Provide script via stidin (windows version without quotes)`() {
        verify("echo println(1+1) | kscript -", 0, "2\n")
        //stdin and further switch (to avoid regressions of #94)
        verify("echo println(1+3) | kscript - --foo", 0, "4\n")
    }

    @Test
    @Tag("posix")
    fun `Make sure that heredoc is accepted as argument`() {
        verify("source ${projectDir}/test/resources/here_doc_test.sh", 0, "hello kotlin\n")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    //Command substitution doesn't work on msys and cygwin
    fun `Make sure that command substitution works as expected`() {
        verify("source ${projectDir}/test/resources/cmd_subst_test.sh", 0, "command substitution works as well\n")
    }

    @Test
    @Tag("posix")
    fun `Make sure that it runs with local bash script files`() {
        verify("source ${projectDir}/test/resources/local_script_file.sh $testDir", 0, "kscript rocks!\n")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that it runs with local script files`() {
        verify(
            "kscript ${resolvePath("${projectDir}/test/resources/multi_line_deps.kts")}",
            0,
            "kscript is  cool!\n",
            "[kscript] Resolving com.offbytwo:docopt:0.6.0.20150202...\n[kscript] Resolving log4j:log4j:1.2.14...\n"
        )
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Scripts with dashes in the file name should work as well`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/dash-test.kts")}", 0, "dash alarm!\n")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Scripts with additional dots in the file name should work as well`() {
        //We also test inner uppercase letters in file name here by using .*T*est
        verify("kscript ${resolvePath("$projectDir/test/resources/dot.Test.kts")}", 0, "dot alarm!\n")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that it runs with remote URLs`() {
        verify(
            "kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts",
            0,
            "I came from the internet\n",
            any()
        )
//        verify("kscript https://git.io/fxHBv", 0, "main was called\n", "[kscript] Resolving log4j:log4j:1.2.14...\n")
        verify("kscript https://git.io/fxHBv", 0, "main was called\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Missing script gives always error on execution`() {
        verify(
            "kscript i_do_not_exist.kts", 1, "", "[kscript] [ERROR] Could not read script from 'i_do_not_exist.kts'\n"
        )
        verify(
            "kscript i_do_not_exist.kts", 1, "", "[kscript] [ERROR] Could not read script from 'i_do_not_exist.kts'\n"
        )
    }
}
