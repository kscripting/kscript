package kscript.app.resolver

import kscript.app.model.*
import kscript.app.model.Deprecated
import java.net.URI

data class ResolutionContext(
    val uriRegistry: MutableSet<URI> = mutableSetOf(),

    var packageLevel: Int = 0,
    var packageName: PackageName? = null,

    var entryLevel: Int = 0,
    var entryPoint: Entry? = null,

    val scriptNodes: MutableSet<ScriptNode> = mutableSetOf(),
    val includes: MutableSet<Include> = mutableSetOf(),
    val dependencies: MutableSet<Dependency> = mutableSetOf(),
    val repositories: MutableSet<Repository> = mutableSetOf(),
    val kotlinOpts: MutableSet<KotlinOpt> = mutableSetOf(),
    val compilerOpts: MutableSet<CompilerOpt> = mutableSetOf(),
    val importNames: MutableSet<ImportName> = mutableSetOf(),
    val deprecated: MutableSet<Deprecated> = mutableSetOf(),
)
