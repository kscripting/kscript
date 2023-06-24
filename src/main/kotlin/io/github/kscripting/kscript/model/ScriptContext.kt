package io.github.kscripting.kscript.model

import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType

data class ScriptContext(
    val osType: OsType,
    val workingDir: OsPath,
    val executorDir: OsPath,
    val scriptLocation: ScriptLocation
)
