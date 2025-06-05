package io.github.kscripting.kscript.scripting

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import org.jetbrains.kotlin.scripting.dependencies.DependsOn
import org.jetbrains.kotlin.scripting.dependencies.Repository

// Define Kscript annotations if they are not already globally available
// For now, we assume @DependsOn and @Repository are from org.jetbrains.kotlin.scripting.dependencies
// which should be pulled in by kotlin-scripting-dependencies-maven.

@KotlinScript(
    displayName = "kscript script",
    fileExtension = "kscript.kts", // Or "kts" - to be decided later if we want to override all .kts
    compilationConfiguration = KscriptCompilationConfiguration::class
)
abstract class KscriptBase {
    // Potential common properties or functions for all kscripts can be added here later
}

object KscriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class)
        // Add other kscript-specific default imports if any, e.g.:
        // defaultImports("io.github.kscripting.kscript.annotation.*")

        jvm {
            // Only include essential kscript jars for the script compilation environment itself,
            // not the whole classpath of kscript.
            // This needs careful selection, for now, let's assume it's minimal or empty
            // if the annotations and handlers are self-contained or globally available.
            dependenciesFromCurrentContext(wholeClasspath = false)
        }

        refineConfiguration {
            // Trigger on finding any of these annotations in the script file
            onAnnotations(DependsOn::class, Repository::class, handler = ::resolveKscriptDependencies)
        }

        // TODO: Add other configurations later:
        // - providedProperties (e.g., 'args: Array<String>')
        // - implicitReceivers
        // - etc.
    }
)
