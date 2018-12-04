import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkStatic
import kscript.app.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

// We can assert messages thrown during pre-processing but not the script result, which is currently in a separate process
class KscriptTest {

    @Before
    fun before() {
        clearCache()
    }

    @Test
    fun `should run a piped script`() {
        assumeTrue(kotlinIsAvailableInPath())

        main(arrayOf("""println("kotlin rocks")"""))
    }

    @Test
    fun `should run a script with only println`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("test/resources/dot.Test.kts"))
        // then
        newErr.toString().lines() shouldNotBe listOf(
                "[kscript] Resolving dependencies...",
                ""
        )
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should package a piped script`() {
        assumeTrue(kotlinIsAvailableInPath())
        assumeTrue(gradleIsAvailableInPath())

        val result: Int = exitStatus {
            main(arrayOf("--package", """println("kotlin rocks")"""))
        }

        result shouldBe 0
    }

    @Test
    fun `should run a script with multiple dependencies as comments`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("test/resources/multi_line_deps.kts"))
        // then
        newErr.toString().lines() shouldBe listOf(
                "[kscript] Resolving dependencies...",
                "[kscript]     Resolving com.offbytwo:docopt:0.6.0.20150202...Done",
                "[kscript]     Resolving log4j:log4j:1.2.14...Done",
                "[kscript] Dependencies resolved",
                ""
        )
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should run a script with dependencies as annotations`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("test/resources/depends_on_with_type.kts"))
        // then
        println(newErr)
        newErr.toString().lines() shouldBe listOf(
                "[kscript] Resolving dependencies...",
                "[kscript]     Resolving org.javamoney:moneta:1.3@pom...Done",
                "[kscript]     Resolving com.github.holgerbrandl:kscript-annotations:1.2...Done",
                "[kscript] Dependencies resolved",
                ""
        )
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should run a script with support API`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("-t", """println("hello")"""))
        // then
        newErr.toString().lines() shouldBe listOf(
                "[kscript] Resolving dependencies...",
                "[kscript]     Resolving com.github.holgerbrandl:kscript-support:1.2.5...Done",
                "[kscript] Dependencies resolved",
                ""
        )
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should run a script from an url`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts"))
        // then
        newErr.toString().trim().length shouldBe 0
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should resolve dependencies from script from an url`() {
        assumeTrue(kotlinIsAvailableInPath())
        // setup
        val (originalErr, newErr) = captureStdErr()
        // when
        main(arrayOf("https://git.io/fxHBv"))
        // then
        newErr.toString().lines() shouldBe listOf(
                "[kscript] Resolving dependencies...",
                "[kscript]     Resolving log4j:log4j:1.2.14...Done",
                "[kscript] Dependencies resolved",
                ""
        )
        // clean up
        restoreStdErr(originalErr)
    }

    @Test
    fun `should exit if kotlinc is not in PATH`() {
        assumeFalse(kotlinIsAvailableInPath())

        val result = exitStatus {
            main(arrayOf("println(\"kotlin rocks\")"))
        }

        result shouldBe 1
    }

    private fun kotlinIsAvailableInPath() = guessKotlinHome() != null

    private fun gradleIsAvailableInPath() = evalCommand("where gradle").exitCode != 1

    private fun captureStdErr(): Pair<PrintStream, ByteArrayOutputStream> {
        val originalErr = System.err
        val newErr = ByteArrayOutputStream()
        restoreStdErr(PrintStream(newErr))
        return Pair(originalErr, newErr)
    }

    private fun restoreStdErr(originalErr: PrintStream) {
        System.setErr(originalErr)
    }

    private fun exitStatus(arg: () -> Unit): Int {
        mockkStatic("kscript.app.AppHelpersKt")
        // workaround for `Nothing` methods: https://github.com/mockk/mockk/issues/187
        every {
            quit(1)
        } answers {
            throw RuntimeException("1")
        }
        every {
            quit(0)
        } answers {
            throw RuntimeException("0")
        }

        try {
            arg.invoke()
        } catch (e: RuntimeException) {
            return e.message?.toInt() ?: 0
        }
        return 0
    }
}
