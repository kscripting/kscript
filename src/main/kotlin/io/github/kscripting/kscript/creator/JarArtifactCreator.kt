package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.ScriptType
import io.github.kscripting.shell.model.writeText

data class JarArtifact(val path: OsPath, val execClassName: String)

class JarArtifactCreator(private val executor: Executor) {

    fun create(basePath: OsPath, script: Script, resolvedDependencies: Set<OsPath>): JarArtifact {
        // Capitalize first letter and get rid of dashes (since this is what kotlin compiler is doing for the wrapper to create a valid java class name)
        // For valid characters see https://stackoverflow.com/questions/4814040/allowed-characters-in-filename
        val className =
            script.scriptLocation.scriptName.replace("[^A-Za-z0-9]".toRegex(), "_").replaceFirstChar { it.titlecase() }
                // also make sure that it is a valid identifier by avoiding an initial digit (to stay in sync with what the kotlin script compiler will do as well)
                .let { if ("^[0-9]".toRegex().containsMatchIn(it)) "_$it" else it }

        // Define the entrypoint for the scriptlet jar
        val execClassName = if (script.scriptLocation.scriptType == ScriptType.KTS) {
            "Main_${className}"
        } else {
            """${script.packageName.value}.${script.entryPoint?.value ?: "${className}Kt"}"""
        }

        val jarFile = basePath.resolve("scriplet.jar")
        val scriptFile = basePath.resolve(className + script.scriptLocation.scriptType.extension)
        val execClassNameFile = basePath.resolve("scripletExecClassName.txt")

        execClassNameFile.writeText(execClassName)

        var scriptContent = script.resolvedCode

        if (script.scriptLocation.scriptType == ScriptType.KTS &&
            script.packageName.value.isNotBlank() &&
            !scriptContent.trimStart().startsWith("package ")
        ) {
            scriptContent = "package ${script.packageName.value}\n\n$scriptContent"
        }

        val filesToCompile = mutableSetOf<OsPath>()

        if (script.scriptLocation.scriptType == ScriptType.KTS) {
            // For KTS scripts, combine script content and wrapper code into a single file.
            // The package declaration is handled by Templates.createWrapperForScript.
            val wrapperContent = Templates.createWrapperForScript(script.packageName, className)
            scriptContent = "$scriptContent\n\n$wrapperContent"

            FileUtils.createFile(scriptFile, scriptContent)
            filesToCompile.add(scriptFile)
        } else {
            // For KT files, keep the existing logic.
            FileUtils.createFile(scriptFile, scriptContent)
            filesToCompile.add(scriptFile)
        }

        executor.compileKotlin(
            jarFile,
            resolvedDependencies,
            filesToCompile,
            script.compilerOpts +
            // This options allows to work with Kotlin 1.9.x, where scripts in source roots are ignored
            CompilerOpt("-Xallow-any-scripts-in-source-roots")
        )

        return JarArtifact(jarFile, execClassName)
    }
}
