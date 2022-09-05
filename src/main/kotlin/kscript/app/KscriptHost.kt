package kscript.app

import kscript.app.model.ScriptNode
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

object KscriptHost {
//    fun evalScript(script: String, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
//        withMainKtsCacheDir(cacheDir?.absolutePath ?: "") {
//            val scriptDefinition = createJvmCompilationConfigurationFromTemplate<MainKtsScript>()
//            val evaluationDefinition = createJvmEvaluationConfigurationFromTemplate<MainKtsScript>()
//
//            Logger.infoMsg(script)
//            BasicJvmScriptingHost().eval(script.toScriptSource(), scriptDefinition, evaluationDefinition)
//        }

    fun evalScript(script: String, scriptLocation: File, args: List<String>, cacheDir: File? = null): ResultWithDiagnostics<EvaluationResult> =
        withMainKtsCacheDir(cacheDir?.absolutePath ?: "") {
            println("Script location: " + scriptLocation.absolutePath.toString())

            val scriptDefinition = createJvmCompilationConfigurationFromTemplate<MainKtsScript>() {
                refineConfiguration {
                    beforeCompiling {//TODO: duplicated code from ScriptDef
                        val scriptFileLocationVariableName = it.compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
                            ?: SCRIPT_FILE_LOCATION_DEFAULT_VARIABLE_NAME

                        ScriptCompilationConfiguration(it.compilationConfiguration) {
                            providedProperties.put(mapOf(scriptFileLocationVariableName to KotlinType(File::class)))
                            scriptFileLocation.put(scriptLocation)
                            scriptFileLocationVariable.put(scriptFileLocationVariableName)
                        }.asSuccess()
                    }
                }
            }

            val evaluationEnv = MainKtsEvaluationConfiguration.with {
                jvm {
                    baseClassLoader(null)
                }
                constructorArgs(args.toTypedArray())

                refineConfigurationBeforeEvaluate {
                    val compilationConfiguration = it.evaluationConfiguration[ScriptEvaluationConfiguration.compilationConfiguration]
                        ?: throw RuntimeException()
//                    val scriptFileLocation = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocation]
//                        ?: return@refineConfigurationBeforeEvaluate it.evaluationConfiguration.asSuccess()
                    val scriptFileLocationVariable = compilationConfiguration[ScriptCompilationConfiguration.scriptFileLocationVariable]
                        ?: return@refineConfigurationBeforeEvaluate it.evaluationConfiguration.asSuccess()

                    val res = it.evaluationConfiguration.with {
                        providedProperties.put(mapOf(scriptFileLocationVariable to scriptLocation))
                    }
                    return@refineConfigurationBeforeEvaluate res.asSuccess()
                }

                enableScriptsInstancesSharing()
            }

            BasicJvmScriptingHost().eval(script.toScriptSource(), scriptDefinition, evaluationEnv)
        }


    fun resolveCodeForNode(scriptNode: ScriptNode): String {
        val sb = StringBuilder()

        for (section in scriptNode.sections) {
            sb.append(section.code).append('\n')
        }

        return sb.toString()
    }

    private fun <T> withMainKtsCacheDir(value: String?, body: () -> T): T {
        val prevCacheDir = System.getProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
        if (value == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
        else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, value)
        try {
            return body()
        } finally {
            if (prevCacheDir == null) System.clearProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY)
            else System.setProperty(COMPILED_SCRIPTS_CACHE_DIR_PROPERTY, prevCacheDir)
        }
    }
}
