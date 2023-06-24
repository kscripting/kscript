package io.github.kscripting.kscript.model

sealed interface ScriptAnnotation

@JvmInline
value class Include(val value: String) : ScriptAnnotation

@JvmInline
value class PackageName(val value: String) : ScriptAnnotation

@JvmInline
value class ImportName(val value: String) : ScriptAnnotation

@JvmInline
value class Dependency(val value: String) : ScriptAnnotation

@JvmInline
value class KotlinOpt(val value: String) : ScriptAnnotation

@JvmInline
value class CompilerOpt(val value: String) : ScriptAnnotation

@JvmInline
value class Entry(val value: String) : ScriptAnnotation

data class DeprecatedItem(val scriptLocation: ScriptLocation, val line: Int, val message: String) : ScriptAnnotation

data class Repository(val id: String, val url: String, val user: String = "", val password: String = "") :
    ScriptAnnotation

object SheBang : ScriptAnnotation
object Code : ScriptAnnotation
