package io.github.kscripting.kscript.resolver

import io.github.kscripting.kscript.model.*
import io.github.kscripting.kscript.parser.LineParser.extractValues
import io.github.kscripting.kscript.shell.leaf
import io.github.kscripting.kscript.util.ScriptUtils
import io.github.kscripting.kscript.util.UriUtils
import java.net.URI

class ScriptResolver(
    private val inputOutputResolver: InputOutputResolver,
    private val sectionResolver: SectionResolver,
    private val scriptingConfig: ScriptingConfig
) {
    private val scripletName = "scriplet"

    //level parameter - for how many levels should include be resolved
    //level 0       -   do not resolve includes in base file and any other embedded
    //level 1 to n  -   resolve includes up to respective level (1 is a base script)
    //level Int.Max -   full resolution (default)
    fun resolve(
        string: String, preambles: List<String> = emptyList(), maxResolutionLevel: Int = Int.MAX_VALUE
    ): Script {
        //Is it stdin?
        if (string == "-" || string == "/dev/stdin") {
            // we need to keep track of the scripts dir or the working dir in case of stdin script to correctly resolve includes
            val scriptText = ScriptUtils.prependPreambles(preambles, generateSequence { readLine() }.joinToString("\n"))
            val scriptType = ScriptUtils.resolveScriptType(scriptText)

            val location =
                Location(
                    0,
                    ScriptSource.STD_INPUT,
                    scriptType,
                    null,
                    inputOutputResolver.resolveCurrentDir(),
                    scripletName
                )

            return createScript(
                location, scriptText, true, maxResolutionLevel
            )
        }

        //Is it a URL?
        if (UriUtils.isUrl(string)) {
            val content = inputOutputResolver.resolveContent(URI(string))
            val scriptText = ScriptUtils.prependPreambles(preambles, content.text)

            val location =
                Location(0, ScriptSource.HTTP, content.scriptType, content.uri, content.contextUri, content.fileName)

            return createScript(
                location, scriptText, false, maxResolutionLevel
            )
        }

        val filePath = inputOutputResolver.tryToCreateShellFilePath(string)

        if (filePath != null) {
            val scriptType = ScriptType.findByExtension(filePath.leaf)

            if (inputOutputResolver.isReadable(filePath)) {
                if (scriptType != null) {
                    //Regular file
                    val content = inputOutputResolver.resolveContent(filePath)
                    val scriptText = ScriptUtils.prependPreambles(preambles, content.text)

                    val location =
                        Location(
                            0,
                            ScriptSource.FILE,
                            content.scriptType,
                            content.uri,
                            content.contextUri,
                            content.fileName
                        )

                    return createScript(
                        location, scriptText, true, maxResolutionLevel
                    )
                }

                //If script input is a process substitution file handle we can not use for content reading following methods:
                //FileInputStream(this).bufferedReader().use{ readText() } nor readText()
                val content = inputOutputResolver.resolveContentUsingInputStream(filePath)
                val scriptText = ScriptUtils.prependPreambles(preambles, content.text)

                val location =
                    Location(
                        0,
                        ScriptSource.OTHER_FILE,
                        content.scriptType,
                        content.uri,
                        content.contextUri,
                        scripletName
                    )

                return createScript(
                    location, scriptText, true, maxResolutionLevel
                )
            }

            if (scriptType != null) {
                throw IllegalStateException("Could not read script from '$string'")
            }
        }

        //As a last resort we assume that input is a Kotlin program...
        val scriptText = ScriptUtils.prependPreambles(preambles, string)
        val scriptType = ScriptUtils.resolveScriptType(scriptText)

        val location =
            Location(0, ScriptSource.PARAMETER, scriptType, null, inputOutputResolver.resolveCurrentDir(), scripletName)

        return createScript(
            location,
            scriptText,
            true,
            maxResolutionLevel
        )
    }

    private fun createScript(
        location: Location, scriptText: String, allowLocalReferences: Boolean, maxResolutionLevel: Int
    ): Script {
        val resolutionContext = ResolutionContext()
        val sections =
            sectionResolver.resolve(location, scriptText, allowLocalReferences, maxResolutionLevel, resolutionContext)

        val scriptNode = ScriptNode(location, sections)
        resolutionContext.scriptNodes.add(scriptNode)
        resolutionContext.packageName = resolutionContext.packageName ?: PackageName("kscript.scriplet")

        val code = ScriptUtils.resolveCode(resolutionContext.packageName, resolutionContext.importNames, scriptNode)

        if (scriptingConfig.providedKotlinOpts.isNotBlank()) {
            extractValues(scriptingConfig.providedKotlinOpts).map { KotlinOpt(it) }.forEach {
                resolutionContext.kotlinOpts.add(it)
            }
        }

        val digest = ScriptUtils.calculateHash(code, resolutionContext)

        return Script(
            location,
            code,
            resolutionContext.packageName!!,
            resolutionContext.entryPoint,
            resolutionContext.importNames,
            resolutionContext.includes,
            resolutionContext.dependencies,
            resolutionContext.repositories,
            resolutionContext.kotlinOpts,
            resolutionContext.compilerOpts,
            resolutionContext.deprecatedItems,
            resolutionContext.scriptNodes,
            scriptNode,
            digest
        )
    }
}
