package io.github.kscripting.kscript.model

enum class ScriptType(val extension: String) {
    KT(".kt"), KTS(".kts");

    companion object {
        fun findByExtension(name: String): ScriptType? = values().find { type -> name.endsWith(type.extension, true) }
    }
}
