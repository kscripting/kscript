package io.github.kscripting.kscript.resolver

import org.junit.jupiter.api.Test
import java.io.File

class IntegrationTests {

    @Test
    fun `it should run kscript and resolve dependencies`() {
        // clear .m2 cache
        val log4jCached = File(System.getProperty("user.home"), ".m2/repository/log4j/log4j/1.2.14/")
        if (log4jCached.isDirectory) {
            System.err.println("Cleaning up cached .m2 copy of log4j")
            log4jCached.deleteRecursively()
        }

        //clear kscript cache
        //kscript.app.main(arrayOf("linux-gnu", "--clear-cache"))

        // run as when being on commandline
//        val observed = captureOutput {
        //kscript.app.main(arrayOf("test/resources/depends_on_annot.kts"))
//        }.stdout.replace(System.getProperty("user.home")!!, "")

//        Assertions.assertEquals("kotlin  -classpath /.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar:/.m2/repository/com/offbytwo/docopt/0.6.0.20150202/docopt-0.6.0.20150202.jar:/.kscript/cache/jar_7cb43f5abd032b31680d6649e4ef6a87/scriplet.jar:/.sdkman/candidates/kotlin/1.5.31/lib/kotlin-script-runtime.jar Main_Depends_on_annot", observed)
        // todo complete the assertion to fix the test
    }
}
