import io.kotlintest.matchers.shouldBe
import kscript.app.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * @author Holger Brandl
 */

class Tests {

    // "comma separated dependencies should be parsed correctly"
    @Test
    fun directiveDependencyCollect() {
        val lines = listOf(
            "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
            "//DEPS  log4j:log4j:1.2.14"
        )

        val expected = listOf(
            "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
            "de.mpicbg.scicomp:kutils:0.7",
            "log4j:log4j:1.2.14"
        )

        Script(lines).collectDependencies() shouldBe expected
    }

    @Test
    fun mixedDependencyCollect() {
        val lines = listOf(
            "//DEPS de.mpicbg.scicomp.joblist:joblist-kotlin:1.1, de.mpicbg.scicomp:kutils:0.7",
            """@file:DependsOn("log4j:log4j:1.2.14")"""
        )

        val expected = listOf(
            "de.mpicbg.scicomp.joblist:joblist-kotlin:1.1",
            "de.mpicbg.scicomp:kutils:0.7",
            "log4j:log4j:1.2.14",
            "com.github.holgerbrandl:kscript-annotations:1.1"
        )

        Script(lines).collectDependencies() shouldBe expected
    }


    @Test
    fun customRepo() {
        val lines = listOf(
            """@file:MavenRepository("imagej-releases", "http://maven.imagej.net/content/repositories/releases" ) // crazy comment""",
            """@file:DependsOnMaven("net.clearvolume:cleargl:2.0.1")""",
            """@file:DependsOn("log4j:log4j:1.2.14")""",
            """println("foo")"""
        )

        with(Script(lines)) {

            collectRepos() shouldBe listOf(
                MavenRepo("imagej-releases", "http://maven.imagej.net/content/repositories/releases")
            )

            collectDependencies() shouldBe listOf(
                "net.clearvolume:cleargl:2.0.1",
                "log4j:log4j:1.2.14",
                "com.github.holgerbrandl:kscript-annotations:1.1"
            )
        }

    }

    @Test
    fun detectEntryPoint() {
        assertTrue(isEntryPointDirective("//ENTRY Foo"))
        assertTrue(isEntryPointDirective("""@file:EntryPoint("Foo")"""))

        assertFalse(isEntryPointDirective("""//@file:EntryPoint("Foo")"""))
        assertFalse(isEntryPointDirective("""// //ENTRY Foo"""))


        val commentDriven = """
            // comment
            //ENTRY Foo
            fun a = ""
            """.trimIndent()

        Script(commentDriven.lines()).findEntryPoint() shouldBe "Foo"


        val annotDriven = """
            // comment
            @file:EntryPoint("Foo")
            fun a = ""
            """.trimIndent()

        Script(annotDriven.lines()).findEntryPoint() shouldBe "Foo"
    }


    @Test
    fun test_consolidate_imports() {
        val file = File("test/resources/consolidate_includes/template.kts")
        val expected = File("test/resources/consolidate_includes/expected.kts")

        val result = resolveIncludes(file)

        result.readText() shouldBe (expected.readText())
    }


    @Test
    fun test_include_annotations() {
        val file = File("test/resources/includes/include_variations.kts")
        val expected = File("test/resources/includes/expexcted_variations.kts")

        val result = resolveIncludes(file)

        result.readText() shouldBe (expected.readText())
    }
}