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
    fun parseAnnotDependencies() {
        val lines = listOf("""@file:DependsOn("something:dev-1.1.0-alpha3(T2):1.2.14", "de.mpicbg.scicomp:kutils:0.7")""")

        val expected = listOf(
            "something:dev-1.1.0-alpha3(T2):1.2.14",
            "de.mpicbg.scicomp:kutils:0.7",
            "com.github.holgerbrandl:kscript-annotations:1.2"
        )

        Script(lines).collectDependencies() shouldBe expected

        // but reject comma separation within dependency entries
        // note: disabled because quits kscript by design
        //        shouldThrow<IllegalArgumentException> {
        //            extractDependencies("""@file:DependsOn("com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0")""")
        //        }
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
            "com.github.holgerbrandl:kscript-annotations:1.2"
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
                "com.github.holgerbrandl:kscript-annotations:1.2"
            )
        }

    }


    // combine kotlin opts spread over multiple lines
    @Test
    fun optsCollect() {
        val lines = listOf(
            "//KOTLIN_OPTS -foo 3 'some file.txt'",
            "//KOTLIN_OPTS  --bar"
        )

        Script(lines).collectRuntimeOptions() shouldBe "-foo 3 'some file.txt' --bar"
    }

    @Test
    fun annotOptsCollect() {
        val lines = listOf(
            "//KOTLIN_OPTS -foo 3 'some file.txt'",
            """@file:KotlinOpts("--bar")"""
        )

        Script(lines).collectRuntimeOptions() shouldBe "-foo 3 'some file.txt' --bar"
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

    @Test
    fun test_include_detection() {
        val script = Script(File("test/resources/includes/include_variations.kts"))
        script.includes.map { it.file.name } shouldBe List(4) { "include_${it+1}.kt" }
    }
}