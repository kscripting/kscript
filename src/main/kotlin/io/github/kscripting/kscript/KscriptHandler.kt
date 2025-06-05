package io.github.kscripting.kscript

import io.github.kscripting.kscript.cache.Cache
import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.creator.*
import io.github.kscripting.kscript.model.Config
import io.github.kscripting.kscript.parser.Parser
import io.github.kscripting.kscript.resolver.DependencyResolver
import io.github.kscripting.kscript.resolver.InputOutputResolver
import io.github.kscripting.kscript.resolver.ScriptResolver
import io.github.kscripting.kscript.resolver.SectionResolver
import io.github.kscripting.kscript.scripting.KscriptBase // Your new definition
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils.getArtifactsRecursively
import io.github.kscripting.kscript.util.Logger.info
// import kotlin.script.experimental.api.ScriptConfigurationRefinementContext // May not be needed directly in handler
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptingHost
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.jvm // For createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvm.util.isError // For result checking
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.kscript.util.Logger.warnMsg
import io.github.kscripting.shell.model.ScriptType
import java.net.URI

class KscriptHandler(
    private val executor: Executor, private val config: Config, private val options: Map<String, String>
) {
    fun handle(userArgs: List<String>) {
        val cache = Cache(config.osConfig.cacheDir)

        // optionally clear up the jar cache
        if (options.containsKey("clear-cache")) {
            info("Cleaning up cache...")
            cache.clear()

            if (!options.containsKey("script")) {
                return
            }
        }

        val scriptSource = options["script"] ?: throw IllegalArgumentException("Script argument is required")

        if (scriptSource.isBlank()) {
            return
        }

        val enableSupportApi = options.containsKey("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.scriptingConfig.customPreamble)
        }

        val inputOutputResolver = InputOutputResolver(config.osConfig, cache)
        val sectionResolver = SectionResolver(inputOutputResolver, Parser(), config.scriptingConfig)
        val scriptResolver = ScriptResolver(inputOutputResolver, sectionResolver, config.scriptingConfig)

        if (options.containsKey("add-bootstrap-header")) {
            val script = scriptResolver.resolve(scriptSource, maxResolutionLevel = 0)
            BootstrapCreator().create(script)
            return
        }

        val script = scriptResolver.resolve(scriptSource, preambles)

        if (script.deprecatedItems.isNotEmpty()) {
            if (options.containsKey("report")) {
                info(DeprecatedInfoCreator().create(script.deprecatedItems))
                return
            }

            warnMsg("There are deprecated features in scripts. Use --report option to print full report.")
        }

        // val resolvedDependenciesOld = cache.getOrCreateDependencies(script.digest) {
        // val localArtifacts = if (config.scriptingConfig.artifactsDir != null) {
        // getArtifactsRecursively(config.scriptingConfig.artifactsDir, DependencyResolver.supportedExtensions)
        // } else emptyList()
        //
        // DependencyResolver(script.repositories).resolve(script.dependencies) + localArtifacts
        // }

        // --- NEW DEPENDENCY RESOLUTION, SANDBOX SETUP, COMPILATION & EXECUTION ---
        var sandboxDir: File? = null // Declare here for visibility in finally

        try {
            sandboxDir = Files.createTempDirectory("kscript_sandbox_").toFile()
            val currentSandboxDir = sandboxDir!! // Use a non-null version within the try block

            val libDir = currentSandboxDir.resolve("lib").also { it.mkdirs() }
            // val outDir = currentSandboxDir.resolve("out").also { it.mkdirs() } // JarArtifactCreator creates this

            val m2RepoDependencies: List<File> = try {
                infoMsg("Resolving script dependencies using Kotlin Scripting API...")
                val scriptFile = File(script.scriptLocation.path!!)

                if (!scriptFile.exists()) {
                    throw IllegalStateException("Script file does not exist: ${scriptFile.path}")
                }
                val sourceCode = scriptFile.toScriptSource()
                val baseCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<KscriptBase>()
                val host = BasicJvmScriptingHost()
                val compileResult = host.compiler(sourceCode, baseCompilationConfiguration)

                if (compileResult.isError()) {
                    val errors = compileResult.reports.filter { it.severity == ScriptDiagnostic.Severity.ERROR }
                    throw IllegalStateException("Dependency resolution/script analysis failed:\n" + errors.joinToString("\n") { it.message + (it.exception?.toString()?.let { "\n$it" } ?: "") })
                }

                val refinedConfiguration = compileResult.valueOrThrow().compilationConfiguration
                refinedConfiguration[ScriptCompilationConfiguration.dependencies]
                    ?.flatMap { (it as? JvmDependency)?.classpath ?: emptyList() }
                    ?: emptyList()
            } catch (e: Exception) {
                // No specific cleanup for sandboxDir here as it might not be initialized, or will be caught by outer finally
                throw IllegalStateException("Failed to resolve dependencies using new Kotlin Scripting API: ${e.message}", e)
            }

            if (m2RepoDependencies.isNotEmpty()) {
                infoMsg("Populating sandbox with ${m2RepoDependencies.size} resolved dependencies from local Maven cache...")
                m2RepoDependencies.forEach { m2Jar ->
                    try {
                        val targetFile = libDir.resolve(m2Jar.name)
                        Files.copy(m2Jar.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        info("Copied ${m2Jar.name} to sandbox.")
                    } catch (ioe: Exception) {
                        throw IllegalStateException("Failed to copy dependency ${m2Jar.name} to sandbox: ${ioe.message}", ioe)
                    }
                }
            } else {
                infoMsg("No external dependencies found or resolved for the script.")
            }

            infoMsg("Sandbox populated at: ${currentSandboxDir.absolutePath}")
            infoMsg("Libs in sandbox: ${libDir.listFiles()?.joinToString { it.name } ?: "None"}")

            // --- Create JarArtifact using the refactored JarArtifactCreator ---
            infoMsg("Attempting to create JarArtifact in sandbox...")
            val jarCreator = JarArtifactCreator(executor)
            val jarArtifact: JarArtifact = try {
                jarCreator.create(script, currentSandboxDir, m2RepoDependencies)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create JarArtifact in sandbox: ${e.message}", e)
            }
            infoMsg("JarArtifact created: ${jarArtifact.path}, execClassName: ${jarArtifact.execClassName}")

            // --- Execute script from sandbox ---
            infoMsg("Executing script from sandbox...")
            val sandboxedLibDir = currentSandboxDir.resolve("lib")
            val sandboxedDependenciesOsPaths = sandboxedLibDir.listFiles()
                ?.map { io.github.kscripting.shell.model.OsPath.createOrThrow(it.absolutePath, config.osConfig.osType) }
                ?.toSet()
                ?: emptySet()

            executor.executeKotlin(jarArtifact, sandboxedDependenciesOsPaths, userArgs, script.kotlinOpts)
            infoMsg("Script execution finished.")

        } finally {
            sandboxDir?.let {
                try {
                    it.deleteRecursively() // Using direct extension function for File
                    infoMsg("Sandbox directory ${it.name} cleaned up.")
                } catch (e: Exception) {
                    warnMsg("Failed to cleanup sandbox directory ${it.name}: ${e.message}")
                }
            }
        }

        // The old execution flow, idea integration, packaging, repl are now bypassed.
        // These would need to be made sandbox-aware if they are to be re-enabled.
        // For example, `idea` would need to set up a project pointing to the sandbox.
        // `package` would need to package the contents of the sandbox or the scriplet.jar from it.
        // `interactive` (REPL) would need its classpath configured from the sandbox.

        //  Create temporary dev environment
        if (options.containsKey("idea")) {
            val path = cache.getOrCreateIdeaProject(script.digest) { basePath ->
                val uriLocalPathProvider = { uri: URI -> inputOutputResolver.resolveContent(uri).localPath }
                IdeaProjectCreator(executor).create(basePath, script, userArgs, uriLocalPathProvider)
            }

            infoMsg("Idea project available at:")
            infoMsg(path.convert(config.osConfig.osType).stringPath())
            return
        }

        // Even if we just need and support the @file:EntryPoint directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        if (script.entryPoint != null && script.scriptLocation.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@file:EntryPoint directive is just supported for kt class files")
        }

        // All primary execution logic is now within the try-finally block above.
        // The sections below for idea, package, interactive mode are currently
        // NOT sandbox-aware and would operate on the old (now non-existent or incorrect)
        // `jar` and `resolvedDependencies` variables if they were not commented out.
        // These need individual refactoring in future subtasks.

        //  Optionally enter interactive mode
        if (options.containsKey("interactive")) {
            // executor.runInteractiveRepl(jarArtifact, sandboxedDependenciesOsPaths, script.compilerOpts, script.kotlinOpts)
            warnMsg("Interactive mode is not yet sandbox-aware. Bypassing.")
            return
        }

        //if requested try to package the into a standalone binary
        if (options.containsKey("package")) {
            // val path =
            //    cache.getOrCreatePackage(script.digest, script.scriptLocation.scriptName) { basePath, packagePath ->
            //        PackageCreator(executor).packageKscript(basePath, packagePath, script, jarArtifact) // Needs sandbox awareness
            //    }
            warnMsg("Package mode is not yet sandbox-aware. Bypassing.")
            return
        }

        // executor.executeKotlin(jarArtifact, sandboxedDependenciesOsPaths, userArgs, script.kotlinOpts)
        // This line is effectively replaced by the call within the try-finally block.
        // If execution reaches here, it means it bypassed the main sandbox logic, which shouldn't happen
        // for standard script execution.
        infoMsg("Main execution path completed or bypassed (e.g. for --idea, --package).")
    }
}
