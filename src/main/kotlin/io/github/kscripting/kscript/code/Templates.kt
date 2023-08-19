package io.github.kscripting.kscript.code

import io.github.kscripting.kscript.model.KotlinOpt
import io.github.kscripting.kscript.model.PackageName
import io.github.kscripting.kscript.model.ScriptType
import org.intellij.lang.annotations.Language
import java.time.ZonedDateTime

object Templates {
    @Language("sh")
    val bootstrapHeader = """
        |#!/bin/bash
        |
        |//usr/bin/env echo '
        |/**** BOOTSTRAP kscript ****\'>/dev/null
        |command -v kscript >/dev/null 2>&1 || source /dev/stdin <<< "${'$'}(curl -L https://git.io/fpF1K)"
        |exec kscript $0 "$@"
        |\*** IMPORTANT: Any code including imports and annotations must come after this line ***/
        |
        |""".trimStart().trimMargin()

    val textProcessingPreamble = """
        |@file:DependsOn("com.github.holgerbrandl:kscript-support-api:1.2.5")
        |
        |import kscript.text.*
        |val lines = resolveArgFile(args)
        |
        |""".trimStart().trimMargin()

    fun createExecuteHeader(kotlinOpts: Set<KotlinOpt>): String {
        val options = mutableListOf("")

        for (opt in kotlinOpts) {
            val s = opt.value
            if (s.startsWith("-J")) {
                options.add(s.substring(2))
            }
        }

        val opts = options.joinToString(" ").trim()

        return """
            |#!/usr/bin/env bash
            |exec java $opts -jar ${'$'}0 "${'$'}@"
            """.trimStart().trimMargin()
    }

    fun createWrapperForScript(packageName: PackageName, className: String): String {
        val classReference = packageName.value + "." + className

        return """
            |class Main_${className}{
            |    companion object {
            |        @JvmStatic
            |        fun main(args: Array<String>) {
            |            val script = Main_${className}::class.java.classLoader.loadClass("$classReference")
            |            script.getDeclaredConstructor(Array<String>::class.java).newInstance(args);
            |        }
            |    }
            |}""".trimStart().trimMargin()
    }

    fun createRunConfig(rootScriptName: String, rootScriptType: ScriptType, userArgs: List<String>): String {
        val rootFileName = rootScriptName + rootScriptType.extension
        val userArgsString = userArgs.joinToString(" ") { it }

        if (rootScriptType == ScriptType.KT) {
            return """
            |<component name="ProjectRunConfigurationManager">
            |  <configuration default="false" name="$rootFileName" type="JetRunConfigurationType" nameIsGenerated="true">
            |    <module name="idea" />
            |    <option name="MAIN_CLASS_NAME" value="${rootScriptName}Kt" />
            |    <option name="PROGRAM_PARAMETERS" value="$userArgsString" />
            |    <shortenClasspath name="NONE" />
            |    <method v="2">
            |      <option name="Make" enabled="true" />
            |    </method>
            |  </configuration>
            |</component>
            |""".trimStart().trimMargin()
        }

        // This is Kotlin scripting configuration (other possible options: ShConfigurationType (linux), BatchConfigurationType (windows))
        return """
            |<component name="ProjectRunConfigurationManager">
            |  <configuration default="false" name="$rootFileName" type="KotlinStandaloneScriptRunConfigurationType" nameIsGenerated="true">
            |    <module name="idea" />
            |    <option name="PROGRAM_PARAMETERS" value="$userArgsString" />
            |    <shortenClasspath name="NONE" />
            |    <option name="filePath" value="${'$'}PROJECT_DIR${'$'}/src/$rootFileName" />
            |    <method v="2">
            |      <option name="Make" enabled="true" />
            |    </method>
            |  </configuration>
            |</component>
            |""".trimStart().trimMargin()
    }

    fun createTitleInfo(selfName: String) =
        "$selfName - Enhanced scripting support for Kotlin on *nix and Windows based systems.\n\n"

    fun createHeaderInfo() =
        "\nThe <script> can be a script file (*kts), a script URL, - for stdin, a *.kt source file with a main method, or some kotlin code.\n\n"


    fun createUsageInfo(selfName: String) =
        """|Usage:
           |  $selfName [options] <script> [<script_args>]...
           |  $selfName --clear-cache [--development]
           |  $selfName (--help | --version) [--development]""".trimMargin()


    fun createFooterInfo() =
        """|
           |Copyright : 2023 Holger Brandl, Marcin Kuszczak
           |Website   : https://github.com/kscripting/kscript
           |License   : MIT""".trimMargin()

    fun createVersionInfo(
        buildDateTime: ZonedDateTime, version: String, newVersion: String, kotlinVersion: String, jreVersion: String
    ): String =
        """|Version   : $version ${if (newVersion.isNotBlank()) "(new version v$newVersion is available)" else ""}
           |Build     : $buildDateTime
           |Kotlin    : $kotlinVersion
           |Java      : $jreVersion
           |""".trimMargin().trim()
}
