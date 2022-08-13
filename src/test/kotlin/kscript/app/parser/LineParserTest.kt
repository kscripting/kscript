package kscript.app.parser

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isFailure
import assertk.assertions.messageContains
import kscript.app.model.*
import kscript.app.parser.LineParser.parseDependency
import kscript.app.parser.LineParser.parseEntry
import kscript.app.parser.LineParser.parseImport
import kscript.app.parser.LineParser.parseKotlinOpts
import kscript.app.parser.LineParser.parseRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.util.stream.Stream

class LineParserTest {
    private val location = Location(0, ScriptSource.HTTP, ScriptType.KT, URI("http://example/test.kt"), URI("http://example/"), "test")
    @Test
    fun `Import parsing`() {
        assertThat(parseImport(location, 1, "import com.script.test1")).containsExactlyInAnyOrder(ImportName("com.script.test1"))
        assertThat(parseImport(location, 1, "      import com.script.test2            ")).containsExactlyInAnyOrder(ImportName("com.script.test2"))
    }

    @Test
    fun `Repository parsing`() {
        assertThat(parseRepository(location, 1, "@file:MavenRepository(\"imagej-releases\",\"http://maven.imagej.net/content/repositories/releases\" )")).containsExactlyInAnyOrder(
            Repository("imagej-releases", "http://maven.imagej.net/content/repositories/releases")
        )
    }

    @ParameterizedTest
    @MethodSource("staticDependencies")
    fun `Dependency parsing - static dependencies`(list: List<String>) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }
        val listWithoutQuotes = list.joinToString(", ")
        val listWithoutQuotesStrangelyFormatted = list.joinToString("   ,    ")

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
            "//DEPS $listWithoutQuotes",
            "    //DEPS $listWithoutQuotesStrangelyFormatted",
        )) {
            println("Case: '$line'")
            assertThat(parseDependency(location, 1, line)).containsExactlyInAnyOrder(*list.map { Dependency(it.trim()) }
                .toTypedArray())
        }
    }

    @ParameterizedTest
    @MethodSource("dynamicDependencies")
    fun `Dependency parsing - dynamic dependencies`(list: List<String>) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
        )) {
            println("Case: '$line'")
            assertThat(parseDependency(location, 1, line)).containsExactlyInAnyOrder(*list.map { Dependency(it) }.toTypedArray())
        }
    }

    @ParameterizedTest
    @MethodSource("invalidDependencies")
    fun `Dependency parsing - invalid dependencies`(list: List<String>, message: String) {
        val listWithQuotes = list.joinToString(", ") { "\"$it\"" }
        val listWithoutQuotes = list.joinToString(", ")

        for (line in listOf(
            "@file:DependsOn($listWithQuotes)",
            "@file:DependsOn($listWithQuotes) //Comment",
            "@file:DependsOnMaven($listWithQuotes)",
            "      @file:DependsOnMaven($listWithQuotes)    ",
            "//DEPS $listWithoutQuotes",
            "    //DEPS $listWithoutQuotes",
        )) {
            println("Case: '$line'")
            assertThat { parseDependency(location, 1, line) }.isFailure().messageContains(message)
        }
    }

    @Test
    fun `Dependency parsing - invalid quoting`() {
        assertThat { parseDependency(location, 1, "@file:DependsOn(\"com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0\") //Comment") }.isFailure()
            .messageContains("Artifact locators must be provided as separate annotation arguments and not as comma-separated list")
    }

    @ParameterizedTest
    @MethodSource("repositories")
    fun `Repository parsing`(line: String, repository: Repository) {
        println("Repository: '$line'")
        assertThat(parseRepository(location, 1, line)).containsExactly(repository)
    }

    @ParameterizedTest
    @MethodSource("kotlinOpts")
    fun `Kotlin options parsing`(line: String, kotlinOpts: List<KotlinOpt>) {
        println("KotlinOpts: '$line'")
        assertThat(parseKotlinOpts(location, 1, line)).containsExactlyInAnyOrder(*kotlinOpts.toTypedArray())
    }

    @ParameterizedTest
    @MethodSource("entryPoint")
    fun `Entry point parsing`(line: String, entry: String) {
        println("Entry point: '$line'")
        assertThat(parseEntry(location, 1, line)).containsExactlyInAnyOrder(Entry(entry))
    }

    companion object {
        @JvmStatic
        fun staticDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("org.javamoney:moneta:pom:1.3")),
            Arguments.of(
                listOf(
                    "  org.javamoney:moneta:pom:1.3  ", "log4j:log4j:1.2.14", "com.offbytwo:docopt:0.6.0.20150202"
                )
            ),
            Arguments.of(listOf("de.mpicbg.scicomp.joblist:joblist-kotlin:1.1", "de.mpicbg.scicomp:kutils:0.7")),
            Arguments.of(listOf("something:dev-1.1.0-alpha3(T2):1.2.14", "de.mpicbg.scicomp:kutils:0.7"))
        )

        @JvmStatic
        fun dynamicDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("log4j:log4j:[1.2,)", "com.offbytwo:docopt:[0.6,)"))
        )

        @JvmStatic
        fun invalidDependencies(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf("log4j:1.0"), "Invalid dependency locator: 'log4j"),
            Arguments.of(listOf("com.offbytwo:docopt:0.6", "log4j:1.0"), "Invalid dependency locator: 'log4j"),
            Arguments.of(
                listOf("log4j:::1.0", "com.offbytwo:docopt:0.6", "log4j:1.0"), "Invalid dependency locator: 'log4j"
            ),
        )

        private const val repositoryReleasesUrl = "http://maven.imagej.net/content/repositories/releases"
        private const val repositorySnapshotsUrl = "http://maven.imagej.net/content/repositories/snapshots"

        @JvmStatic
        fun repositories(): Stream<Arguments> = Stream.of(
            Arguments.of(
                """@file:MavenRepository("imagej-releases", "$repositoryReleasesUrl" ) // crazy comment""",
                Repository("imagej-releases", repositoryReleasesUrl)
            ),
            Arguments.of(
                """@file:MavenRepository("imagej-releases", "$repositoryReleasesUrl", user="user", password="pass") """,
                Repository("imagej-releases", repositoryReleasesUrl, "user", "pass")
            ),
            Arguments.of(
                """@file:MavenRepository("imagej-snapshots", "$repositorySnapshotsUrl", password="pass", user="user") """,
                Repository("imagej-snapshots", repositorySnapshotsUrl, "user", "pass")
            ),
            Arguments.of(
                // Whitespaces around credentials see #228
                """@file:MavenRepository("spaceAroundCredentials", "$repositorySnapshotsUrl", password= "pass" , user ="user" ) """,
                Repository("spaceAroundCredentials", repositorySnapshotsUrl, "user", "pass")
            ),
            Arguments.of(
                // Different whitespaces around credentials see #228
                """@file:MavenRepository("spaceAroundCredentials2", "$repositorySnapshotsUrl", password= "pass", user="user" ) """,
                Repository("spaceAroundCredentials2", repositorySnapshotsUrl, "user", "pass")
            ),
            Arguments.of(
                // Different whitespaces around credentials see #228
                """@file:MavenRepository("unnamedCredentials", "$repositorySnapshotsUrl", "user", "pass") """,
                Repository("unnamedCredentials", repositorySnapshotsUrl, "user", "pass")
            ),
            Arguments.of(
                // Named repo options
                """@file:MavenRepository(id= "imagej-releases", url = "$repositoryReleasesUrl", user="user", password="pass") """,
                Repository("imagej-releases", repositoryReleasesUrl, "user", "pass")
            ),
        )

        @JvmStatic
        fun kotlinOpts(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "//KOTLIN_OPTS -foo, 3 ,'some file.txt'",
                listOf(KotlinOpt("-foo"), KotlinOpt("3"), KotlinOpt("'some file.txt'"))
            ),
            Arguments.of(
                """@file:KotlinOpts("--bar") // some other crazy comment""", listOf(KotlinOpt("--bar"))
            ),
        )

        @JvmStatic
        fun entryPoint(): Stream<Arguments> = Stream.of(
            Arguments.of("//ENTRY Foo", "Foo"),
            Arguments.of("@file:EntryPoint(\"Foo\")", "Foo"),
        )
    }
}
