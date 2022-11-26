package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.startsWith
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.projectDir
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

class ResolverTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `It should run kscript and resolve dependencies`() {
        // The first time artifact resolution is started because the cache is cleaned...
        verify(
            "kscript ${resolvePath("$projectDir/test/resources/depends_on_annot.kts")}",
            0,
            "kscript with annotations rocks!\n",
            startsWith("[kscript] Resolving log4j:log4j:1.2.14")
        )


        // clear .m2 cache
        val log4jCached = File(System.getProperty("user.home"), ".m2/repository/log4j/log4j/1.2.14/")

        if (log4jCached.isDirectory) {
            System.err.println("Cleaning up cached copy of log4j: ${log4jCached.absolutePath}")
            log4jCached.deleteRecursively()
        }

        // The second time it is because of removing artifact from cache...
        verify(
            "kscript ${resolvePath("$projectDir/test/resources/depends_on_annot.kts")}",
            0,
            "kscript with annotations rocks!\n",
            startsWith("[kscript] Resolving log4j:log4j:1.2.14")
        )
    }
}
