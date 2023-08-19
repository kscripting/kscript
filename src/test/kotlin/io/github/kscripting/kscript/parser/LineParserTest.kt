package io.github.kscripting.kscript.parser

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isFailure
import assertk.assertions.messageContains
import io.github.kscripting.kscript.model.*
import io.github.kscripting.kscript.parser.LineParser.parseDependency
import io.github.kscripting.kscript.parser.LineParser.parseEntry
import io.github.kscripting.kscript.parser.LineParser.parseImport
import io.github.kscripting.kscript.parser.LineParser.parseKotlinOpts
import io.github.kscripting.kscript.parser.LineParser.parseRepository
import io.github.kscripting.kscript.model.ScriptLocation
import io.github.kscripting.kscript.model.ScriptSource
import io.github.kscripting.kscript.model.ScriptType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.util.stream.Stream

class LineParserTest {
    @Test
    fun `Import parsing`() {
        assertThat(
            parseImport(
                scriptLocation, 1, "import com.script.test1"
            )
        ).containsExactlyInAnyOrder(ImportName("com.script.test1"))

        assertThat(
            parseImport(
                scriptLocation, 1, "      import com.script.test2            "
            )
        ).containsExactlyInAnyOrder(
            ImportName("com.script.test2")
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
            var expectedAnnotations: List<ScriptAnnotation> = list.map { Dependency(it.trim()) }

            if (line.trimStart().startsWith("//")) {
                val expectedList = list.joinToString(", ") { "\"${it.trim()}\"" }
                expectedAnnotations = expectedAnnotations + DeprecatedItem(
                    scriptLocation,
                    1,
                    "Deprecated annotation:\n$line\nshould be replaced with:\n@file:DependsOn($expectedList)"
                )
            }

            assertThat(
                parseDependency(
                    scriptLocation, 1, line
                )
            ).containsExactlyInAnyOrder(*expectedAnnotations.toTypedArray())
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
            assertThat(parseDependency(scriptLocation, 1, line)).containsExactlyInAnyOrder(*list.map { Dependency(it) }
                .toTypedArray())
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
            assertThat { parseDependency(scriptLocation, 1, line) }.isFailure().messageContains(message)
        }
    }

    @Test
    fun `Dependency parsing - invalid quoting`() {
        assertThat {
            parseDependency(
                scriptLocation,
                1,
                "@file:DependsOn(\"com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0\") //Comment"
            )
        }.isFailure()
            .messageContains("Artifact locators must be provided as separate annotation arguments and not as comma-separated list")
    }

    @ParameterizedTest
    @MethodSource("repositories")
    fun `Repository parsing`(line: String, annotations: List<ScriptAnnotation>) {
        println("Repository: '$line'")
        assertThat(parseRepository(scriptLocation, 1, line)).containsExactlyInAnyOrder(*annotations.toTypedArray())
    }

    @ParameterizedTest
    @MethodSource("kotlinOptions")
    fun `Kotlin options parsing`(line: String, kotlinOpts: List<ScriptAnnotation>) {
        println("KotlinOptions: '$line'")
        assertThat(parseKotlinOpts(scriptLocation, 1, line)).containsExactlyInAnyOrder(*kotlinOpts.toTypedArray())
    }

    @ParameterizedTest
    @MethodSource("entryPoint")
    fun `Entry point parsing`(line: String, entry: String) {
        println("Entry point: '$line'")

        var expectedAnnotations: List<ScriptAnnotation> = listOf(Entry(entry))

        if (line.trimStart().startsWith("//")) {
            expectedAnnotations = expectedAnnotations + DeprecatedItem(
                scriptLocation,
                1,
                "Deprecated annotation:\n$line\nshould be replaced with:\n@file:EntryPoint(\"$entry\")"
            )
        }

        assertThat(parseEntry(scriptLocation, 1, line)).containsExactlyInAnyOrder(*expectedAnnotations.toTypedArray())
    }

    companion object {
        @JvmStatic
        private val scriptLocation = ScriptLocation(
            0, ScriptSource.HTTP, ScriptType.KT, URI("http://example/test.kt"), URI("http://example/"), "test"
        )

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
        fun createDeprecatedAnnotation(line: String, expectedContent: String): DeprecatedItem {
            return DeprecatedItem(
                scriptLocation,
                1,
                "Deprecated annotation:\n$line\nshould be replaced with:\n@file:Repository($expectedContent)"
            )
        }

        @JvmStatic
        fun repositories(): Stream<Arguments> {
            val lines = listOf(
                """@file:MavenRepository("imagej-releases", "$repositoryReleasesUrl" ) // crazy comment""",
                """@file:MavenRepository("imagej-releases", "$repositoryReleasesUrl", user="user", password="pass") """,
                """@file:MavenRepository("imagej-snapshots", "$repositorySnapshotsUrl", password="pass", user="user") """,
                // Whitespaces around credentials see #228
                """@file:MavenRepository("spaceAroundCredentials", "$repositorySnapshotsUrl", password= "pass" , user ="user" ) """,
                // Different whitespaces around credentials see #228
                """@file:MavenRepository("spaceAroundCredentials2", "$repositorySnapshotsUrl", password= "pass", user="user" ) """,
                // Different whitespaces around credentials see #228
                """@file:MavenRepository("unnamedCredentials", "$repositorySnapshotsUrl", "user", "pass") """,
                // Named repo options
                """@file:MavenRepository(id= "imagej-releases", url = "$repositoryReleasesUrl", user="user", password="pass") """,
                """@file:Repository("$repositoryReleasesUrl")""",
                """@file:Repository("$repositoryReleasesUrl", user="user", password="pass")""",
            )

            return Stream.of(
                Arguments.of(
                    lines[0], listOf(
                        Repository("imagej-releases", repositoryReleasesUrl),
                        createDeprecatedAnnotation(lines[0], "\"$repositoryReleasesUrl\"")
                    )
                ),
                Arguments.of(
                    lines[1], listOf(
                        Repository("imagej-releases", repositoryReleasesUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[1], "\"$repositoryReleasesUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),
                Arguments.of(
                    lines[2], listOf(
                        Repository("imagej-snapshots", repositorySnapshotsUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[2], "\"$repositorySnapshotsUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),
                Arguments.of(
                    lines[3], listOf(
                        Repository("spaceAroundCredentials", repositorySnapshotsUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[3], "\"$repositorySnapshotsUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),
                Arguments.of(
                    lines[4], listOf(
                        Repository("spaceAroundCredentials2", repositorySnapshotsUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[4], "\"$repositorySnapshotsUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),
                Arguments.of(
                    lines[5], listOf(
                        Repository("unnamedCredentials", repositorySnapshotsUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[5], "\"$repositorySnapshotsUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),
                Arguments.of(
                    lines[6], listOf(
                        Repository("imagej-releases", repositoryReleasesUrl, "user", "pass"),
                        createDeprecatedAnnotation(
                            lines[6], "\"$repositoryReleasesUrl\", user=\"user\", password=\"pass\""
                        )
                    )
                ),

                Arguments.of(
                    lines[7], listOf(Repository("", repositoryReleasesUrl, "", ""))
                ),
                Arguments.of(
                    lines[8], listOf(Repository("", repositoryReleasesUrl, "user", "pass"))
                ),
            )
        }

        @JvmStatic
        fun kotlinOptions(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "//KOTLIN_OPTS -foo, 3 ,'some file.txt'", listOf(
                    KotlinOpt("-foo"), KotlinOpt("3"), KotlinOpt("'some file.txt'"), DeprecatedItem(
                        ScriptLocation(
                            0,
                            ScriptSource.HTTP,
                            ScriptType.KT,
                            URI.create("http://example/test.kt"),
                            URI.create("http://example/"),
                            "test"
                        ),
                        line = 1,
                        message = "Deprecated annotation:\n//KOTLIN_OPTS -foo, 3 ,'some file.txt'\nshould be replaced with:\n@file:KotlinOptions(\"-foo\", \"3\", \"'some file.txt'\")"
                    )
                )
            ),
            Arguments.of(
                """@file:KotlinOpts("--bar") // some other crazy comment""", listOf(
                    KotlinOpt("--bar"), DeprecatedItem(
                        ScriptLocation(
                            0,
                            ScriptSource.HTTP,
                            ScriptType.KT,
                            URI.create("http://example/test.kt"),
                            URI.create("http://example/"),
                            "test"
                        ),
                        line = 1,
                        message = "Deprecated annotation:\n@file:KotlinOpts(\"--bar\") // some other crazy comment\nshould be replaced with:\n@file:KotlinOptions(\"--bar\")"
                    )
                )
            ),
            Arguments.of(
                """@file:KotlinOptions("--bar") // some other crazy comment""", listOf(KotlinOpt("--bar"))
            ),
        )

        @JvmStatic
        fun entryPoint(): Stream<Arguments> = Stream.of(
            Arguments.of("//ENTRY Foo", "Foo"),
            Arguments.of("@file:EntryPoint(\"Foo\")", "Foo"),
        )
    }
}
