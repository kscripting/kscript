package io.github.kscripting.kscript.integration.tools

import io.github.kscripting.kscript.integration.tools.TestContext.runProcess
import io.github.kscripting.shell.model.GobbledProcessResult

object TestAssertion {
    fun <T : Any> geq(value: T) = GenericEquals(value)

    fun any() = AnyMatch()
    fun eq(string: String, ignoreCase: Boolean = false) = Equals(string, ignoreCase)
    fun startsWith(string: String, ignoreCase: Boolean = false) = StartsWith(string, ignoreCase)
    fun contains(string: String, ignoreCase: Boolean = false) = Contains(string, ignoreCase)

    fun verify(
        command: String,
        exitCode: Int = 0,
        stdOut: TestMatcher<String>,
        stdErr: String = ""
    ): GobbledProcessResult =
        verify(command, exitCode, stdOut, eq(stdErr))

    fun verify(command: String, exitCode: Int = 0, stdOut: String, stdErr: TestMatcher<String>): GobbledProcessResult =
        verify(command, exitCode, eq(stdOut), stdErr)

    fun verify(command: String, exitCode: Int = 0, stdOut: String = "", stdErr: String = ""): GobbledProcessResult =
        verify(command, exitCode, eq(stdOut), eq(stdErr))

    fun verify(
        command: String, exitCode: Int = 0, stdOut: TestMatcher<String>, stdErr: TestMatcher<String>
    ): GobbledProcessResult {
        val processResult = runProcess(command)
        val extCde = geq(exitCode)

        extCde.checkAssertion("ExitCode", processResult.exitCode)
        stdOut.checkAssertion("StdOut", processResult.stdout)
        stdErr.checkAssertion("StdErr", processResult.stderr)
        println()

        return processResult
    }
}
