package io.github.kscripting.kscript.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import org.junit.jupiter.api.Test

class ScriptUtilsTest {
    @Test
    fun `Test resolve repository option literal`() {
        val option = ScriptUtils.resolveRepositoryOption(
            "literal-value",
            "optionName",
            "{{PLACEHOLDER}}",
            "placeholder-value",
            emptyMap(),
        )

        assertThat(option).isEqualTo("literal-value")
    }

    @Test
    fun `Test resolve repository option placeholder`() {
        val option = ScriptUtils.resolveRepositoryOption(
            "{{FOO}}",
            "optionName",
            "{{FOO}}",
            "placeholder-value",
            emptyMap(),
        )

        assertThat(option).isEqualTo("placeholder-value")
    }

    @Test
    fun `Test resolve repository option placeholder with prefix and suffix`() {
        assertThat(
            ScriptUtils.resolveRepositoryOption(
                "{{FOO}}-bar",
                "optionName",
                "{{FOO}}",
                "placeholder-value",
                emptyMap(),
            )
        ).isEqualTo("placeholder-value-bar")

        assertThat(
            ScriptUtils.resolveRepositoryOption(
                "bar-{{FOO}}",
                "optionName",
                "{{FOO}}",
                "placeholder-value",
                emptyMap(),
            )
        ).isEqualTo("bar-placeholder-value")
    }

    @Test
    fun `Test resolve repository option environment`() {
        val option = ScriptUtils.resolveRepositoryOption(
            "\$FOO",
            "optionName",
            "{{FOO}}",
            "placeholder-value",
            mapOf(
                "FOO" to "env-value",
            ),
        )

        assertThat(option).isEqualTo("env-value")
    }

    @Test
    fun `Test resolve repository option environment with placeholder`() {
        assertThat(
            ScriptUtils.resolveRepositoryOption(
                "\$FOO",
                "optionName",
                "{{BAR}}",
                "placeholder-value",
                mapOf(
                    "FOO" to "env-value-{{BAR}}",
                ),
            )
        ).isEqualTo("env-value-placeholder-value")
    }

    @Test
    fun `Test repository option resolution failure, no environment value`() {
        assertThat {
            ScriptUtils.resolveRepositoryOption(
                "\$FOO",
                "optionName",
                "{{BAR}}",
                "placeholder-value",
                emptyMap(),
            )
        }.isFailure()
    }
}
