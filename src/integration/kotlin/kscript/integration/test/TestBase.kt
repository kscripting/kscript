package kscript.integration.test

import kscript.integration.tool.TestContext
import org.junit.jupiter.api.BeforeAll

interface TestBase {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            TestContext.clearCache()
            TestContext.printKscriptPath()
        }
    }
}
