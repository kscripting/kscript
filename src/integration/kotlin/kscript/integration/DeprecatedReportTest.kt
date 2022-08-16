package kscript.integration

import kscript.integration.tools.TestAssertion.contains
import kscript.integration.tools.TestAssertion.startsWith
import kscript.integration.tools.TestAssertion.verify
import kscript.integration.tools.TestContext.projectDir
import kscript.integration.tools.TestContext.resolvePath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class DeprecatedReportTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that for deprecated features warn is generated`() {
        verify(
            "kscript ${resolvePath("$projectDir/test/resources/deprecated_report.kt")}",
            0,
            "made it!\n",
            startsWith("[kscript] [WARN] There are deprecated features in scripts. Use --report option to print full report.")
        )
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Assert that report with deprecated features is generated`() {
        verify(
            "kscript --report ${resolvePath("$projectDir/test/resources/deprecated_report.kt")}",
            0,
            "made it!\n",
            contains("@file:DependsOn(\"org.apache.commons:commons-lang3:3.12.0\")")
        )
    }
}
