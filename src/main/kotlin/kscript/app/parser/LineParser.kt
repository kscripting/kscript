package kscript.app.parser

import kscript.app.model.*
import kscript.app.model.Deprecated

@Suppress("UNUSED_PARAMETER")
object LineParser {
    private const val deprecatedAnnotation = "Deprecated annotation:"
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

                it.startsWith(include) -> {
                    val value = extractValue(it.substring(include.length))

                    listOf(
                        Include(value), createDeprecatedAnnotation(
                            location, line, deprecatedAnnotation, text, "@file:Include(\"$value\")"
                        )
                    )
                }

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

        val deprecated: MutableList<Deprecated> = mutableListOf()

        text.trim().let { s ->
            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> {
                    extractQuotedValuesInParenthesis(s.substring(fileDependsOnMaven.length))
                }

                s.startsWith(fileDependsOn) -> {
                    extractQuotedValuesInParenthesis(s.substring(fileDependsOn.length))
                }

                s.startsWith(depends) -> {
                    val values = extractValues(s.substring(depends.length))
                    deprecated.add(createDeprecatedAnnotation(location,
                                                              line,
                                                              deprecatedAnnotation,
                                                              text,
                                                              "@file:DependsOn(" + values.joinToString(",") { "\"$it\"" } + ")"))

                    values
                }

                else -> emptyList()
            }

            val dependencyAnnotations = dependencies.map {
                val validated = validateDependency(it)
                Dependency(validated)
            }

            return dependencyAnnotations + deprecated
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

                it.startsWith(entry) -> {
                    val value = extractValue(it.substring(entry.length))
                    listOf(
                        Entry(value), createDeprecatedAnnotation(
                            location, line, deprecatedAnnotation, text, "@file:EntryPoint(\"$value\")"
                        )
                    )
                }

                else -> emptyList()
            }
        }
    }

    fun parseRepository(location: Location, line: Int, text: String): List<ScriptAnnotation> {
        //Format:
        // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
        // @file:Repository("http://maven.imagej.net/content/repositories/releases/", user="user", password="pass")

        val fileMavenRepository = "@file:MavenRepository"
        val fileRepository = "@file:Repository"

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

                    var str = """"${repository.url}""""

                    if (repository.user.isNotBlank()) {
                        str += """, "${repository.user}""""
                    }

                    if (repository.password.isNotBlank()) {
                        str += """, "${repository.password}""""
                    }

                    return listOf(
                        repository, createDeprecatedAnnotation(
                            location, line, deprecatedAnnotation, text, "@file:Repository($str)"
                        )
                    )
                }

                it.startsWith(fileRepository) -> {
                    val value = it.substring(fileRepository.length).substringBeforeLast(")")

                    val repository = value.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                        val keyValSep = "[ ]*=[ ]*\"".toRegex()

                        val namedArgs = annotationParams.filter { it.contains(keyValSep) }.map { keyVal ->
                            keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let { it.first() to it.last() }
                        }.toMap()

                        if (annotationParams.isEmpty()) {
                            throw ParseException("Missing required argument of annotation @file:Repository(url)")
                        }

                        Repository(
                            namedArgs.getOrDefault("id", ""),
                            namedArgs.getOrDefault("url", annotationParams[0]),
                            namedArgs.getOrDefault("user", annotationParams.getOrNull(1) ?: ""),
                            namedArgs.getOrDefault("password", annotationParams.getOrNull(2) ?: "")
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

                it.startsWith(kotlinOpts) -> {
                    val values = extractValues(it.substring(kotlinOpts.length))
                    values.map { KotlinOpt(it) } + createDeprecatedAnnotation(location,
                                                                              line,
                                                                              deprecatedAnnotation,
                                                                              text,
                                                                              values.joinToString { "@file:KotlinOpts(\"$it\")\n" })
                }

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

                it.startsWith(compilerOpts) -> {
                    val values = extractValues(it.substring(compilerOpts.length))
                    values.map { CompilerOpt(it) } + createDeprecatedAnnotation(location,
                                                                                line,
                                                                                deprecatedAnnotation,
                                                                                text,
                                                                                values.joinToString { "@file:CompilerOpts(\"$it\")\n" })
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

    private fun createDeprecatedAnnotation(
        location: Location, line: Int, introText: String, existing: String, replacement: String
    ): Deprecated = Deprecated(location, line, "$introText\n$existing\nshould be replaced with:\n$replacement")
}
