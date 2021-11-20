package kscript.app.creator

import kscript.app.util.ShellUtils.isInPath
import kscript.app.appdir.AppDir
import kscript.app.code.Templates
import kscript.app.model.Script
import kscript.app.util.Logger.infoMsg
import kscript.app.util.ShellUtils.evalBash
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Paths

class PackageCreator(private val appDir: AppDir) {
    /**
     * Create and use a temporary gradle project to package the compiled script using capsule.
     * See https://github.com/puniverse/capsule
     */
    fun packageKscript(
        script: Script, scriptJar: File, wrapperClassName: String, appName: String
    ) {
        if (!isInPath("gradle")) {
            throw IllegalStateException("gradle is required to package kscripts")
        }

        infoMsg("Packaging script '$appName' into standalone executable...")

        val tmpProjectDir = appDir.projectCache.projectDir()

        val jvmOptions = script.kotlinOpts.map { it.value }.filter { it.startsWith("-J") }.map { it.removePrefix("-J") }
            .joinToString(", ") { '"' + it + '"' }

        // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/

        val gradleScript = Templates.createGradlePackageScript(
            script.repositories,
            script.dependencies,
            scriptJar.invariantSeparatorsPath,
            wrapperClassName,
            appName,
            jvmOptions
        )

        val pckgedJar = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile


        // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
        // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
        File(tmpProjectDir, "exec_header.sh").writeText(
            """#!/usr/bin/env bash
            exec java -jar ${'$'}0 "${'$'}@"
            """
        )

        File(tmpProjectDir, "build.gradle").writeText(gradleScript)

        val packageResult = evalBash("cd '${tmpProjectDir}' && gradle simpleCapsule")

        with(packageResult) {
            if (exitCode != 0) {
                throw IllegalStateException("packaging of '$appName' failed:\n$packageResult")
            }
            Unit
        }

        pckgedJar.delete()
        File(tmpProjectDir, "build/libs/${appName}").copyTo(pckgedJar, true).setExecutable(true)

        infoMsg("Finished packaging into $pckgedJar")
    }
}
