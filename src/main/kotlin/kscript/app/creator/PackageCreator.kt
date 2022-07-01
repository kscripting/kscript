package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Executor
import kscript.app.util.FileUtils
import kscript.app.util.Logger.infoMsg
import kscript.app.util.OsPath
import kscript.app.util.toNativeFile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class PackageCreator(private val executor: Executor) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */

    fun makeHeader(options: List<String>) : String {
        val opts = options.joinToString(" ").trim()

        return Templates.executeHeader.replace("java -jar", "java $opts -jar")
    }

    fun packageKscript(basePath: OsPath, script: Script, jarArtifact: JarArtifact): OsPath {
        val baseName = script.scriptName

        infoMsg("Packaging script '${script.scriptName}' into standalone executable...")

        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        val options = mutableListOf("")

        for (opt in script.kotlinOpts) {
            val s = opt.value
            if (s.startsWith("-J")) {
                options.add(s.substring(2))
            }
        }
        FileUtils.createFile(basePath.resolve("exec_header.sh"), makeHeader(options))
        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradlePackageScript(script, jarArtifact)
        )

        executor.createPackage(basePath)

        val executable = File(basePath.resolve("build/libs/$baseName").toString())
        executable.setExecutable(true)

        infoMsg("Finished packaging '${script.scriptName}'; executable path: ${basePath}/build/libs/$baseName")

        return basePath
    }
}
