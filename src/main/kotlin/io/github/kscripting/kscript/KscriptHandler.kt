package io.github.kscripting.kscript

import io.github.kscripting.kscript.cache.Cache
import io.github.kscripting.kscript.code.Templates
import io.github.kscripting.kscript.creator.*
import io.github.kscripting.kscript.model.Config
import io.github.kscripting.kscript.model.ScriptType
import io.github.kscripting.kscript.parser.Parser
import io.github.kscripting.kscript.resolver.*
import io.github.kscripting.kscript.shell.Executor
import io.github.kscripting.kscript.util.Logger
import io.github.kscripting.kscript.util.Logger.info
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.kscript.util.Logger.warnMsg
import java.net.URI

class KscriptHandler(private val config: Config, private val options: Map<String, String>) {
    fun handle(kscriptArgs: List<String>, userArgs: List<String>) {
        Logger.silentMode = options.getBoolean("silent")
        Logger.devMode = options.getBoolean("development")

        if (Logger.devMode) {
            info(DebugInfoCreator().create(config, kscriptArgs, userArgs))
        }

        val cache = Cache(config.osConfig.cacheDir)

        // optionally clear up the jar cache
        if (options.getBoolean("clear-cache")) {
            info("Cleaning up cache...")
            cache.clear()
            return
        }

        val scriptSource = options.getString("script")

        if (scriptSource.isBlank()) {
            return
        }

        val enableSupportApi = options.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.scriptingConfig.customPreamble)
        }

        val inputOutputResolver = InputOutputResolver(config.osConfig, cache)
        val sectionResolver = SectionResolver(inputOutputResolver, Parser(), config.scriptingConfig)
        val scriptResolver = ScriptResolver(inputOutputResolver, sectionResolver, config.scriptingConfig)

        if (options.getBoolean("add-bootstrap-header")) {
            val script = scriptResolver.resolve(scriptSource, maxResolutionLevel = 0)
            BootstrapCreator().create(script)
            return
        }

        val script = scriptResolver.resolve(scriptSource, preambles)

        if (script.deprecatedItems.isNotEmpty()) {
            if (options.getBoolean("report")) {
                info(DeprecatedInfoCreator().create(script.deprecatedItems))
            } else {
                warnMsg("There are deprecated features in scripts. Use --report option to print full report.")
            }
        }

        val resolvedDependencies = cache.getOrCreateDependencies(script.digest) {
            DependencyResolver(script.repositories).resolve(script.dependencies)
        }
        val executor = Executor(CommandResolver(config.osConfig), config.osConfig)

        //  Create temporary dev environment
        if (options.getBoolean("idea")) {
            val path = cache.getOrCreateIdeaProject(script.digest) { basePath ->
                val uriLocalPathProvider = { uri: URI -> inputOutputResolver.resolveContent(uri).localPath }
                IdeaProjectCreator().create(basePath, script, userArgs, uriLocalPathProvider)
            }

            infoMsg("Idea project available at:")
            infoMsg(path.convert(config.osConfig.osType).stringPath())

            executor.runIdea(path)
            return
        }

        //  Optionally enter interactive mode
        if (options.getBoolean("interactive")) {
            executor.runInteractiveRepl(resolvedDependencies, script.compilerOpts, script.kotlinOpts)
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

        //if requested try to package the into a standalone binary
        if (options.getBoolean("package")) {
            val path = cache.getOrCreatePackage(script.digest, script.scriptLocation.scriptName) { basePath, packagePath ->
                PackageCreator(executor).packageKscript(basePath, packagePath, script, jar)
            }

            infoMsg("Packaged script '${script.scriptLocation.scriptName}' available at path:")
            infoMsg(path.convert(config.osConfig.osType).stringPath())
            return
        }

        executor.executeKotlin(jar, resolvedDependencies, userArgs, script.kotlinOpts)
    }

    private fun Map<String, String>.getBoolean(key: String): Boolean = getValue(key).toBoolean()
    private fun Map<String, String>.getString(key: String): String = getValue(key)
}
