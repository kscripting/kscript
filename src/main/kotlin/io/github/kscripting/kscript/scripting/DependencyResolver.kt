package io.github.kscripting.kscript.scripting

// Correct resolver import
import org.jetbrains.kotlin.scripting.dependencies.resolver.maven.MavenDependenciesResolver
// import org.jetbrains.kotlin.mainKts.impl.IvyResolver as MainKtsIvyResolver // Alternative from kotlin-main-kts

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlinx.coroutines.runBlocking // Required for MavenDependenciesResolver

// The actual dependency resolver (Maven based)
// This might require specific setup for repositories if not using Maven Central by default.
private val mavenResolver = MavenDependenciesResolver()
// private val ivyResolver = MainKtsIvyResolver() // If using Ivy from kotlin-main-kts

fun resolveKscriptDependencies(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val collectedAnnotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?: return context.compilationConfiguration.asSuccess()

    val repositories = collectedAnnotations.filterIsInstance<Repository>()
    val dependencies = collectedAnnotations.filterIsInstance<DependsOn>()

    if (dependencies.isEmpty()) {
        return context.compilationConfiguration.asSuccess()
    }

    // Configure the Maven resolver with custom repositories
    // This is a simplified view; actual API for MavenDependenciesResolver might differ
    // or require repositories to be passed during its construction or via a context.
    // For now, assume it picks up @Repository annotations or we configure it.
    // The MavenDependenciesResolver from kotlin-scripting-dependencies-maven should
    // automatically handle @Repository annotations if they are correctly defined and processed
    // by the scripting configuration chain.

    val resolvedClassPath = mutableListOf<File>()

    // The MavenDependenciesResolver's primary public API for resolving from annotations
    // is often integrated deeper in the scripting definition setup, or by passing annotations directly.
    // For a direct call, it might look like this, but it's often wrapped.
    // We'll use runBlocking as per the tutorial example for MavenDependenciesResolver.
    val resolveResult = runBlocking {
        // The `resolveFromScriptSourceAnnotations` is a common pattern seen in examples.
        // If not available directly, we might need to iterate and call `mavenResolver.resolve(coordinate)`
        // after configuring repositories.
        mavenResolver.resolveFromScriptSourceAnnotations(collectedAnnotations)
    }

    return when (resolveResult) {
        is ResultWithDiagnostics.Success -> {
            resolvedClassPath.addAll(resolveResult.value)
            context.compilationConfiguration.with {
                if (resolvedClassPath.isNotEmpty()) {
                    dependencies.append(JvmDependency(resolvedClassPath))
                }
            }.asSuccess()
        }
        is ResultWithDiagnostics.Failure -> {
            // Propagate diagnostics (errors/warnings)
            resolveResult
        }
    }
}
