package kscript.app.creator

import kscript.app.code.GradleTemplates
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.FileUtils
import kscript.app.util.FileUtils.resolveUniqueFilePath
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.util.OsPath
import java.net.URI

class IdeaProjectCreator {
    fun create(basePath: OsPath, script: Script, userArgs: List<String>, uriLocalPathProvider: (URI) -> OsPath): OsPath {
        infoMsg("Setting up idea project...")
        val srcPath = basePath.resolve("src/")

        for (scriptNode in script.scriptNodes) {
            val sourceUri = scriptNode.sourceUri
            val filePath = resolveUniqueFilePath(srcPath, scriptNode.scriptName, scriptNode.scriptType)

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
            Templates.createRunConfig(script.rootNode.scriptName, script.rootNode.scriptType, userArgs)
        )

        FileUtils.createFile(
            basePath.resolve("build.gradle.kts"), GradleTemplates.createGradleIdeaScript(script)
        )

        return basePath
    }
}
