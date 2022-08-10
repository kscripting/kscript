package kscript.app.util

import kscript.app.util.ShellUtils.whitespaceCharsToSymbols
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {
    override fun toString(): String {
        return """|Command     : '${whitespaceCharsToSymbols(command)}'
                  |Exit Code   : $exitCode   
                  |Stdout      : '${whitespaceCharsToSymbols(stdout)}'
                  |Stderr      : '${whitespaceCharsToSymbols(stderr)}'
                  |""".trimMargin()
    }
}

class StreamGobbler(
    private val inputStream: InputStream
) {
    private val stringBuilder = StringBuilder()
    private var thread: Thread? = null

    val output: String
        get() {
            thread?.join()
            return stringBuilder.toString()
        }

    fun start(): StreamGobbler {
        thread = Thread { readInputStreamSequentially() }
        thread!!.start()

        return this
    }

    private fun readInputStreamSequentially() {
        val buffer = ByteArray(1024)
        var length: Int

        while (inputStream.read(buffer).also { length = it } != -1) {
            val readContent = String(buffer, 0, length)
            stringBuilder.append(readContent)
        }
    }
}

object ProcessRunner {
    fun runProcess(vararg cmd: String, wd: OsPath? = null, env: Map<String, String> = emptyMap()): ProcessResult {
        return runProcess(cmd.asList(), wd, env)
    }

    fun runProcess(
        cmd: List<String>,
        wd: OsPath? = null,
        env: Map<String, String> = emptyMap(),
    ): ProcessResult {
        val command = cmd.joinToString(" ")

        try {
            // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
            val proc = ProcessBuilder(cmd).directory(wd?.toNativeFile()).apply {
                prepareMinimalEnvironment(environment(), env)
            }.start()

            // we need to gobble the streams to prevent that the internal pipes hit their respective buffer limits, which
            // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
            // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
            val inputStreamReader = StreamGobbler(proc.inputStream).start()
            val errorStreamReader = StreamGobbler(proc.errorStream).start()

            val waitTimeMinutes = 10L
            val exitedNormally = proc.waitFor(waitTimeMinutes, TimeUnit.MINUTES)

            if (!exitedNormally) {
                throw IllegalStateException("Command has timed out after $waitTimeMinutes minutes.")
            }

            // we need to wait for the gobbler threads, or we may lose some output (e.g. in case of short-lived processes
            return ProcessResult(command, proc.exitValue(), inputStreamReader.output, errorStreamReader.output)
        } catch (e: Exception) {
            throw IllegalStateException("Error executing command: '$command'", e)
        }
    }

    private fun prepareMinimalEnvironment(environment: MutableMap<String, String>, env: Map<String, String>) {
        // see https://youtrack.jetbrains.com/issue/KT-20785
        // on Windows also other env variables (like KOTLIN_OPTS) interfere with executed command, so they have to be cleaned

        //NOTE: It would be better to prepare minimal env only with environment variables that are required,
        //but it means that we should track, what are default env variables in different OSes

        //Env variables set by Unix scripts (from kscript and Kotlin)
        environment.remove("KOTLIN_RUNNER")

        //Env variables set by Windows scripts (from kscript and Kotlin)
        environment.remove("_KOTLIN_RUNNER")
        environment.remove("KOTLIN_OPTS")
        environment.remove("JAVA_OPTS")
        environment.remove("_version")
        environment.remove("_KOTLIN_HOME")
        environment.remove("_BIN_DIR")
        environment.remove("_KOTLIN_COMPILER")
        environment.remove("JAR_PATH")
        environment.remove("COMMAND")
        environment.remove("_java_major_version")
        environment.remove("ABS_KSCRIPT_PATH")

        environment.putAll(env)
    }
}
