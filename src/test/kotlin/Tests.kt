import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import kscript.app.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.function.Consumer

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

        result.scriptFile.readText() shouldBe (expected.readText())
    }


    @Test
    fun test_include_annotations() {
        val file = File("test/resources/includes/include_variations.kts")
        val expected = File("test/resources/includes/expexcted_variations.kts")

        val result = resolveIncludes(file)

        result.scriptFile.readText() shouldBe (expected.readText())
    }

    @Test
    fun test_include_detection() {
        val result = resolveIncludes(File("test/resources/includes/include_variations.kts"))

        result.includes.filter { it.protocol == "file" }.map { File(it.toURI()).name } shouldBe List(4) { "include_${it + 1}.kt" }
        result.includes.filter { it.protocol != "file" }.size shouldBe 1
    }

    @Test
    fun `test include detection - should not include dependency twice`() {
      val result = resolveIncludes(File("test/resources/includes/dup_include/dup_include.kts"))

      result.includes.map { File(it.toURI()).name } shouldBe listOf(
            "dup_include_1.kt",
            "dup_include_2.kt"
        )
    }

    @Test
    fun `should run process in Windows shell`() {
        // run only in windows
        assumeTrue(isWindows())

        val sout = StringBuilder()
        val runProcess = runProcess(
                cmd = *arrayOf("cmd", "/C", "dir", "."),
                wd = File(".").absoluteFile,
                stdoutConsumer = Consumer {
                    sout.append("$it\n")
                })

        runProcess.exitCode shouldBe 0
        sout.split("\n").size should beGreaterThan(1)
    }

    @Test
    fun `should resolve if command is in path`() {
        val cmd = if (isWindows()) "cmd" else "ls"
        val isInPath = ShellUtils.isInPath(cmd)

        isInPath shouldBe true
    }

    @Test
    fun `should resolve dependencies`() {
        val resolvedDependencies = resolveDependencies(depIds = listOf("junit:junit:4.12"), customRepos = emptyList(), loggingEnabled = true)

        resolvedDependencies!!.split(File.pathSeparator).size shouldBe 2
    }

    @Test
    fun `should run a script with only println`() {
        main(arrayOf("test/resources/dot.Test.kts"))
    }

}
