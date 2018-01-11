package kscript.app

import kscript.app.ShellUtils.requireInPath
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.function.Consumer
import kotlin.system.exitProcess


data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {

    override fun toString(): String {
        return """
            Exit Code   : ${exitCode}Comand      : ${command}
            Stdout      : ${stdout}
            Stderr      : """.trimIndent() + "\n" + stderr
    }
}

fun evalBash(cmd: String, wd: File? = null,
             stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
             stderrConsumer: Consumer<String> = StringBuilderConsumer()): ProcessResult {
    return runProcess("bash", "-c", cmd,
        wd = wd, stderrConsumer = stderrConsumer, stdoutConsumer = stdoutConsumer)
}


fun runProcess(cmd: String, wd: File? = null): ProcessResult {
    val parts = cmd.split("\\s".toRegex())
    return runProcess(cmd = *parts.toTypedArray(), wd = wd)
}

fun runProcess(vararg cmd: String, wd: File? = null,
               stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
               stderrConsumer: Consumer<String> = StringBuilderConsumer()): ProcessResult {

    try {
        // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        val proc = ProcessBuilder(cmd.asList()).
            directory(wd).
            // see https://youtrack.jetbrains.com/issue/KT-20785
            apply { environment()["KOTLIN_RUNNER"] = "" }.
            start();


        // we need to gobble the streams to prevent that the internal pipes hit their respecitive buffer limits, which
        // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
        // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
        val stdoutGobbler = StreamGobbler(proc.inputStream, stdoutConsumer).apply { start() }
        val stderrGobbler = StreamGobbler(proc.errorStream, stderrConsumer).apply { start() }

        val exitVal = proc.waitFor()

        // we need to wait for the gobbler threads or we may loose some output (e.g. in case of short-lived processes
        stderrGobbler.join()
        stdoutGobbler.join()

        return ProcessResult(cmd.joinToString(" "), exitVal, stdoutConsumer.toString(), stderrConsumer.toString())

    } catch (t: Throwable) {
        throw RuntimeException(t)
    }
}


internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) : Thread() {


    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}

internal open class StringBuilderConsumer : Consumer<String> {
    val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendln(t)
    }

    override fun toString(): String {
        return sb.toString()
    }
}


object ShellUtils {

    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()

    fun requireInPath(tool: String, msg: String = "$tool is not in PATH") = errorIf(!isInPath(tool)) { msg }

}


fun errorMsg(msg: String) {
    System.err.println("[kscript] [ERROR] " + msg)
}


fun errorIf(value: Boolean, lazyMessage: () -> Any) {
    if (value) {
        errorMsg(lazyMessage().toString())
        quit(1)
    }
}

fun quit(status: Int): Nothing {
    exitProcess(status)
}

/** see discussion on https://github.com/holgerbrandl/kscript/issues/15*/
fun guessKotlinHome(): String? {
    return evalBash("KOTLIN_RUNNER=1 JAVACMD=echo kotlinc").stdout.run {
        "kotlin.home=([^\\s]*)".toRegex()
            .find(this)?.groups?.get(1)?.value
    }
}


fun createTmpScript(scriptText: String, extension: String = "kts"): File {
    return File(SCRIPT_TEMP_DIR, "scriptlet.${md5(scriptText)}.$extension").apply {
        writeText(scriptText)
    }
}


fun String.stripShebang(): String = lines().dropWhile { it.startsWith("#!/") }.joinToString("\n")


fun fetchFromURL(scriptURL: String): File? {
    val urlHash = md5(scriptURL)
    val urlCache = File(KSCRIPT_CACHE_DIR, "/url_cache_${urlHash}.kts")

    if (!urlCache.isFile) {
        urlCache.writeText(URL(scriptURL).readText())
    }

    return urlCache
}


fun md5(byteProvider: () -> ByteArray): String {
    // from https://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java
    val md = MessageDigest.getInstance("MD5")
    md.update(byteProvider())

    // disabled to support java9 which dropeed DataTypeConverter
    //    md.update(byteProvider())
    //    val digestInHex = DatatypeConverter.printHexBinary(md.digest()).toLowerCase()

    val digestInHex = bytesToHex(md.digest()).toLowerCase()

    return digestInHex.substring(0, 16)
}

fun md5(msg: String) = md5 { msg.toByteArray() }


fun md5(file: File) = md5 { Files.readAllBytes(Paths.get(file.toURI())) }


// from https://github.com/frontporch/pikitis/blob/master/src/test/kotlin/repacker.tests.kt
private fun bytesToHex(buffer: ByteArray): String {
    val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    val len = buffer.count()
    val result = StringBuffer(len * 2)
    var ix = 0
    while (ix < len) {
        val octet = buffer[ix].toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
        ix++
    }
    return result.toString()
}


fun numLines(str: String) = str.split("\r\n|\r|\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size


fun info(msg: String) = System.err.println(msg)


fun launchIdeaWithKscriptlet(scriptFile: File, dependencies: List<String>, customRepos: List<MavenRepo>): String {
    requireInPath("idea", "Could not find 'idea' in your PATH. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`")

    System.err.println("Setting up idea project from ${scriptFile}")

    //    val tmpProjectDir = createTempDir("edit_kscript", suffix="")
    //            .run { File(this, "kscript_tmp_project") }
    //            .apply { mkdir() }


    //  fixme use tmp instead of cachdir. Fails for now because idea gradle import does not seem to like tmp
    val tmpProjectDir = KSCRIPT_CACHE_DIR
        .run { File(this, "kscript_tmp_project__${scriptFile.name}_${System.currentTimeMillis()}") }
        .apply { mkdir() }
    //    val tmpProjectDir = File("/Users/brandl/Desktop/")
    //            .run { File(this, "kscript_tmp_project") }
    //            .apply { mkdir() }

    val stringifiedDeps = dependencies.map { "    compile \"$it\"" }.joinToString("\n")
    val stringifiedRepos = customRepos.map { "    maven {\n        url '${it.url}'\n    }\n" }.joinToString("\n")

    val gradleScript = """
plugins {
    id "org.jetbrains.kotlin.jvm" version "1.2.10"
}

repositories {
    mavenLocal()
    jcenter()
$stringifiedRepos
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib"
$stringifiedDeps
}

sourceSets.main.java.srcDirs 'src'
    """.trimIndent()

    File(tmpProjectDir, "build.gradle").writeText(gradleScript)

    // also copy/symlink script resource in
    File(tmpProjectDir, "src").run {
        mkdir()

        val tmpProjectScript = File(this, scriptFile.name)

        // https://stackoverflow.com/questions/17926459/creating-a-symbolic-link-with-java
        try {
            Files.createSymbolicLink(tmpProjectScript.toPath(), scriptFile.absoluteFile.toPath());
        } catch (e: IOException) {
            errorMsg("Failed to create symbolic link to script. Copying instead...")
            scriptFile.copyTo(tmpProjectScript)
        }
    }

    return "idea ${tmpProjectDir.absolutePath}"
}