package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.any
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.nl
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SupportApiTest : TestBase {
    @Test
    @Tag("posix")
    fun `Make sure that one-liners include support-api`() {
        verify("""echo "foo${nl}bar" | kscript -t "stdin.print()"""", 0, "foo\nbar\n", any())
        verify("""echo "foo${nl}bar" | kscript -t "lines.print()"""", 0, "foo\nbar\n", any())
        verify("""echo 'foo${nl}bar' | kscript -t 'lines.print()'""", 0, "foo\nbar\n", any())
        verify(
            """echo 'foo${nl}bar' | kscript -s --text 'lines.split().select(1,2,-3)'""",
            1,
            "",
            "[ERROR] Can not mix positive and negative selections\n"
        )
    }
}
