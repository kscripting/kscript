package kscript.integration

import kscript.app.util.ProcessResult
import kscript.app.util.ProcessRunner

object Tools {
    private val shell = listOf("/usr/bin/bash", "-c")
    private val projectDir = "/home/vagrant/workspace/Kod/Repos/kscript"

    fun resolveKscript(): String = "$projectDir/build/libs/kscript"
    fun resolvePath(path: String): String = path
    fun runProcess(command: String): ProcessResult {
        val result = ProcessRunner.runProcess(shell + command)
        println(result)
        return result
    }

    fun resolveProjectDir(): String = projectDir
}
