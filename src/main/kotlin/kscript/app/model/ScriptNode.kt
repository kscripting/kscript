package kscript.app.model

data class ScriptNode(
    val location: Location,
    val sections: List<Section>,
) : ScriptAnnotation
