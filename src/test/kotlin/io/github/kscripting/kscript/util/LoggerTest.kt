package io.github.kscripting.kscript.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import io.github.kscripting.kscript.util.Logger.errorMsg
import org.junit.jupiter.api.Test

class LoggerTest {
    private val exception1 = IllegalArgumentException("Test message\nSecond line")
    private val exception2 = IllegalStateException("This is some sophisticated exception message.")

    @Test
    fun `Test error message for exception`() {
        Logger.devMode = false
        Logger.silentMode = false
        assertThat(errorMsg(exception1)).isEqualTo("[kscript] [ERROR] Test message\n[kscript] [ERROR] Second line")
    }

    @Test
    fun `Test stacktrace for exception`() {
        Logger.devMode = true
        Logger.silentMode = false
        assertThat(errorMsg(exception2)).startsWith(
            """ |[kscript] [ERROR] This is some sophisticated exception message.
                |[kscript] [ERROR] 
                |[kscript] [ERROR] java.lang.IllegalStateException: This is some sophisticated exception message.
                |[kscript] [ERROR] 	at io.github.kscripting.kscript.util.LoggerTest.<init>""".trimMargin()
        )
    }
}
