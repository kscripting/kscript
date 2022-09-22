package io.github.kscripting.kscript.model

import io.github.kscripting.shell.model.ScriptLocation

data class ScriptNode(
    val scriptLocation: ScriptLocation,
    val sections: List<Section>,
) : ScriptAnnotation
