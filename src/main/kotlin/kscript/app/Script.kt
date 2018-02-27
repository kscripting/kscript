package kscript.app

import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL

/* Immutable script class */
data class Script(
        val lines: List<String>,
        val extension: String = "kts",
        val file: File = createTmpScript(lines.joinToString("\n"), extension),
        private var originUrl: URL? = null) : Iterable<String> {

    constructor(scriptFile: File) : this(scriptFile.readLines(), scriptFile.extension, scriptFile)
    constructor(url: URL) : this(fetchFromURL(url.toString())) {
        originUrl = url
    }

    /** Returns a the namespace/package of the script (if declared). */
    val pckg by lazy {
        lines.find { it.startsWith("package ") }?.split("[ ]+".toRegex())?.get(1)?.run { this + "." }
    }

    val includes by lazy {
        lines.mapNotNull {
            if (isIncludeDirective(it)) extractIncludeTarget(it) else null
        }.map {
            when {
                // absolute file/url
                isUrl(it) -> Script(URL(it))
                it.startsWith("/") -> Script(File(it))

                // relative file/url
                originUrl != null -> Script(originUrl!!.toURI().resolve(it).normalize().toURL())
                else -> Script(file.parentFile.resolve(it).normalize())
            }
        }.toMutableList()
    }

    val deps by lazy {
        lines.flatMap {
            if (isDependDeclare(it)) extractDependencies(it) else emptyList()
        } + if (lines.any { isKscriptAnnotation(it) }) listOf("com.github.holgerbrandl:kscript-annotations:1.2") else emptyList()
    }

    val repos: List<MavenRepo> by lazy { collectRepos() }

    fun <T> recursive(property: Script.() -> Iterable<T>): Set<T> {
        return linkedSetOf<T>().also { recursivelyCollectTo(it, property) }
    }

    private fun <T> recursivelyCollectTo(out: MutableSet<T>, property: Script.() -> Iterable<T>) {
        out.addAll(property())
        includes.forEach { it.recursivelyCollectTo(out, property) }
    }

    override fun toString(): String = lines.joinToString("\n")


    override fun iterator(): Iterator<String> = lines.iterator()


    fun stripShebang(): Script = lines.filterNot { it.startsWith("#!/") }.let { Script(it, extension) }


    fun createTmpScript() = createTmpScript(toString(), extension)


    fun prependWith(preamble: String): Script = Script(preamble.lines() + lines, extension).consolidateStructure()


    fun consolidateStructure(): Script {
        val codeBits = mutableListOf<String>()
        val imports = emptySet<String>().toMutableSet()
        val annotations = emptySet<String>().toMutableSet()

        stripShebang().forEach {
            if (it.startsWith(IMPORT_STATMENT_PREFIX)) {
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
                sorted().map(String::trim).distinct().map { appendln(it) }
                // kotlin seems buggy here, so maybe we need to recode annot-directives into comment directives
                if (isNotEmpty()) appendln()
            }

            // restablish the package statement if present
            lines.firstOrNull { it.startsWith(PACKAGE_STATEMENT_PREFIX) }?.let {
                appendln(it)
            }

            with(imports) {
                sorted().map(String::trim).distinct().map { appendln(it) }
                if (isNotEmpty()) appendln()
            }

            // append actual script
            codeBits.forEach { appendln(it) }
        }

        return Script(consolidated.lines(), extension)
    }
}


private fun isKscriptAnnotation(line: String) =
    listOf("DependsOn", "KotlinOpts", "Include", "EntryPoint", "MavenRepository", "DependsOnMaven")
        .any { line.contains("^@file:${it}[(]".toRegex()) }


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
            .split(")")[0].trim(' ', '"')
    line.startsWith(ENTRY_COMMENT_PREFIX) ->
        line.split("[ ]+".toRegex()).last()
    else ->
        throw IllegalArgumentException("can not extract entry point from non-directive")
}


//
// DependsOn directive
//


private val DEPS_COMMENT_PREFIX = "//DEPS "
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
        dependencies += "com.github.holgerbrandl:kscript-annotations:1.2"
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
    annotationArgs.filter { it.contains(',') }.let {
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
//        .split(")")[0].split(",")
//        .map { it.trim(' ', '"') }


    line.contains(DEPSMAVEN_ANNOT_PREFIX) -> line
        .replaceFirst(DEPSMAVEN_ANNOT_PREFIX, "")
        .split(")")[0].trim(' ', '"').let { listOf(it) }

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


data class MavenRepo(val id: String, val url: String)

/**
 * Collect custom artifact repos declared with @file:MavenRepository
 */
fun Script.collectRepos(): List<MavenRepo> {
    val dependsOnMavenPrefix = "^@file:MavenRepository[(]".toRegex()
    // only supported annotation format for now

    // @file:MavenRepository("imagej", "http://maven.imagej.net/content/repositories/releases/")
    return lines
        .filter { it.contains(dependsOnMavenPrefix) }
        .map { it.replaceFirst(dependsOnMavenPrefix, "").split(")")[0] }
        .map { it.split(",").map { it.trim(' ', '"', '(') }.let { MavenRepo(it[0], it[1]) } }

    // todo add credential support https://stackoverflow.com/questions/36282168/how-to-add-custom-maven-repository-to-gradle
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
    kotlinOpts += lines
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

    var compilerOpts = lines.filter { it.startsWith(koptsPrefix) }.map { it.replaceFirst(koptsPrefix, "").trim() }

    val annotatonPrefix = "^@file:CompilerOpts[(]".toRegex()
    compilerOpts += lines
        .filter { it.contains(annotatonPrefix) }
        .map { it.replaceFirst(annotatonPrefix, "").split(")")[0] }
        .map { it.trim(' ', '"') }

    return compilerOpts.joinToString(" ")
}
