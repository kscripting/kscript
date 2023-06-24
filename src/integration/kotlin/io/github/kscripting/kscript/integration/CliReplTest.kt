package io.github.kscripting.kscript.integration

import io.github.kscripting.shell.integration.tools.TestAssertion.startsWith
import io.github.kscripting.shell.integration.tools.TestAssertion.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class CliReplTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Do not run interactive mode prep without script argument`() {
        verify("kscript -i", 1, "", startsWith("kscript - Enhanced scripting support for Kotlin"))
    }

//    fun `CLI REPL tests`() {
//        ## interactive mode without dependencies
//        #assert "kscript -i 'exitProcess(0)'" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"
//        #assert "echo '' | kscript -i -" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"
//
//
//        ## first version is disabled because support-auto-prefixing kicks in
//        #assert "kscript -i '//DEPS log4j:log4j:1.2.14'" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"
//        #assert "kscript -i <(echo '//DEPS log4j:log4j:1.2.14')" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"
//    }
}
