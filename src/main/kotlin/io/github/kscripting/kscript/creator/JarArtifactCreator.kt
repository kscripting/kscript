package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.writeText

data class JarArtifact(val path: OsPath, val execClassName: String)

class JarArtifactCreator(private val executor: Executor) {

    fun create(basePath: OsPath, script: Script, resolvedDependencies: Set<OsPath>): JarArtifact {
        val execClassName = "${script.entryPoint.value}Kt"
        val jarFile = basePath.resolve("scriplet.jar")
        val scriptFile = basePath.resolve("Scriplet.kt")
        val execClassNameFile = basePath.resolve("scripletExecClassName.txt")

        execClassNameFile.writeText(execClassName)

        FileUtils.createFile(scriptFile, script.resolvedCode)

        val filesToCompile = mutableSetOf<OsPath>()
        filesToCompile.add(scriptFile)

        executor.compileKotlin(jarFile, resolvedDependencies, filesToCompile, script.compilerOpts)

        return JarArtifact(jarFile, execClassName)
    }
}
