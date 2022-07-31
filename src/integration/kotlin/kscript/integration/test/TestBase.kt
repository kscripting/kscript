package kscript.integration.test

import kscript.integration.tool.TestContext
import org.junit.jupiter.api.BeforeAll

interface TestBase {
    val kscript: String get() = TestContext.resolveKscript()
    val projectDir: String get() = TestContext.resolveProjectDir()
    val testDir: String get() = "$projectDir/build/tmp/test"

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUp() {
            TestContext.clearCache()
            TestContext.printKscriptPath()
        }
    }
}
