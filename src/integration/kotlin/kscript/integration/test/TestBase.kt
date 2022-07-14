package kscript.integration.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kscript.app.util.ProcessResult
import kscript.integration.Tools
import org.junit.jupiter.api.BeforeAll

interface TestMatcher {
    fun matches(string: String): Boolean

    fun normalize(string: String) = string.replace("\n", TestBase.nl)
}

class AnyMatch : TestMatcher {
    override fun matches(string: String): Boolean = true
}

class StartsWith(private val expectedString: String, private val ignoreCase: Boolean) : TestMatcher {
    override fun matches(string: String): Boolean = string.startsWith(normalize(expectedString), ignoreCase)
}

class Contains(private val expectedString: String, private val ignoreCase: Boolean) : TestMatcher {
    override fun matches(string: String): Boolean = string.contains(normalize(expectedString), ignoreCase)
}

interface TestBase {
    val kscript: String get() = Companion.kscript
    val projectDir: String get() = Companion.projectDir
    val testDir: String get() = "projectDir/build/tmp/test"

    fun any() = AnyMatch()
    fun startsWith(string: String, ignoreCase: Boolean = false) = StartsWith(string, ignoreCase)
    fun contains(string: String, ignoreCase: Boolean = false) = Contains(string, ignoreCase)

    fun verify(
        command: String, exitCode: Int = 0, stdOutMatcher: TestMatcher, stdErr: String = ""
    ): ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(stdOutMatcher.matches(processResult.stdout)).isTrue()
        assertThat(processResult.stderr).isEqualTo(stdErr.replace("\n", nl))
        return processResult
    }

    fun verify(
        command: String, exitCode: Int = 0, stdOut: String, stdErrMatcher: TestMatcher
    ):ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(processResult.stdout).isEqualTo(stdOut.replace("\n", nl))
        assertThat(stdErrMatcher.matches(processResult.stderr)).isTrue()
        return processResult
    }

    fun verify(
        command: String, exitCode: Int = 0, stdOutMatcher: TestMatcher, stdErrMatcher: TestMatcher
    ): ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(stdOutMatcher.matches(processResult.stdout)).isTrue()
        assertThat(stdErrMatcher.matches(processResult.stderr)).isTrue()
        return processResult
    }

    fun verify(
        command: String, exitCode: Int = 0, stdOut: String = "", stdErr: String = ""
    ): ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(processResult.stdout).isEqualTo(stdOut.replace("\n", nl))
        assertThat(processResult.stderr).isEqualTo(stdErr.replace("\n", nl))
        return processResult
    }

    fun resolvePath(path: String): String = path

    fun runProcess(command: String): ProcessResult = Tools.runProcess(command)

    companion object {
        private val projectDir = Tools.resolveProjectDir()
        private val kscript = Tools.resolveKscript()
        val nl: String = System.getProperty("line.separator")

        @BeforeAll
        @JvmStatic
        fun setUp() {
            Tools.runProcess("$kscript --clear-cache")
        }
    }
}
