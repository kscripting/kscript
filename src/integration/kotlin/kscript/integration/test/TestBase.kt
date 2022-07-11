package kscript.integration.test

import kscript.integration.Tools
import org.junit.jupiter.api.BeforeAll

interface TestBase {
    val projectDir: String get() = Companion.projectDir
    val kscript: String get() = Companion.kscript

    companion object {
        private val projectDir = Tools.resolveProjectDir()
        private val kscript = Tools.resolveKscript()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            Tools.runProcess("$kscript --clear-cache")
        }
    }
}
