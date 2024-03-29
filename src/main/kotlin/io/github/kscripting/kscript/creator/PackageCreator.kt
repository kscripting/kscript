package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.code.GradleTemplates
import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils
import io.github.kscripting.shell.model.toNativeFile
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.kscript.util.Logger.infoMsg

class PackageCreator(private val executor: Executor) {
    fun packageKscript(basePath: OsPath, packageFile: OsPath, script: Script, jarArtifact: JarArtifact): OsPath {
        infoMsg("Packaging script '${script.scriptLocation.scriptName}' into standalone executable...")

        FileUtils.createFile(basePath.resolve("exec_header.sh"), Templates.createExecuteHeader(script.kotlinOpts))
        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact)
        )

        executor.createPackage(basePath)
        packageFile.toNativeFile().setExecutable(true)

        return packageFile
    }
}
