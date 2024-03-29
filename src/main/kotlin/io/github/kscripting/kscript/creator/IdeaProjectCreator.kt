package io.github.kscripting.kscript.creator

import io.github.kscripting.kscript.code.GradleTemplates
import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils
import io.github.kscripting.kscript.util.FileUtils.resolveUniqueFilePath
import io.github.kscripting.kscript.util.Logger.devMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.shell.model.OsPath
import java.net.URI

class IdeaProjectCreator(private val executor: Executor) {

    fun create(
        basePath: OsPath,
        script: Script,
        userArgs: List<String>,
        uriLocalPathProvider: (URI) -> OsPath
    ): OsPath {
        infoMsg("Setting up idea project...")
        val srcPath = basePath.resolve("src/")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.scriptLocation.sourceUri
            val filePath =
                resolveUniqueFilePath(
                    srcPath,
                    scriptNode.scriptLocation.scriptName,
                    scriptNode.scriptLocation.scriptType
                )

            if (sourceUri == null) {
                FileUtils.createFile(filePath, scriptNode.sections.joinToString("\n") { it.code })
            } else {
                val targetPath = uriLocalPathProvider(sourceUri)
                devMsg("link: $filePath, target: $targetPath")

                FileUtils.symLinkOrCopy(filePath, targetPath)
            }
        }

        FileUtils.createFile(
            basePath.resolve(".idea/runConfigurations/Main.xml"),
            Templates.createRunConfig(
                script.rootNode.scriptLocation.scriptName,
                script.rootNode.scriptLocation.scriptType,
                userArgs
            )
        )

        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradleIdeaScript(script)
        )

        executor.runGradleInIdeaProject(basePath)

        return basePath
    }
}
