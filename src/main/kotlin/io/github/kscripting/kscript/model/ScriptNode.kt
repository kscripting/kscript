package io.github.kscripting.kscript.model

data class ScriptNode(
    val location: Location,
    val sections: List<Section>,
) : ScriptAnnotation
