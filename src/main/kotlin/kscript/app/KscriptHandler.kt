package kscript.app

import kscript.app.appdir.Cache
import kscript.app.code.Templates
import kscript.app.creator.*
import kscript.app.model.Config
import kscript.app.model.ScriptType
import kscript.app.parser.Parser
import kscript.app.resolver.*
import kscript.app.util.Executor
import kscript.app.util.Logger
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import org.docopt.DocOptWrapper
import java.net.URI

class KscriptHandler(private val config: Config, private val docopt: DocOptWrapper) {

    fun handle(userArgs: List<String>) {
        Logger.silentMode = docopt.getBoolean("silent")
        Logger.devMode = docopt.getBoolean("development")

        devMsg("KScript configuration:")
        devMsg(config.toString())

        if (Logger.devMode) {
            devMsg("Classpath:")
            devMsg(System.getProperty("java.class.path"))
        }

        val cache = Cache(config.cacheDir)

        // optionally clear up the jar cache
        if (docopt.getBoolean("clear-cache")) {
            Logger.info("Cleaning up cache...")
            cache.clear()
            return
        }

        val enableSupportApi = docopt.getBoolean("text")

        val preambles = buildList {
            if (enableSupportApi) {
                add(Templates.textProcessingPreamble)
            }

            add(config.customPreamble)
        }

        val contentResolver = ContentResolver(cache)
        // see https://github.com/holgerbrandl/kscript/issues/127
//        val fileResolver = FileSystemDependenciesResolver()
        val sectionResolver = SectionResolver(Parser(), contentResolver, config)
        val scriptResolver = ScriptResolver(sectionResolver, contentResolver, config.kotlinOptsEnvVariable)

        if (docopt.getBoolean("add-bootstrap-header")) {
            val script = scriptResolver.resolve(docopt.getString("script"), maxResolutionLevel = 0)
            BootstrapCreator().create(script)
            return
        }

        val script = scriptResolver.resolve(docopt.getString("script"), preambles)
        val resolvedDependencies = cache.getOrCreateDependencies(script.digest) {
            DependencyResolver(script.repositories).resolve(script.dependencies)
        }
        val executor = Executor(CommandResolver(config, script), config)

        //  Create temporary dev environment
        if (docopt.getBoolean("idea")) {
            val path = cache.getOrCreateIdeaProject(script.digest) { basePath ->
                val uriLocalPathProvider = { uri: URI -> contentResolver.resolve(uri).localPath }
                IdeaProjectCreator().create(basePath, script, userArgs, uriLocalPathProvider)
            }

            infoMsg("Project set up at $path")
            executor.runIdea(path)
            return
        }

        //  Optionally enter interactive mode
        if (docopt.getBoolean("interactive")) {
            executor.runInteractiveRepl(resolvedDependencies)
            return
        }

        // Even if we just need and support the //ENTRY directive in case of kt-class
        // files, we extract it here to fail if it was used in kts files.
        if (script.entryPoint != null && script.scriptType == ScriptType.KTS) {
            throw IllegalStateException("@Entry directive is just supported for kt class files")
        }

        val jar = cache.getOrCreateJar(script.digest) { basePath ->
            JarArtifactCreator(executor).create(basePath, script, resolvedDependencies)
        }

        //if requested try to package the into a standalone binary
        if (docopt.getBoolean("package")) {
            val path = cache.getOrCreatePackage(script.digest) { basePath ->
                PackageCreator(executor).packageKscript(basePath, script, jar)
            }

            infoMsg("Package created in: $path")
            return
        }

        executor.executeKotlin(jar, resolvedDependencies, userArgs)
    }
}
