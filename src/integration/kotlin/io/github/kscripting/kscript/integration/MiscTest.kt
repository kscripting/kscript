package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.any
import io.github.kscripting.kscript.integration.tools.TestAssertion.contains
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.projectDir
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import io.github.kscripting.kscript.integration.tools.TestContext.testDir
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class MiscTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Clearing cache test`() {
        verify("kscript --clear-cache", 0, "", "Cleaning up cache...\n")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    @Tag("msys")
    @Tag("windows")
    //TODO: Additional new lines are in stdout for cygwin
    fun `Prevent regressions of #98 (no cygwin)`() {
        verify("""kscript "print(args[0])" "foo bar"""", 0, "foo bar") //make sure quotes are not propagated into args
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Prevent regressions of #98 (it fails to process empty or space-containing arguments)`() {
        verify("""kscript "print(args.size)" foo bar""", 0, "2") //regular args
        verify(
            """kscript "print(args.size)" "--params foo"""",
            0,
            "1"
        ) //make sure dash args are not confused with options
        verify("""kscript "print(args.size)" "foo bar"""", 0, "1") //allow for spaces
    }

    @Test
    @Tag("posix")
    fun `Prevent regressions of #98 (only posix)`() {
        verify("""kscript "print(args.size)" "" foo bar""", 0, "3") //accept empty args
    }

    @Test
    @Tag("posix")
    fun `Prevent regression of #181`() {
        verify("""echo "println(123)" > $testDir/123foo.kts; kscript $testDir/123foo.kts""", 0, "123\n")
    }

    @Test
    @Tag("linux")
    @Tag("macos")
    @Tag("msys")
    //TODO: @Tag("cygwin") - doesn't work on cygwin
    fun `Prevent regression of #185`() {
        verify("source $projectDir/test/resources/home_dir_include.sh $testDir", 0, "42\n")
    }

    @Test
    @Tag("posix")
    fun `Prevent regression of #173`() {
        verify("source $projectDir/test/resources/compiler_opts_with_includes.sh $testDir", 0, "hello42\n", any())
    }

    @Test
    @Tag("posix")
    fun `Ensure relative includes with in shebang mode`() {
        verify("$projectDir/test/resources/includes/shebang_mode_includes", 0, "include_1\n")
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Ensure that compilation errors are not cached #349`() {
        //first run (not yet cached)
        verify("kscript $projectDir/test/resources/invalid_script.kts", 1, "", contains("error: expecting ')'"))
        //real test
        verify("kscript $projectDir/test/resources/invalid_script.kts", 1, "", contains("error: expecting ')'"))
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Test local jar dir referenced in ENV variable`() {
        val shellPath = resolvePath("$projectDir/test/resources/config/")

        verify(
            "kscript ${shellPath.resolve("script_with_local_jars.kts")}",
            0,
            "I am living in Test1 class...\nAnd I come from Test2 class...\n",
            ""
        ) { env ->
            env["KSCRIPT_DIRECTORY_ARTIFACTS"] = shellPath.resolve("jars").stringPath()
        }
    }
}
