package kscript.app.shell

import kscript.app.model.ScriptType
import kscript.app.util.Logger
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object FileUtils {
    fun createFile(path: OsPath, content: String): OsPath {
        createDirsIfNeeded(path)
        path.writeText(content)
        return path
    }

    fun symLinkOrCopy(link: OsPath, target: OsPath): OsPath {
        createDirsIfNeeded(link)

        val isSymlinked = createSymLink(link, target)

        if (!isSymlinked) {
            Logger.warnMsg("Failed to create symbolic link to script. Copying instead...")
            target.copyTo(link)
        }

        return link
    }

    fun createDirsIfNeeded(path: OsPath) {
        val dir = path.parent

        if (!dir.exists()) {
            dir.createDirectories()
        }
    }

    fun createSymLink(link: OsPath, target: OsPath): Boolean {
        return try {
            Files.createSymbolicLink(link.toNativePath(), target.toNativePath())
            true
        } catch (e: IOException) {
            false
        }
    }

    fun resolveUniqueFilePath(basePath: OsPath, fileName: String, scriptType: ScriptType): OsPath {
        var path = basePath.resolve(fileName + scriptType.extension)

        var counter = 1
        while (path.exists()) {
            path = basePath.resolve(fileName + "_$counter" + scriptType.extension)
            counter++
        }

        return path
    }
}
