package io.github.kscripting.kscript.util

import io.github.kscripting.kscript.model.*
import io.github.kscripting.kscript.resolver.ResolutionContext
import org.apache.commons.codec.digest.DigestUtils
import java.net.URI

object ScriptUtils {
    fun extractScriptFileDetails(uri: URI): Pair<String, ScriptType?> {
        return extractScriptFileDetails(uri.normalize().path)
    }

    private fun extractScriptFileDetails(path: String): Pair<String, ScriptType?> {
        var filename = path

        val idx = path.lastIndexOf("/")
        if (idx >= 0) {
            filename = path.substring(idx + 1, path.length)
        }

        val scriptType = ScriptType.findByExtension(filename)

        if (scriptType != null) {
            //Drop extension
            filename = filename.dropLast(scriptType.extension.length)
        }

        return Pair(filename, scriptType)
    }

    fun prependPreambles(preambles: List<String>, string: String): String {
        return preambles.joinToString("\n") + string
    }

    fun resolveScriptType(code: String): ScriptType {
        return if (code.contains("fun main")) ScriptType.KT else ScriptType.KTS
    }

    fun resolveCode(packageName: PackageName?, importNames: Set<ImportName>, scriptNode: ScriptNode): String {
        val sortedImports = importNames.sortedBy { it.value }.toList()
        val sb = StringBuilder()

        if (packageName != null) {
            sb.append("package ${packageName.value}\n\n")
        }

        sortedImports.forEach {
            sb.append("import ${it.value}\n")
        }

        resolveSimpleCode(sb, scriptNode)

        return sb.toString()
    }

    private fun resolveSimpleCode(sb: StringBuilder, scriptNode: ScriptNode, lastLineIsEmpty: Boolean = false) {
        var isLastLineEmpty = lastLineIsEmpty

        for (section in scriptNode.sections) {
            val scriptNodes = section.scriptAnnotations.filterIsInstance<ScriptNode>()

            if (scriptNodes.isNotEmpty()) {
                val subNode = scriptNodes.single()
                resolveSimpleCode(sb, subNode, isLastLineEmpty)
                continue
            }

            val droppedAnnotations = section.scriptAnnotations.filter { it !is Code }
            if (droppedAnnotations.isNotEmpty()) {
                continue
            }

            if (section.code.isNotEmpty() || (!isLastLineEmpty && section.code.isEmpty())) {
                sb.append(section.code).append('\n')
            }

            isLastLineEmpty = section.code.isEmpty()
        }
    }

    fun calculateHash(code: String, resolutionContext: ResolutionContext): String {
        val text =
            code + resolutionContext.repositories.joinToString("\n") + resolutionContext.dependencies.joinToString("\n") + resolutionContext.compilerOpts.joinToString(
                "\n"
            ) + resolutionContext.kotlinOpts.joinToString("\n") + (resolutionContext.entryPoint ?: "")

        return DigestUtils.md5Hex(text)
    }
}
