package kscript.integration.test

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class EnvironmentTest : TestBase {
    @Test
    @Tag("posix")
    fun `Do not run interactive mode prep without script argument`() {
        verify("$kscript -i", 1, "", startsWith("kscript - Enhanced scripting support for Kotlin"))
    }

    @Test
    @Tag("posix")
    fun `Make sure that KOTLIN_HOME can be guessed from kotlinc correctly`() {
        verify("unset KOTLIN_HOME; echo 'println(99)' | $kscript -", 0, "99\n")
    }

    //TODO: test what happens if kotlin/kotlinc/java/gradle/idea is not in PATH

    @Test
    @Tag("posix")
    fun `Run script that tries to find out its own filename via environment variable`() {
        val path = "$projectDir/test/resources/uses_self_file_name.kts"
        verify(path, 0, "Usage: $path [-ae] [--foo] file+\n")
    }
}
