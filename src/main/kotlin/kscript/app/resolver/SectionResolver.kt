package kscript.app.resolver

import kscript.app.model.*
import kscript.app.parser.Parser
import kscript.app.util.OsPath
import kscript.app.util.ScriptUtils
import kscript.app.util.path
import java.io.File
import java.net.URI
import java.nio.file.Path

class SectionResolver(
    private val parser: Parser,
    private val contentResolver: ContentResolver,
    private val config: Config
) {
    fun resolve(
        scriptText: String,
        includeContext: URI,
        allowLocalReferences: Boolean,
        currentLevel: Int,
        maxResolutionLevel: Int,
        resolutionContext: ResolutionContext
    ): List<Section> {
        val sections = parser.parse(scriptText)
        val resultingSections = mutableListOf<Section>()

        for (section in sections) {
            val resultingScriptAnnotations = mutableListOf<ScriptAnnotation>()

            for (annotation in section.scriptAnnotations) {
                resultingScriptAnnotations += resolveAnnotation(
                    annotation,
                    includeContext,
                    allowLocalReferences,
                    currentLevel,
                    maxResolutionLevel,
                    resolutionContext
                )
            }

            resultingSections += Section(section.code, resultingScriptAnnotations)
        }

        return resultingSections
    }

    private fun resolveAnnotation(
        scriptAnnotation: ScriptAnnotation,
        includeContext: URI,
        allowLocalReferences: Boolean,
        currentLevel: Int,
        maxResolutionLevel: Int,
        resolutionContext: ResolutionContext
    ): List<ScriptAnnotation> {
        val resolvedScriptAnnotations = mutableListOf<ScriptAnnotation>()

        when (scriptAnnotation) {
            is SheBang -> resolvedScriptAnnotations += scriptAnnotation

            is Code -> resolvedScriptAnnotations += scriptAnnotation

            is ScriptNode -> resolvedScriptAnnotations += scriptAnnotation

            is Include -> {
                val uri = resolveIncludeUri(includeContext, scriptAnnotation.value, config.homeDir)

                if (currentLevel < maxResolutionLevel && !resolutionContext.uriRegistry.contains(uri)) {
                    resolutionContext.uriRegistry.add(uri)

                    val scriptSource = if (ScriptUtils.isRegularFile(uri)) ScriptSource.FILE else ScriptSource.HTTP

                    if (scriptSource == ScriptSource.FILE && !allowLocalReferences) {
                        throw IllegalStateException("References to local files from remote scripts are disallowed.")
                    }

                    val content = contentResolver.resolve(uri)

                    val newSections = resolve(
                        content.text,
                        content.contextUri,
                        allowLocalReferences && scriptSource == ScriptSource.FILE,
                        currentLevel + 1,
                        maxResolutionLevel,
                        resolutionContext
                    )

                    val scriptNode = ScriptNode(
                        currentLevel + 1,
                        scriptSource,
                        content.scriptType,
                        uri,
                        content.contextUri,
                        ScriptUtils.extractFileName(uri),
                        newSections
                    )

                    resolutionContext.scriptNodes.add(scriptNode)
                    resolvedScriptAnnotations += scriptNode
                }

                resolutionContext.includes.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is PackageName -> {
                if (resolutionContext.packageName == null || (resolutionContext.packageName != null && resolutionContext.packageLevel > currentLevel)) {
                    resolutionContext.packageName = scriptAnnotation
                    resolutionContext.packageLevel = currentLevel
                }
                resolvedScriptAnnotations += scriptAnnotation
            }

            is Entry -> {
                if (resolutionContext.entryPoint == null || (resolutionContext.entryPoint != null && resolutionContext.entryLevel > currentLevel)) {
                    resolutionContext.entryPoint = scriptAnnotation
                    resolutionContext.entryLevel = currentLevel
                }
                resolvedScriptAnnotations += scriptAnnotation
            }

            is ImportName -> {
                resolutionContext.importNames.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is Dependency -> {
                resolutionContext.dependencies.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is KotlinOpt -> {
                resolutionContext.kotlinOpts.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is CompilerOpt -> {
                resolutionContext.compilerOpts.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is Repository -> {
                val repository = Repository(
                    scriptAnnotation.id,
                    scriptAnnotation.url.replace("{{KSCRIPT_REPOSITORY_URL}}", config.repositoryUrlEnvVariable),
                    scriptAnnotation.user.replace("{{KSCRIPT_REPOSITORY_USER}}", config.repositoryUserEnvVariable),
                    scriptAnnotation.password.replace(
                        "{{KSCRIPT_REPOSITORY_PASSWORD}}", config.repositoryPasswordEnvVariable
                    )
                )

                resolutionContext.repositories.add(repository)
                resolvedScriptAnnotations += repository
            }
        }

        return resolvedScriptAnnotations
    }

    private fun resolveIncludeUri(includeContext: URI, include: String, homeDir: OsPath): URI {
        val result = when {
            include.startsWith("/") -> File(include).toURI()
            include.startsWith("~/") -> File(homeDir.path().toAbsolutePath().toString() + include.substring(1)).toURI()
            else -> includeContext.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }
}
