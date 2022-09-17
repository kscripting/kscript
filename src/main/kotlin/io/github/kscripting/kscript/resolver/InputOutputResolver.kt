package io.github.kscripting.kscript.resolver

import io.github.kscripting.kscript.cache.Cache
import io.github.kscripting.kscript.model.Content
import io.github.kscripting.kscript.model.OsConfig
import io.github.kscripting.kscript.shell.OsType
import io.github.kscripting.kscript.shell.OsPath
import io.github.kscripting.kscript.shell.toNativeOsPath
import io.github.kscripting.kscript.shell.toNativePath
import io.github.kscripting.kscript.util.ScriptUtils
import io.github.kscripting.kscript.util.UriUtils
import io.github.kscripting.kscript.util.UriUtils.isUrl
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.io.path.readText

class InputOutputResolver(private val osConfig: OsConfig, private val cache: Cache) {
    fun resolveContent(osPath: OsPath): Content {
        val uri = resolveUri(osPath)
        val content = osPath.toNativePath().readText()
        val (fileName, scriptType) = ScriptUtils.extractScriptFileDetails(uri)
        val contextUri = uri.resolve(".")
        return Content(
            content, scriptType ?: ScriptUtils.resolveScriptType(content), fileName, uri, contextUri, osPath
        )
    }

    fun resolveContent(uri: URI): Content {
        if (!isUrl(uri)) {
            return resolveContent(OsPath.createOrThrow(OsType.native, Paths.get(uri).toString()))
        }

        return cache.getOrCreateUriItem(uri) { sourceUrl, contentFile ->
            val resolvedUri = UriUtils.resolveRedirects(sourceUrl.toURL()).toURI()
            val content = resolvedUri.toURL().readText()
            val (fileName, scriptType) = ScriptUtils.extractScriptFileDetails(resolvedUri)
            val contextUri = resolvedUri.resolve(".")

            Content(
                content,
                scriptType ?: ScriptUtils.resolveScriptType(content),
                fileName,
                resolvedUri,
                contextUri,
                contentFile
            )
        }
    }

    fun resolveContentUsingInputStream(osPath: OsPath): Content {
        val resolvedUri = resolveUri(osPath)
        val contextUri = resolvedUri.resolve(".")
        val content = FileInputStream(osPath.toNativeOsPath().stringPath()).bufferedReader().readText()
        val scriptType = ScriptUtils.resolveScriptType(content)

        return Content(content, scriptType, osPath.pathParts.last(), resolvedUri, contextUri, osPath)
    }

    fun resolveUriRelativeToRoot(path: String): URI {
        return resolveUri(
            OsPath.createOrThrow(
                OsType.native, File(".").toPath().toAbsolutePath().root!!.resolve(path).pathString
            )
        )
    }

    fun resolveUriRelativeToHomeDir(path: String): URI = File(osConfig.userHomeDir.resolve(path).stringPath()).toURI()

    fun resolveCurrentDir(): URI = File(".").toURI()

    fun tryToCreateShellFilePath(path: String): OsPath? {
        val type = when (osConfig.osType) {
            //Path provided by MSys is like:
            //C:/Workspace/Kod/Repos/kscript/test/resources/multi_line_deps.kts (slashes instead of backslashes)
            //Because of that it should be parsed as Windows path
            OsType.MSYS -> OsType.WINDOWS
            else -> osConfig.osType
        }

        return OsPath.create(type, path)
    }

    fun isReadable(osPath: OsPath): Boolean = File(osPath.toNativeOsPath().stringPath()).canRead()

    private fun resolveUri(osPath: OsPath): URI = File(osPath.toNativeOsPath().stringPath()).toURI()
}
