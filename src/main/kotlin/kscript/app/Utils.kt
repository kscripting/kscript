package kscript.app

import kscript.app.model.ScriptNode
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

object Utils {
    fun resolveCodeForNode(scriptNode: ScriptNode): String {
        val sb = StringBuilder()

        for (section in scriptNode.sections) {
            sb.append(section.code).append('\n')
        }

        return sb.toString()
    }
}
