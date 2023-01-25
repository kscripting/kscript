package io.github.kscripting.kscript

import io.github.kscripting.kscript.model.ScriptNode

object Utils {
    fun resolveCodeForNode(scriptNode: ScriptNode): String {
        val sb = StringBuilder()

        for (section in scriptNode.sections) {
            sb.append(section.code).append('\n')
        }

        return sb.toString()
    }
}
