package io.github.kscripting.kscript.model

data class ScriptNode(
    val scriptLocation: ScriptLocation,
    val sections: List<Section>,
) : ScriptAnnotation
