package kscript.app.model

data class Script(
    val location: Location,

    val resolvedCode: String,

    val packageName: PackageName,
    val entryPoint: Entry?,
    val importNames: Set<ImportName>,

    val includes: Set<Include>,
    val dependencies: Set<Dependency>,
    val repositories: Set<Repository>,
    val kotlinOpts: Set<KotlinOpt>,
    val compilerOpts: Set<CompilerOpt>,
    val deprecated: Set<Deprecated>,

    val scriptNodes: Set<ScriptNode>,
    val rootNode: ScriptNode,

    val digest: String
)
