package kscript.integration.tools

import kscript.app.util.ProcessResult
import kscript.integration.tools.TestContext.runProcess

object TestAssertion {
    fun <T : Any> geq(value: T) = GenericEquals(value)

    fun any() = AnyMatch()
    fun eq(string: String, ignoreCase: Boolean = false) = Equals(string, ignoreCase)
    fun startsWith(string: String, ignoreCase: Boolean = false) = StartsWith(string, ignoreCase)
    fun contains(string: String, ignoreCase: Boolean = false) = Contains(string, ignoreCase)

    fun verify(command: String, exitCode: Int = 0, stdOut: TestMatcher<String>, stdErr: String = ""): ProcessResult =
        verify(command, exitCode, stdOut, eq(stdErr))

    fun verify(command: String, exitCode: Int = 0, stdOut: String, stdErr: TestMatcher<String>): ProcessResult =
        verify(command, exitCode, eq(stdOut), stdErr)

    fun verify(command: String, exitCode: Int = 0, stdOut: String = "", stdErr: String = ""): ProcessResult =
        verify(command, exitCode, eq(stdOut), eq(stdErr))

    fun verify(
        command: String, exitCode: Int = 0, stdOut: TestMatcher<String>, stdErr: TestMatcher<String>
    ): ProcessResult {
        val processResult = runProcess(command)
        val extCde = geq(exitCode)

        extCde.checkAssertion("ExitCode", processResult.exitCode)
        stdOut.checkAssertion("StdOut", processResult.stdout)
        stdErr.checkAssertion("StdErr", processResult.stderr)
        println()

        return processResult
    }
}
