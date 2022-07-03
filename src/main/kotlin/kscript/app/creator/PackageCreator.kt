package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Executor
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import kscript.app.util.OsPath
import kscript.app.util.toNativeFile

class PackageCreator(private val executor: Executor) {
    fun packageKscript(basePath: OsPath, packageFile: OsPath, script: Script, jarArtifact: JarArtifact): OsPath {
        infoMsg("Packaging script '${script.scriptName}' into standalone executable...")

        FileUtils.createFile(basePath.resolve("exec_header.sh"), Templates.createExecuteHeader(script.kotlinOpts))
        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact)
        )

        executor.createPackage(basePath)
        packageFile.toNativeFile().setExecutable(true)

        infoMsg("Packaging finished.")

        return packageFile
    }
}
