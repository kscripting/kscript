package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestContext
import io.github.kscripting.kscript.util.ShellUtils
import io.github.kscripting.shell.ShellExecutor
import io.github.kscripting.shell.integration.tools.ShellTestBase
import io.github.kscripting.shell.integration.tools.ShellTestCompanionBase
import io.github.kscripting.shell.process.EnvAdjuster
import org.junit.jupiter.api.BeforeAll

interface TestBase : ShellTestBase {
    companion object : ShellTestCompanionBase() {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            clearCache()
            printPaths()
            println("[nl] - new line; [bs] - backspace")
        }

        override fun commonEnvAdjuster(specificEnvAdjuster: EnvAdjuster): EnvAdjuster {
            return { map ->
                map[TestContext.pathEnvVariableName] = TestContext.pathEnvVariableCalculatedPath
                specificEnvAdjuster(map)
                ShellUtils.environmentAdjuster(map)
            }
        }

        private fun printPaths() {
            val kscriptPath = ShellUtils.which(TestContext.osType, "kscript", commonEnvAdjuster())
            println("kscript path: $kscriptPath")
            val kotlincPath = ShellUtils.which(TestContext.osType, "kotlinc", commonEnvAdjuster())
            println("kotlinc path: $kotlincPath")
        }

        private fun clearCache() {
            print("Clearing kscript cache... ")
            ShellExecutor.eval("kscript --clear-cache", TestContext.osType, null, envAdjuster = commonEnvAdjuster())
            println("done.")
        }
    }
}
