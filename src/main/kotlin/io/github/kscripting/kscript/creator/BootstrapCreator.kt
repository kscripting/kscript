package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.model.ScriptSource
import io.github.kscripting.kscript.util.Logger.infoMsg
import java.io.File

class BootstrapCreator {
    fun create(script: Script) {
        if (script.scriptLocation.scriptSource != ScriptSource.FILE) {
            throw IllegalStateException("Can not add bootstrap header to resources, which are not regular Kotlin files.")
        }

        val scriptLines = script.rootNode.sections.map { it.code }.dropWhile {
            it.startsWith("#!/") && it != "#!/bin/bash"
        }

        val bootstrapHeader = Templates.bootstrapHeader.lines()

        if (scriptLines.getOrNull(0) == bootstrapHeader[0] && scriptLines.any { "command -v kscript >/dev/null 2>&1 || " in it }) {
            val lastHeaderLine = bootstrapHeader.findLast { it.isNotBlank() }!!
            val preexistingHeader = scriptLines.dropLastWhile { it != lastHeaderLine }.joinToString("\n")
            throw IllegalStateException("Bootstrap header already detected:\n\n$preexistingHeader\n\nYou can remove it to force the re-generation")
        }

        File(script.scriptLocation.sourceUri!!).writeText((bootstrapHeader + scriptLines).joinToString("\n"))
        infoMsg("${script.scriptLocation.sourceUri} updated")
    }
}
