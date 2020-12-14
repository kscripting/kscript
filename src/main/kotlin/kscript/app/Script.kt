package kscript.app

import java.io.File

/* Immutable script class */
data class Script(val lines: List<String>, val extension: String = "kts") : Iterable<String> {

    constructor(scriptFile: File) : this(scriptFile.readLines(), scriptFile.extension)

    /** Returns a the namespace/package of the script (if declared). */
    val pckg by lazy {
        lines.find { it.startsWith("package ") }?.split("[ ]+".toRegex())?.get(1)?.run { this + "." }
    }

    override fun toString(): String = lines.joinToString("\n")


    override fun iterator(): Iterator<String> = lines.iterator()


    fun stripShebang(): Script = copy(lines.filterNot { it.startsWith("#!/") })


    fun createTmpScript() = createTmpScript(toString(), extension)


    fun prependWith(preamble: String): Script = copy(lines = preamble.lines() + lines).consolidateStructure()


    fun consolidateStructure(): Script {
        val codeBits = mutableListOf<String>()
        val imports = emptySet<String>().toMutableSet()
        val annotations = emptySet<String>().toMutableSet()

        stripShebang().forEach {
            if (it.startsWith(IMPORT_STATEMENT_PREFIX)) {
                imports.add(it)
            } else if (isKscriptAnnotation(it)) {
                annotations.add(it)
            } else if (!it.startsWith(PACKAGE_STATEMENT_PREFIX)) {
                // if its not an annotation directive or an import, emit as is
                codeBits += it
            }
        }

        val consolidated = StringBuilder().apply {
            // file annotations have to be on top of everything, just switch places between your annotation and package
            with(annotations) {
                sorted().map(String::trim).distinct().map { appendLine(it) }
                // kotlin seems buggy here, so maybe we need to recode annot-directives into comment directives
                if (isNotEmpty()) appendLine()
            }

            // restablish the package statement if present
            lines.firstOrNull { it.startsWith(PACKAGE_STATEMENT_PREFIX) }?.let {
                appendLine(it)
            }

            with(imports) {
                sorted().map(String::trim).distinct().map { appendLine(it) }
                if (isNotEmpty()) appendLine()
            }

            // append actual script
            codeBits.forEach { appendLine(it) }
        }

        return copy(lines = consolidated.lines())
    }
}


private val KSCRIPT_DIRECTIVE_ANNO: List<Regex> = listOf("DependsOn", "KotlinOpts", "Include", "EntryPoint", "MavenRepository", "DependsOnMaven", "CompilerOpts")
    .map { "^@file:$it[(]".toRegex() }

private fun isKscriptAnnotation(line: String) =
    KSCRIPT_DIRECTIVE_ANNO.any { line.contains(it) }

//
// Entry directive
//

private val ENTRY_ANNOT_PREFIX = "^@file:EntryPoint[(]".toRegex()
private const val ENTRY_COMMENT_PREFIX = "//ENTRY "


fun isEntryPointDirective(line: String) =
    line.startsWith(ENTRY_COMMENT_PREFIX) || line.contains(ENTRY_ANNOT_PREFIX)


fun Script.findEntryPoint(): String? {
    return lines.find { isEntryPointDirective(it) }?.let { extractEntryPoint(it) }
}

private fun extractEntryPoint(line: String) = when {
    line.contains(ENTRY_ANNOT_PREFIX) ->
        line
            .replaceFirst(ENTRY_ANNOT_PREFIX, "")
            .substringBeforeLast(")").trim(' ', '"')
    line.startsWith(ENTRY_COMMENT_PREFIX) ->
        line.split("[ ]+".toRegex()).last()
    else ->
        throw IllegalArgumentException("can not extract entry point from non-directive")
}


//
// DependsOn directive
//


private const val DEPS_COMMENT_PREFIX = "//DEPS "
private val DEPS_ANNOT_PREFIX = "^@file:DependsOn[(]".toRegex()
private val DEPSMAVEN_ANNOT_PREFIX = "^@file:DependsOnMaven[(]".toRegex()


fun Script.collectDependencies(): List<String> {
    // Make sure that dependencies declarations are well formatted
    if (lines.any { it.startsWith("// DEPS") }) {
        error("Dependencies must be declared by using the line prefix //DEPS")
    }

    val dependencies = lines.filter {
        isDependDeclare(it)
    }.flatMap {
        extractDependencies(it)
    }.toMutableList()


    // if annotations are used add dependency on kscript-annotations
    if (lines.any { isKscriptAnnotation(it) }) {
        dependencies += "com.github.holgerbrandl:kscript-annotations:1.4"
    }

    return dependencies.distinct()
}


private fun String.extractAnnotParams(): List<String> {
    // https://stackoverflow.com/questions/171480/regex-grabbing-values-between-quotation-marks
    val annotationArgs = """(["'])(\\?.*?)\1""".toRegex()
        .findAll(this).toList().map {
        it.groupValues[2]
    }

    // fail if any argument is a comma separated list of artifacts (see #101)
    annotationArgs.filter { it.contains(",[^)]".toRegex()) }.let {
        errorIf(it.isNotEmpty()) {
            "Artifact locators must be provided as separate annotation arguments and not as comma-separated list: " + it
        }
    }

    return annotationArgs
}

internal fun extractDependencies(line: String) = when {
    line.contains(DEPS_ANNOT_PREFIX) -> line
        .replaceFirst(DEPS_ANNOT_PREFIX, "")
        .extractAnnotParams()
//        ..substringBeforeLast(")").split(",")
//        .map { it.trim(' ', '"') }


    line.contains(DEPSMAVEN_ANNOT_PREFIX) -> line
        .replaceFirst(DEPSMAVEN_ANNOT_PREFIX, "")
        .substringBeforeLast(")").trim(' ', '"').let { listOf(it) }

    line.startsWith(DEPS_COMMENT_PREFIX) ->
        line.split("[ ;,]+".toRegex()).drop(1).map(String::trim)

    else ->
        throw IllegalArgumentException("can not extract entry point from non-directive")
}


private fun isDependDeclare(line: String) =
    line.startsWith(DEPS_COMMENT_PREFIX) || line.contains(DEPS_ANNOT_PREFIX) || line.contains(DEPSMAVEN_ANNOT_PREFIX)


//
// Custom Artifact Repos
//


data class MavenRepo(val id: String, val url: String, val user: String = "", val password: String = "")

/**
 * Collect custom artifact repos declared with @file:MavenRepository
 */
fun Script.collectRepos(): List<MavenRepo> {
    val mvnRepoAnnotPrefix = "^@file:MavenRepository[(]".toRegex()
    // only supported annotation format for now

    // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
    // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/", user="user", password="pass")
    return lines
        .filter { it.contains(mvnRepoAnnotPrefix) }
        .map { it.replaceFirst(mvnRepoAnnotPrefix, "").substringBeforeLast(")") }
        .map { repoLine ->
            repoLine.split(",").map { it.trim(' ', '"', '(') }.let { annotationParams ->
                val keyValSep = "[ ]*=[ ]*\"".toRegex()

                val namedArgs = annotationParams
                        .filter { it.contains(keyValSep) }
                        .map { keyVal -> keyVal.split(keyValSep).map { it.trim(' ', '\"') }.let{ it.first() to it.last()}}
                        .toMap()

                if (annotationParams.size < 2) {
                    throw IllegalArgumentException("Missing ${2 - annotationParams.size} of the required arguments for @file:MavenRepository(id, url)")
                }
                MavenRepo(
                        namedArgs.getOrDefault("id", annotationParams[0]),
                        namedArgs.getOrDefault("url", annotationParams[1]),
                        namedArgs.getOrDefault("user", annotationParams.getOrNull(2) ?: ""),
                        namedArgs.getOrDefault("password", annotationParams.getOrNull(3) ?: "")
                )
            }
        }
}


//
// Runtime Configuration
//


/**
 * Collect runtime options declared using //KOTLIN_OPTS or @file:KotlinOpts
 */
fun Script.collectRuntimeOptions(): String {
    val koptsPrefix = "//KOTLIN_OPTS "

    var kotlinOpts = lines.filter { it.startsWith(koptsPrefix) }.map { it.replaceFirst(koptsPrefix, "").trim() }

    //support for @file:KotlinOpts see #47
    val annotatonPrefix = "^@file:KotlinOpts[(]".toRegex()
    kotlinOpts = kotlinOpts + lines
            .filter { it.contains(annotatonPrefix) }
            .map { it.replaceFirst(annotatonPrefix, "").split(")")[0] }
            .map { it.trim(' ', '"') }


    // Append $KSCRIPT_KOTLIN_OPTS if defined in the parent environment
    System.getenv()["KSCRIPT_KOTLIN_OPTS"]?.run {
        kotlinOpts = kotlinOpts + this
    }

    return kotlinOpts.joinToString(" ")
}


/**
 * Collect compiler options declared using //COMPILER_OPTS or @file:CompilerOpts
 */
fun Script.collectCompilerOptions(): String {
    val koptsPrefix = "//COMPILER_OPTS "

    val compilerOpts = lines.filter { it.startsWith(koptsPrefix) }.map { it.replaceFirst(koptsPrefix, "").trim() }.toMutableList()

    val annotationPrefix = "^@file:CompilerOpts[(]".toRegex()
    compilerOpts += lines
        .filter { it.contains(annotationPrefix) }
        .map { it.replaceFirst(annotationPrefix, "").split(")")[0] }
        .map { it.trim(' ', '"') }

    return compilerOpts.joinToString(" ")
}
