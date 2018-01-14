package kscript.app

import java.io.File
import java.lang.IllegalArgumentException

/* Immutable script class */
data class Script(val lines: List<String>, val extension: String = "kts") : Iterable<String> {

    constructor(scriptFile: File) : this(scriptFile.readLines(), scriptFile.extension)

    /** Returns a the namespace/package of the script (if declared). */
    val pckg by lazy {
        lines.find { it.startsWith("package ") }?.split("[ ]+".toRegex())?.get(1)?.run { this + "." }
    }

    override fun toString(): String = lines.joinToString("\n")


    override fun iterator(): Iterator<String> = lines.iterator()


    fun stripShebang(): Script = lines.filterNot { it.startsWith("#!/") }.let { copy(it) }


    fun createTmpScript() = createTmpScript(toString(), extension)


    fun prependWith(preamble: String): Script = copy(lines = preamble.lines() + lines).consolidateStructure()


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

        return copy(lines = consolidated.lines())
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
        dependencies += "com.github.holgerbrandl:kscript-annotations:1.1"
    }

    return dependencies.distinct()
}


private fun extractDependencies(line: String) = when {
    line.contains(DEPS_ANNOT_PREFIX) -> line
        .replaceFirst(DEPS_ANNOT_PREFIX, "")
        .split(")")[0].split(",")
        .map { it.trim(' ', '"') }

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
