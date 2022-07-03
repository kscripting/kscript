package kscript.app

import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.creator.*
import kscript.app.model.Config
import kscript.app.model.ScriptType
import kscript.app.parser.Parser
import kscript.app.resolver.*
import kscript.app.util.Executor
import kscript.app.util.Logger
import kscript.app.util.Logger.info
import kscript.app.util.Logger.infoMsg
import org.docopt.DocOptWrapper
import java.net.URI

class KscriptHandler(private val config: Config, private val docopt: DocOptWrapper) {

    fun handle(kscriptArgs: List<String>, userArgs: List<String>) {
        Logger.silentMode = docopt.getBoolean("silent")
        Logger.devMode = docopt.getBoolean("development")

        if (Logger.devMode) {
            info(DebugInfoCreator().create(config, kscriptArgs, userArgs))
        }

        // create kscript dir if it does not yet exist
        val appDir = AppDir(config.osConfig.kscriptConfigDir)

        // optionally clear up the jar cache
        if (docopt.getBoolean("clear-cache")) {
            info("Cleaning up cache...")
            appDir.clearCache()
            return
        }

        val enableSupportApi = docopt.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.scriptingConfig.customPreamble)
        }

        val inputOutputResolver = InputOutputResolver(config.osConfig, appDir.cache)
        val sectionResolver = SectionResolver(inputOutputResolver, Parser(), config.scriptingConfig)
        val scriptResolver = ScriptResolver(inputOutputResolver, sectionResolver, config.scriptingConfig)

        if (docopt.getBoolean("add-bootstrap-header")) {
            val script = scriptResolver.resolve(docopt.getString("script"), maxResolutionLevel = 0)
            BootstrapCreator().create(script)
            return
        }

        val script = scriptResolver.resolve(docopt.getString("script"), preambles)
        val resolvedDependencies = appDir.cache.getOrCreateDependencies(script.digest) {
            DependencyResolver(script.repositories).resolve(script.dependencies)
        }
        val executor = Executor(CommandResolver(config.osConfig), config.osConfig)

        //  Create temporary dev environment
        if (docopt.getBoolean("idea")) {
            val path = appDir.cache.getOrCreateIdeaProject(script.digest) { basePath ->
                val uriLocalPathProvider = { uri: URI -> inputOutputResolver.resolveContent(uri).localPath }
                IdeaProjectCreator().create(basePath, script, userArgs, uriLocalPathProvider)
            }

            infoMsg("Idea project available at:")
            infoMsg(path.stringPath())

            executor.runIdea(path)
            return
        }

        //  Optionally enter interactive mode
        if (docopt.getBoolean("interactive")) {
            executor.runInteractiveRepl(resolvedDependencies, script.compilerOpts, script.kotlinOpts)
            return
        }

        // Even if we just need and support the //ENTRY directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        if (script.entryPoint != null && script.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@Entry directive is just supported for kt class files")
        }

        val jar = appDir.cache.getOrCreateJar(script.digest) { basePath ->
            JarArtifactCreator(executor).create(basePath, script, resolvedDependencies)
        }

        //if requested try to package the into a standalone binary
        if (docopt.getBoolean("package")) {
            val path = appDir.cache.getOrCreatePackage(script.digest, script.scriptName) { basePath, packagePath ->
                PackageCreator(executor).packageKscript(basePath, packagePath, script, jar)
            }

            infoMsg("Packaged script '${script.scriptName}' available at path:")
            infoMsg(path.stringPath())
            return
        }

        executor.executeKotlin(jar, resolvedDependencies, userArgs, script.kotlinOpts)
    }
}
