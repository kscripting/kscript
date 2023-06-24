package io.github.kscripting.kscript.resolver

import io.github.kscripting.kscript.model.*
import io.github.kscripting.kscript.parser.Parser
import io.github.kscripting.kscript.util.Logger
import io.github.kscripting.kscript.util.UriUtils
import io.github.kscripting.shell.model.ScriptLocation
import io.github.kscripting.shell.model.ScriptSource
import java.net.URI

class SectionResolver(
    private val inputOutputResolver: InputOutputResolver,
    private val parser: Parser,
    private val scriptingConfig: ScriptingConfig,
    private val osConfig: OsConfig,
) {
    fun resolve(
        scriptLocation: ScriptLocation,
        scriptText: String,
        allowLocalReferences: Boolean,
        maxResolutionLevel: Int,
        resolutionContext: ResolutionContext,
    ): List<Section> {
        val sections = parser.parse(scriptLocation, scriptText)
        val resultingSections = mutableListOf<Section>()

        for (section in sections) {
            val resultingScriptAnnotations = mutableListOf<ScriptAnnotation>()

            for (annotation in section.scriptAnnotations) {
                resultingScriptAnnotations += resolveAnnotation(
                    annotation,
                    scriptLocation.sourceContextUri,
                    allowLocalReferences,
                    scriptLocation.level,
                    maxResolutionLevel,
                    resolutionContext,
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
        resolutionContext: ResolutionContext,
    ): List<ScriptAnnotation> {
        val resolvedScriptAnnotations = mutableListOf<ScriptAnnotation>()

        when (scriptAnnotation) {
            is SheBang -> resolvedScriptAnnotations += scriptAnnotation

            is Code -> resolvedScriptAnnotations += scriptAnnotation

            is ScriptNode -> resolvedScriptAnnotations += scriptAnnotation

            is Include -> {
                val uri = resolveIncludeUri(includeContext, scriptAnnotation.value)

                if (currentLevel < maxResolutionLevel && !resolutionContext.uriRegistry.contains(uri)) {
                    resolutionContext.uriRegistry.add(uri)

                    val scriptSource = if (UriUtils.isRegularFile(uri)) ScriptSource.FILE else ScriptSource.HTTP

                    if (scriptSource == ScriptSource.FILE && !allowLocalReferences) {
                        throw IllegalStateException("References to local files from remote scripts are disallowed.")
                    }

                    val content = inputOutputResolver.resolveContent(uri)

                    val scriptLocation = ScriptLocation(
                        currentLevel + 1, scriptSource, content.scriptType, uri, content.contextUri, content.fileName
                    )

                    val newSections = resolve(
                        scriptLocation,
                        content.text,
                        allowLocalReferences && scriptSource == ScriptSource.FILE,
                        maxResolutionLevel,
                        resolutionContext,
                    )

                    val scriptNode = ScriptNode(scriptLocation, newSections)

                    resolutionContext.scriptNodes.add(scriptNode)
                    resolvedScriptAnnotations += scriptNode
                }

                resolutionContext.includes.add(scriptAnnotation)
                resolvedScriptAnnotations += scriptAnnotation
            }

            is PackageName -> {
                if (resolutionContext.packageName == null || resolutionContext.packageLevel > currentLevel) {
                    resolutionContext.packageName = scriptAnnotation
                    resolutionContext.packageLevel = currentLevel
                }
                resolvedScriptAnnotations += scriptAnnotation
            }

            is Entry -> {
                if (resolutionContext.entryPoint == null || resolutionContext.entryLevel > currentLevel) {
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
                val environment = osConfig.environment
                val repository = Repository(
                    id = scriptAnnotation.id,
                    url = resolveRepositoryOption(
                        scriptAnnotation.url,
                        "url",
                        "{{KSCRIPT_REPOSITORY_URL}}",
                        scriptingConfig.providedRepositoryUrl,
                        environment,
                    ),
                    user = resolveRepositoryOption(
                        scriptAnnotation.user,
                        "user",
                        "{{KSCRIPT_REPOSITORY_USER}}",
                        scriptingConfig.providedRepositoryUser,
                        environment,
                    ),
                    password = resolveRepositoryOption(
                        scriptAnnotation.password,
                        "password",
                        "{{KSCRIPT_REPOSITORY_PASSWORD}}",
                        scriptingConfig.providedRepositoryPassword,
                        environment,
                    ),
                )

                resolutionContext.repositories.add(repository)
                resolvedScriptAnnotations += repository
            }

            is DeprecatedItem -> {
                resolutionContext.deprecatedItems.add(scriptAnnotation)
            }
        }

        return resolvedScriptAnnotations
    }

    private fun resolveIncludeUri(includeContext: URI, include: String): URI {
        val result = when {
            include.startsWith("/") -> inputOutputResolver.resolveUriRelativeToRoot(include.substring(1))
            include.startsWith("~/") -> inputOutputResolver.resolveUriRelativeToHomeDir(include.substring(2))
            else -> includeContext.resolve(URI(include.removePrefix("./")))
        }

        return result.normalize()
    }

    private fun resolveRepositoryOption(
        str: String?,
        optionName: String,
        placeholder: String,
        property: String,
        environment: ProcessEnvironment,
    ): String = tryResolveEnvironmentVariable(str, optionName, environment)
        ?.replace(placeholder, property)
        ?: error("Failed to resolve value for option '$optionName'")

    /**
     * This is a variant of [kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver.tryResolveEnvironmentVariable].
     */
    private fun tryResolveEnvironmentVariable(
        str: String?,
        optionName: String,
        environment: ProcessEnvironment,
    ): String? {
        if (str == null) return null
        if (!str.startsWith("$")) return str
        val envName = str.substring(1)
        val envValue: String? = environment[envName]
        if (envValue.isNullOrEmpty()) {
            Logger.errorMsg("Environment variable '$envName' is not defined for option '$optionName'")
            return null
        }
        return envValue
    }
}
