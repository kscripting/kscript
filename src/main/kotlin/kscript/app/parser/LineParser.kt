package kscript.app.parser

import kscript.app.model.*
import kscript.app.model.Deprecated

@Suppress("UNUSED_PARAMETER")
object LineParser {
    private val annotationStartingWithCommentError = "Annotation starting with comment are deprecated:"
    private val sheBang = listOf(SheBang)

    fun parseSheBang(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        if (text.startsWith("#!/")) {
            return sheBang
        }
        return emptyList()
    }

    fun parseInclude(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        text.trim().let {
            return when {
                it.startsWith(fileInclude) -> listOf(
                    Include(extractQuotedValueInParenthesis(it.substring(fileInclude.length)))
                )

                it.startsWith(include) -> listOf(
                    Include(extractValue(it.substring(include.length))), Deprecated(
                        location, line, "$annotationStartingWithCommentError\n$text"
                    )
                )

                else -> emptyList()
            }
        }
    }

    private fun validateDependency(dependency: String): String {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        regex.find(dependency) ?: throw ParseException(
            "Invalid dependency locator: '${dependency}'. Expected format is groupId:artifactId:version[:classifier][@type]"
        )
        return dependency
    }

    fun parseDependency(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        text.trim().let { s ->
            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> extractQuotedValuesInParenthesis(s.substring(fileDependsOnMaven.length))
                s.startsWith(fileDependsOn) -> extractQuotedValuesInParenthesis(s.substring(fileDependsOn.length))
                s.startsWith(depends) -> extractValues(s.substring(depends.length))
                else -> emptyList()
            }

            return dependencies.map {
                val validated = validateDependency(it)
                Dependency(validated)
            }
        }
    }

    fun parseEntry(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val fileEntry = "@file:EntryPoint"
        val entry = "//ENTRY "

        text.trim().let {
            return when {
                it.startsWith(fileEntry) -> listOf(
                    Entry(extractQuotedValueInParenthesis(it.substring(fileEntry.length)))
                )

                it.startsWith(entry) -> listOf(Entry(extractValue(it.substring(entry.length))))
                else -> emptyList()
            }
        }
    }

    fun parseRepository(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        //Format:
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/", user="user", password="pass")

        val fileMavenRepository = "@file:MavenRepository"

        text.trim().let {
            return when {
                it.startsWith(fileMavenRepository) -> {
                    val value = it.substring(fileMavenRepository.length).substringBeforeLast(")")

                    val repository = value.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                        val keyValSep = "[ ]*=[ ]*\"".toRegex()

                        val namedArgs = annotationParams.filter { it.contains(keyValSep) }.map { keyVal ->
                            keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let { it.first() to it.last() }
                        }.toMap()

                        if (annotationParams.size < 2) {
                            throw ParseException(
                                "Missing ${2 - annotationParams.size} of the required arguments for @file:MavenRepository(id, url)"
                            )
                        }

                        Repository(
                            namedArgs.getOrDefault("id", annotationParams[0]),
                            namedArgs.getOrDefault("url", annotationParams[1]),
                            namedArgs.getOrDefault("user", annotationParams.getOrNull(2) ?: ""),
                            namedArgs.getOrDefault("password", annotationParams.getOrNull(3) ?: "")
                        )
                    }
                    return listOf(repository)
                }

                else -> emptyList()
            }
        }
    }

    fun parseKotlinOpts(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        text.trim().let {
            return when {
                it.startsWith(fileKotlinOpts) -> extractQuotedValuesInParenthesis(it.substring(fileKotlinOpts.length)).map {
                    KotlinOpt(it)
                }

                it.startsWith(kotlinOpts) -> extractValues(it.substring(kotlinOpts.length)).map { KotlinOpt(it) }
                else -> emptyList()
            }
        }
    }

    fun parseCompilerOpts(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        text.trim().let {
            return when {
                it.startsWith(fileCompilerOpts) -> extractQuotedValuesInParenthesis(it.substring(fileCompilerOpts.length)).map {
                    CompilerOpt(it)
                }

                it.startsWith(compilerOpts) -> extractValues(it.substring(compilerOpts.length)).map {
                    CompilerOpt(it)
                }

                else -> emptyList()
            }
        }
    }

    fun parsePackage(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val packagePrefix = "package "

        text.trim().let {
            if (it.startsWith(packagePrefix)) {
                return listOf(PackageName(it.substring(packagePrefix.length)))
            }
            return emptyList()
        }
    }

    fun parseImport(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        val importPrefix = "import "

        text.trim().let {
            if (it.startsWith(importPrefix)) {
                return listOf(ImportName(it.substring(importPrefix.length)))
            }
            return emptyList()
        }
    }

    private fun extractQuotedValueInParenthesis(string: String): String {
        val result = extractQuotedValuesInParenthesis(string)

        if (result.size != 1) {
            throw ParseException("Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    private fun extractQuotedValuesInParenthesis(string: String): List<String> {
        // https://stackoverflow.com/questions/171480/regex-grabbing-values-between-quotation-marks

        if (!string.startsWith("(")) {
            throw ParseException("Missing parenthesis")
        }

        val annotationArgs = """(["'])(\\?.*?)\1""".toRegex().findAll(string.drop(1)).toList().map {
            it.groupValues[2].trim()
        }

        // fail if any argument is a comma separated list of artifacts (see #101)
        annotationArgs.filter { it.contains(",[^)]".toRegex()) }.let {
            if (it.isNotEmpty()) {
                throw ParseException(
                    "Artifact locators must be provided as separate annotation arguments and not as comma-separated list: $it"
                )
            }
        }

        return annotationArgs
    }

    private fun extractValue(string: String): String {
        val result = extractValues(string)

        if (result.size != 1) {
            throw ParseException("Expected single value, but get ${result.size}")
        }

        return result[0]
    }

    fun extractValues(string: String): List<String> {
        string.trim().let {
            return it.split(",(?=(?:[^']*'[^']*')*[^']*\$)".toRegex()).map(String::trim).filter(String::isNotBlank)
        }
    }
}
