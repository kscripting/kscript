package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestContext
import io.github.kscripting.kscript.util.ShellUtils
import io.github.kscripting.shell.ShellExecutor
import org.junit.jupiter.api.BeforeAll

interface TestBase {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            clearCache()
            printPaths()
            println("[nl] - new line; [bs] - backspace")
        }

        //TODO: in kscript I have to call it in runProcess
        //ShellUtils.environmentAdjuster(map)

        private fun printPaths() {
            val kscriptPath = ShellUtils.which(TestContext.osType, "kscript", TestContext::adjustEnv)
            println("kscript path: $kscriptPath")
            val kotlincPath = ShellUtils.which(TestContext.osType, "kotlinc", TestContext::adjustEnv)
            println("kotlinc path: $kotlincPath")
        }

        private fun clearCache() {
            print("Clearing kscript cache... ")
            ShellExecutor.eval("kscript --clear-cache", TestContext.osType, null, TestContext::adjustEnv)
            println("done.")
        }
    }
}
