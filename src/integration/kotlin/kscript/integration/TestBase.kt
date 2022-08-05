package kscript.integration

import kscript.integration.tools.TestContext
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
