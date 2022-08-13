package kscript.app.creator

import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.model.ScriptSource
import kscript.app.util.Logger
import java.io.File

class BootstrapCreator {
    fun create(script: Script) {
        if (script.location.scriptSource != ScriptSource.FILE) {
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

        File(script.location.sourceUri!!).writeText((bootstrapHeader + scriptLines).joinToString("\n"))
        Logger.infoMsg("${script.location.sourceUri} updated")
    }
}
