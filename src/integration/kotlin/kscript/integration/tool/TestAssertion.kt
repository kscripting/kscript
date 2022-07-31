package kscript.integration.tool

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kscript.app.util.ProcessResult
import kscript.integration.tool.TestContext.runProcess

object TestAssertion {
    fun any() = AnyMatch()
    fun startsWith(string: String, ignoreCase: Boolean = false) = StartsWith(string, ignoreCase)
    fun contains(string: String, ignoreCase: Boolean = false) = Contains(string, ignoreCase)

    fun verify(
        command: String, exitCode: Int = 0, stdOutMatcher: TestMatcher, stdErr: String = ""
    ): ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(stdOutMatcher.matches(processResult.stdout)).isTrue()
        assertThat(processResult.stderr).isEqualTo(stdErr.replace("\n", TestContext.nl))
        return processResult
    }

    fun verify(
        command: String, exitCode: Int = 0, stdOut: String, stdErrMatcher: TestMatcher
    ): ProcessResult {
        val processResult = runProcess(command)

        assertThat(processResult.exitCode).isEqualTo(exitCode)
        assertThat(processResult.stdout).isEqualTo(stdOut.replace("\n", TestContext.nl))
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
        assertThat(processResult.stdout).isEqualTo(stdOut.replace("\n", TestContext.nl))
        assertThat(processResult.stderr).isEqualTo(stdErr.replace("\n", TestContext.nl))
        return processResult
    }
}
