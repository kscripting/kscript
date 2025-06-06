package io.github.kscripting.kscript.model

/**
 * Represents project coordinates that can be defined in a kscript file
 * via the `@file:ProjectCoordinates(group="...", artifact="...", version="...")` annotation.
 * All properties are optional.
 */
data class ProjectCoordinates(
    val group: String? = null,
    val artifact: String? = null,
    val version: String? = null
) : ScriptAnnotation
