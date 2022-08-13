package kscript.app.model

import kscript.app.shell.OsPath
import java.net.URI

data class Content(
    val text: String,
    val scriptType: ScriptType,
    val fileName: String,
    val uri: URI, //Real one from Web, not the cached file
    val contextUri: URI, //Real one from Web, not the cached file
    val localPath: OsPath, //Local file path
)
