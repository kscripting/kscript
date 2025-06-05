package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.KotlinOpt // Assuming KotlinOpt might be needed, or it's part of Script
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.ScriptType
import io.github.kscripting.shell.model.writeText
import java.io.File

data class JarArtifact(val path: OsPath, val execClassName: String)

class JarArtifactCreator(private val executor: Executor) {

    // Signature changed: basePath and resolvedDependencies (Set<OsPath>) are replaced by sandboxDir and m2RepoDependencies (List<File>)
    fun create(script: Script, sandboxDir: File, m2RepoDependencies: List<File>): JarArtifact {
        val outDir = sandboxDir.resolve("out").also { it.mkdirs() }

        // Capitalize first letter and get rid of dashes
        val baseClassName = script.scriptLocation.scriptName
            .replace("[^A-Za-z0-9]".toRegex(), "_")
            .replaceFirstChar { it.titlecase() }
            .let { if ("^[0-9]".toRegex().containsMatchIn(it)) "_$it" else it }

        // Revised execClassName logic
        val execClassName = if (script.scriptLocation.scriptType == ScriptType.KTS) {
            if (script.packageName.value.isNotBlank()) {
                "${script.packageName.value}.${baseClassName}Kt"
            } else {
                "${baseClassName}Kt"
            }
        } else { // For .kt files
            script.entryPoint?.value ?: if (script.packageName.value.isNotBlank()) {
                "${script.packageName.value}.${baseClassName}Kt"
            } else {
                "${baseClassName}Kt"
            }
        }

        val jarFileOsPath = OsPath.createOrThrow(outDir.resolve("scriplet.jar").absolutePath) // Output JAR path
        val tempScriptFileName = baseClassName + script.scriptLocation.scriptType.extension
        val tempScriptFile = outDir.resolve(tempScriptFileName) // Temporary script file in sandbox/out

        // Write execClassName to a file in the sandbox for potential use by executor
        // (though BasicJvmScriptingHost might not need this if execClassName is standard)
        val execClassNameFile = outDir.resolve("scripletExecClassName.txt")
        execClassNameFile.writeText(execClassName)

        var scriptContent = script.resolvedCode

        // Package declaration for KTS: This logic might need to be revisited.
        // If KscriptBase handles packaging, or if @file:Package is used, this might be redundant
        // or conflict. For now, keeping it as it was for the script's own content.
        if (script.scriptLocation.scriptType == ScriptType.KTS &&
            script.packageName.value.isNotBlank() &&
            !scriptContent.trimStart().startsWith("package ")
        ) {
            scriptContent = "package ${script.packageName.value}\n\n$scriptContent"
        }

        // Write the (potentially modified) script content to the temporary file in sandbox/out
        FileUtils.createFile(OsPath.createOrThrow(tempScriptFile.absolutePath), scriptContent)

        val filesToCompile = setOf(OsPath.createOrThrow(tempScriptFile.absolutePath))

        // Convert m2RepoDependencies (List<File>) to Set<OsPath> for executor.compileKotlin
        val dependenciesToCompile = m2RepoDependencies.map { OsPath.createOrThrow(it.absolutePath) }.toSet()

        executor.compileKotlin(
            jarFileOsPath,
            dependenciesToCompile, // Compile classpath from m2RepoDependencies
            filesToCompile,        // Only the user's script file
            script.compilerOpts + CompilerOpt("-Xallow-any-scripts-in-source-roots") // Retain this important opt
            // Note: script.kotlinOpts are runtime options, not typically for kotlinc
        )

        return JarArtifact(jarFileOsPath, execClassName)
    }
}
