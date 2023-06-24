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
import io.github.kscripting.kscript.util.Executor
import io.github.kscripting.kscript.util.FileUtils.getArtifactsRecursively
import io.github.kscripting.kscript.util.Logger.info
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
        val sectionResolver = SectionResolver(inputOutputResolver, Parser(), config.scriptingConfig, config.osConfig)
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

        val resolvedDependencies = cache.getOrCreateDependencies(script.digest) {
            val localArtifacts = if (config.scriptingConfig.artifactsDir != null) {
                getArtifactsRecursively(config.scriptingConfig.artifactsDir, DependencyResolver.supportedExtensions)
            } else emptyList()

            DependencyResolver(script.repositories).resolve(script.dependencies) + localArtifacts
        }

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

        val jar = cache.getOrCreateJar(script.digest) { basePath ->
            JarArtifactCreator(executor).create(basePath, script, resolvedDependencies)
        }

        //  Optionally enter interactive mode
        if (options.containsKey("interactive")) {
            executor.runInteractiveRepl(jar, resolvedDependencies, script.compilerOpts, script.kotlinOpts)
            return
        }

        //if requested try to package the into a standalone binary
        if (options.containsKey("package")) {
            val path =
                cache.getOrCreatePackage(script.digest, script.scriptLocation.scriptName) { basePath, packagePath ->
                    PackageCreator(executor).packageKscript(basePath, packagePath, script, jar)
                }

            infoMsg("Packaged script '${script.scriptLocation.scriptName}' available at path:")
            infoMsg(path.convert(config.osConfig.osType).stringPath())
            return
        }

        executor.executeKotlin(jar, resolvedDependencies, userArgs, script.kotlinOpts)
    }
}
