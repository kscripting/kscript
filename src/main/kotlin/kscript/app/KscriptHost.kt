package kscript.app

import kscript.app.shell.OsPath
import kscript.app.shell.toNativeFile
import kscript.app.shell.toNativeOsPath
import kscript.app.util.Logger
import java.io.File
import java.nio.file.Files
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class KscriptHost(private val cacheDir: OsPath) {

    private val baseHostConfiguration: ScriptingHostConfiguration = ScriptingHostConfiguration {
        getScriptingClass(JvmGetScriptingClass())

        jvm {
            compilationCache(CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                val jarFile = File(
                    cacheDir.toNativeFile(), compiledScriptUniqueName(
                        script, scriptCompilationConfiguration
                    ) + ".jar"
                )

                Files.createDirectories(jarFile.parentFile.toPath())

                jarFile
            })
        }
    }

    private val compiler: JvmScriptCompiler =
        JvmScriptCompiler(baseHostConfiguration.withDefaultsFrom(defaultJvmScriptingHostConfiguration))

    private val evaluator: ScriptEvaluator = BasicJvmScriptEvaluator()

    fun compile(cacheDir: OsPath, scriptLocation: OsPath, script: String): ResultWithDiagnostics<CompiledScript> =
        withMainKtsCacheDir(cacheDir) {
                val scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<MainKtsScript>(baseHostConfiguration) {
                    refineConfiguration {
                        beforeCompiling {//TODO: duplicated code from ScriptDef
                            val scriptFileLocationVariableName =
                                it.compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
                                    ?: SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME

                            ScriptCompilationConfiguration(it.compilationConfiguration) {
                                providedProperties.put(mapOf(scriptFileLocationVariableName to KotlinType(File::class)))
                                scriptFileLocation.put(scriptLocation.toNativeFile())
                                scriptFileLocationVariable.put(scriptFileLocationVariableName)
                            }.asSuccess()
                        }
                    }
                }

                println("scriptFileLocation (just before compile) :" + scriptCompilationConfiguration[ScriptCompilationConfiguration.scriptFileLocation])

            runInCoroutineContext {
                compiler(script.toScriptSource(), scriptCompilationConfiguration)
            }
        }

    fun evaluate(
        args: List<String>, scriptLocation: OsPath, compiledScript: CompiledScript
    ): ResultWithDiagnostics<EvaluationResult> = withMainKtsCacheDir(cacheDir) {
        runInCoroutineContext {
            val evaluationEnv = MainKtsEvaluationConfiguration.with {
                jvm {
                    baseClassLoader(null)
                }
                constructorArgs(args.toTypedArray())

                refineConfigurationBeforeEvaluate {
                    val compilationConfiguration =
                        it.evaluationConfiguration[ScriptEvaluationConfiguration.compilationConfiguration]
                            ?: throw RuntimeException()
//                    val scriptFileLocation = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocation]
//                        ?: return@refineConfigurationBeforeEvaluate it.evaluationConfiguration.asSuccess()
                    val scriptFileLocationVariable =
                        compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
                            ?: return@refineConfigurationBeforeEvaluate it.evaluationConfiguration.asSuccess()

                    val res = it.evaluationConfiguration.with {
                        providedProperties.put(mapOf(scriptFileLocationVariable to scriptLocation.toNativeFile()))
                    }
                    return@refineConfigurationBeforeEvaluate res.asSuccess()
                }

                enableScriptsInstancesSharing()
            }

            evaluator(compiledScript, evaluationEnv)
        }
    }

    fun <T> handleDiagnostics(resultWithDiagnostics: ResultWithDiagnostics<T>) {
        when (resultWithDiagnostics) {
            is ResultWithDiagnostics.Success -> {
            }

            is ResultWithDiagnostics.Failure -> {
                resultWithDiagnostics.reports.forEach {
                    Logger.infoMsg(it.toString())

                    if (it.severity > ScriptDiagnostic.Severity.DEBUG) {
                        val stackTrace = if (it.exception == null) "" else it.exception!!.stackTraceToString()

                        Logger.errorMsg(it.message + stackTrace)
                    }
                }

                throw IllegalStateException("Execution failed!")
            }
        }
    }

    private fun <T> runInCoroutineContext(block: suspend () -> T): T =
        @Suppress("DEPRECATION_ERROR") (internalScriptingRunSuspend { block() })

    private fun <T> withMainKtsCacheDir(cacheDir: OsPath?, body: () -> T): T {
        val prevCacheDir = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)

        if (cacheDir == null) {
            System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
        } else {
            System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, cacheDir.toNativeOsPath().stringPath())
        }

        try {
            return body()
        } finally {
            if (prevCacheDir == null) {
                System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
            } else {
                System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, prevCacheDir)
            }
        }
    }
}
