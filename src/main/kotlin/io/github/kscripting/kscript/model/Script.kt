package io.github.kscripting.kscript.model

import io.github.kscripting.shell.model.ScriptLocation

data class Script(
    val scriptLocation: ScriptLocation,

    val resolvedCode: String,

    val packageName: PackageName,
    val entryPoint: Entry?,
    val importNames: Set<ImportName>,

    val includes: Set<Include>,
    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>,
    val deprecatedItems: Set<DeprecatedItem>,

    val scriptNodes: Set<ScriptNode>,
    val rootNode: ScriptNode,

    val digest: String
)
