package io.github.kscripting.kscript.parser

import io.github.kscripting.kscript.model.*
import io.github.kscripting.shell.model.ScriptLocation

@Suppress("UNUSED_PARAMETER")
object LineParser {
    private const val deprecatedAnnotation = "Deprecated annotation:"
    private val sheBang = listOf(SheBang)

    fun parseSheBang(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        if (text.startsWith("#!/")) {
            return sheBang
        }
        return emptyList()
    }

    fun parseInclude(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        val fileImport = "@file:Import"
        val fileInclude = "@file:Include"
        val include = "//INCLUDE "

        text.trim().let {
            return when {
                it.startsWith(fileImport) -> {
                    val value = extractQuotedValueInParenthesis(it.substring(fileImport.length))
                    listOf(Include(value))
                }

                it.startsWith(fileInclude) -> {
                    val value = extractQuotedValueInParenthesis(it.substring(fileInclude.length))

                    listOf(
                        Include(value), createDeprecatedAnnotation(
                            scriptLocation, line, deprecatedAnnotation, text, "@file:Import(\"$value\")"
                        )
                    )
                }

                it.startsWith(include) -> {
                    val value = extractValue(it.substring(include.length))

                    listOf(
                        Include(value), createDeprecatedAnnotation(
                            scriptLocation, line, deprecatedAnnotation, text, "@file:Import(\"$value\")"
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

    fun parseDependency(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        val fileDependsOn = "@file:DependsOn"
        val fileDependsOnMaven = "@file:DependsOnMaven"
        val depends = "//DEPS "

        text.trim().let { s ->
            val deprecatedItems: MutableList<DeprecatedItem> = mutableListOf()

            val dependencies = when {
                s.startsWith(fileDependsOnMaven) -> {
                    extractQuotedValuesInParenthesis(s.substring(fileDependsOnMaven.length))
                }

                s.startsWith(fileDependsOn) -> {
                    extractQuotedValuesInParenthesis(s.substring(fileDependsOn.length))
                }

                s.startsWith(depends) -> {
                    val values = extractValues(s.substring(depends.length))
                    deprecatedItems.add(createDeprecatedAnnotation(scriptLocation,
                        line,
                        deprecatedAnnotation,
                        text,
                        "@file:DependsOn(" + values.joinToString(", ") { "\"${it.trim()}\"" } + ")"))

                    values
                }

                else -> emptyList()
            }

            val dependencyAnnotations = dependencies.map {
                val validated = validateDependency(it)
                Dependency(validated)
            }

            return dependencyAnnotations + deprecatedItems
        }
    }

    fun parseEntry(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
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
                            scriptLocation, line, deprecatedAnnotation, text, "@file:EntryPoint(\"$value\")"
                        )
                    )
                }

                else -> emptyList()
            }
        }
    }

    fun parseRepository(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
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
                        str += """, user="${repository.user}""""
                    }

                    if (repository.password.isNotBlank()) {
                        str += """, password="${repository.password}""""
                    }

                    return listOf(
                        repository, createDeprecatedAnnotation(
                            scriptLocation, line, deprecatedAnnotation, text, "@file:Repository($str)"
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

    fun parseKotlinOpts(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        val fileKotlinOptions = "@file:KotlinOptions"
        val fileKotlinOpts = "@file:KotlinOpts"
        val kotlinOpts = "//KOTLIN_OPTS "

        text.trim().let {
            return when {
                it.startsWith(fileKotlinOptions) -> extractQuotedValuesInParenthesis(it.substring(fileKotlinOptions.length)).map {
                    KotlinOpt(it)
                }

                it.startsWith(fileKotlinOpts) -> {
                    val values = extractQuotedValuesInParenthesis(it.substring(fileKotlinOpts.length))

                    values.map { KotlinOpt(it) } + createDeprecatedAnnotation(scriptLocation,
                        line,
                        deprecatedAnnotation,
                        text,
                        "@file:KotlinOptions(" + values.joinToString(
                            ", "
                        ) { "\"$it\"" } + ")")
                }

                it.startsWith(kotlinOpts) -> {
                    val values = extractValues(it.substring(kotlinOpts.length))
                    values.map { KotlinOpt(it) } + createDeprecatedAnnotation(scriptLocation,
                        line,
                        deprecatedAnnotation,
                        text,
                        "@file:KotlinOptions(" + values.joinToString(
                            ", "
                        ) { "\"$it\"" } + ")")
                }

                else -> emptyList()
            }
        }
    }

    fun parseCompilerOpts(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        val fileCompilerOptions = "@file:CompilerOptions"
        val fileCompilerOpts = "@file:CompilerOpts"
        val compilerOpts = "//COMPILER_OPTS "

        text.trim().let {
            return when {
                it.startsWith(fileCompilerOptions) -> extractQuotedValuesInParenthesis(it.substring(fileCompilerOptions.length)).map {
                    CompilerOpt(it)
                }

                it.startsWith(fileCompilerOpts) -> {
                    val values = extractQuotedValuesInParenthesis(it.substring(fileCompilerOpts.length))

                    values.map { CompilerOpt(it) } + createDeprecatedAnnotation(scriptLocation,
                        line,
                        deprecatedAnnotation,
                        text,
                        "@file:CompilerOptions(" + values.joinToString(
                            ", "
                        ) { "\"$it\"" } + ")")
                }

                it.startsWith(compilerOpts) -> {
                    val values = extractValues(it.substring(compilerOpts.length))
                    values.map { CompilerOpt(it) } + createDeprecatedAnnotation(scriptLocation,
                        line,
                        deprecatedAnnotation,
                        text,
                        "@file:CompilerOptions(" + values.joinToString(
                            ", "
                        ) { "\"$it\"" } + ")")
                }

                else -> emptyList()
            }
        }
    }

    fun parsePackage(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
        val packagePrefix = "package "

        text.trim().let {
            if (it.startsWith(packagePrefix)) {
                return listOf(PackageName(it.substring(packagePrefix.length)))
            }
            return emptyList()
        }
    }

    fun parseImport(scriptLocation: ScriptLocation, line: Int, text: String): List<ScriptAnnotation> {
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
        scriptLocation: ScriptLocation, line: Int, introText: String, existing: String, replacement: String
    ): DeprecatedItem =
        DeprecatedItem(scriptLocation, line, "$introText\n$existing\nshould be replaced with:\n$replacement")
}
