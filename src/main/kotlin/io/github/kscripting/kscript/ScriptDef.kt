package io.github.kscripting.kscript

import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223
import kotlin.script.experimental.util.filterByAnnotationType

@Suppress("unused")
@KotlinScript(
    fileExtension = ".kts",
    compilationConfiguration = MainKtsScriptDefinition::class,
    evaluationConfiguration = MainKtsEvaluationConfiguration::class,
)
abstract class MainKtsScript(val args: Array<String>)

const val COMPILED_SCRIPTS_CACHE_DIR_PROPERTY = "kotlin.main.kts.compiled.scripts.cache.dir"
const val COMPILED_SCRIPTS_CACHE_VERSION = 1
const val SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME = "__FILE__"

class MainKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, ScriptFileLocation::class)

        jvm {
//            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kotlin-main-kts", "kotlin-stdlib", "kotlin-reflect", wholeClasspath = true)
//            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kscript", wholeClasspath = true)

            val keyResource = MainKtsScriptDefinition::class.java.name.replace('.', '/') + ".class"
            val thisJarFile = MainKtsScriptDefinition::class.java.classLoader.getResource(keyResource)?.toContainingJarOrNull()
            if (thisJarFile != null) {
                dependenciesFromClassContext(
                    MainKtsScriptDefinition::class,
                    thisJarFile.name
                )
            } else {
                dependenciesFromClassContext(MainKtsScriptDefinition::class, wholeClasspath = true)
            }

        }

        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, CompilerOptions::class, handler = MainKtsConfigurator())
            onAnnotations(ScriptFileLocation::class, handler = ScriptFileLocationCustomConfigurator())
            //beforeCompiling(::configureScriptFileLocationPathVariablesForCompilation)
            beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }

        jsr223 {
            importAllBindings(true)
        }
    }
)

object MainKtsEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        scriptsInstancesSharing(true)
        refineConfigurationBeforeEvaluate(::configureScriptFileLocationPathVariablesForEvaluation)
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
        refineConfigurationBeforeEvaluate(::configureConstructorArgsFromMainArgs)
    }
)

fun configureScriptFileLocationPathVariablesForEvaluation(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val compilationConfiguration = context.evaluationConfiguration[ScriptEvaluationConfiguration.compilationConfiguration]
        ?: throw RuntimeException()
    val scriptFileLocation = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocation]
        ?: return context.evaluationConfiguration.asSuccess()
    val scriptFileLocationVariable = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
        ?: return context.evaluationConfiguration.asSuccess()

    //println("scriptFileLocation before evaluation: ${scriptFileLocation.absolutePath}")

    val res = context.evaluationConfiguration.with {
        providedProperties.put(mapOf(scriptFileLocationVariable to scriptFileLocation))
    }
    return res.asSuccess()
}

fun configureScriptFileLocationPathVariablesForCompilation(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val scriptFile = (context.script as? FileBasedScriptSource)?.file ?: return context.compilationConfiguration.asSuccess()
    val scriptFileLocationVariableName = context.compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
        ?: SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        providedProperties.put(mapOf(scriptFileLocationVariableName to KotlinType(File::class)))
        scriptFileLocation.put(scriptFile)
        scriptFileLocationVariable.put(scriptFileLocationVariableName)
    }.asSuccess()
}

class ScriptFileLocationCustomConfigurator : RefineScriptCompilationConfigurationHandler {

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {

        val scriptLocationVariable = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
            ?.filterByAnnotationType<ScriptFileLocation>()?.firstOrNull()?.annotation?.variable
            ?: return context.compilationConfiguration.asSuccess()

        val compilationConfiguration = ScriptCompilationConfiguration(context.compilationConfiguration) {
            scriptFileLocationVariable.put(scriptLocationVariable)
        }

        return compilationConfiguration.asSuccess()
    }
}

fun configureConstructorArgsFromMainArgs(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val mainArgs = context.evaluationConfiguration[ScriptEvaluationConfiguration.jvm.mainArguments]
    val res = if (context.evaluationConfiguration[ScriptEvaluationConfiguration.constructorArgs] == null && mainArgs != null) {
        context.evaluationConfiguration.with {
            constructorArgs(mainArgs)
        }
    } else context.evaluationConfiguration
    return res.asSuccess()
}

class MainKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> =
        processAnnotations(context)

    fun processAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources = linkedMapOf<String, Pair<File, String>>()
        var hasImportErrors = false

        annotations.filterByAnnotationType<Import>().forEach { scriptAnnotation ->

            scriptAnnotation.annotation.paths.forEach { sourceName ->
                val file = (scriptBaseDir?.resolve(sourceName) ?: File(sourceName)).normalize()
                val keyPath = file.absolutePath
                val prevImport = importedSources.put(keyPath, file to sourceName)
                if (prevImport != null) {
                    diagnostics.add(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError, "Duplicate imports: \"${prevImport.second}\" and \"$sourceName\"",
                            sourcePath = context.script.locationId, location = scriptAnnotation.location?.locationInText
                        )
                    )
                    hasImportErrors = true
                }
            }
        }

        if (hasImportErrors) return ResultWithDiagnostics.Failure(diagnostics)

        val compileOptions = annotations.filterByAnnotationType<CompilerOptions>().flatMap {
            it.annotation.options.toList()
        }

        val resolveResult = try {
            @Suppress("DEPRECATION_ERROR")
            internalScriptingRunSuspend {
                resolver.resolveFromScriptSourceAnnotations(annotations.filter { it.annotation is DependsOn || it.annotation is Repository })
            }
        } catch (e: Throwable) {
            diagnostics.add(e.asDiagnostics(path = context.script.locationId))
            ResultWithDiagnostics.Failure(diagnostics)
        }

        return resolveResult.onSuccess { resolvedClassPath ->
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                updateClasspath(resolvedClassPath)
                if (importedSources.isNotEmpty()) importScripts.append(importedSources.values.map { FileScriptSource(it.first) })
                if (compileOptions.isNotEmpty()) compilerOptions.append(compileOptions)
            }.asSuccess()
        }
    }
}

fun compiledScriptUniqueName(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): String {
    val digestWrapper = MessageDigest.getInstance("SHA-256")

    fun addToDigest(chunk: String) = with(digestWrapper) {
        val chunkBytes = chunk.toByteArray()
        update(chunkBytes.size.toByteArray())
        update(chunkBytes)
    }

    digestWrapper.update(COMPILED_SCRIPTS_CACHE_VERSION.toByteArray())
    addToDigest(script.text)
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            addToDigest(it.key.name)
            addToDigest(it.value.toString())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES).also { it.putInt(this) }.array()

internal fun URL.toContainingJarOrNull(): File? =
    if (protocol == "jar") {
        (openConnection() as? JarURLConnection)?.jarFileURL?.toFileOrNull()
    } else null

internal fun URL.toFileOrNull() =
    try {
        File(toURI())
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: java.net.URISyntaxException) {
        null
    } ?: run {
        if (protocol != "file") null
        else File(file)
    }
