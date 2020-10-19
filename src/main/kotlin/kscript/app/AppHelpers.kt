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


fun info(msg: String) = System.err.println(msg)


fun infoMsg(msg: String) = System.err.println("[kscript] " + msg)


fun warnMsg(msg: String) = System.err.println("[kscript] [WARN] " + msg)


fun errorMsg(msg: String) = System.err.println("[kscript] [ERROR] " + msg)


fun errorIf(value: Boolean, lazyMessage: () -> Any) {
    if (value) {
        errorMsg(lazyMessage().toString())
        quit(1)
    }
}

fun quit(status: Int): Nothing {
    print(if (status == 0) "true" else "false")
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


fun fetchFromURL(scriptURL: String): File {
    val urlHash = md5(scriptURL)
    val scriptText = URL(scriptURL).readText()
    val urlExtension = when {
        scriptURL.endsWith(".kt") -> "kt"
        scriptURL.endsWith(".kts") -> "kts"
        else -> if (scriptText.contains("fun main")) {
            "kt"
        } else {
            "kts"
        }
    }
    val urlCache = File(KSCRIPT_CACHE_DIR, "/url_cache_${urlHash}.$urlExtension")

    if (!urlCache.isFile) {
        urlCache.writeText(scriptText)
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


fun launchIdeaWithKscriptlet(scriptFile: File,
                             userArgs: List<String>,
                             dependencies: List<String>,
                             customRepos: List<MavenRepo>,
                             includeURLs: List<URL>,
                             compilerOpts: String): String {
    val intellijCommand = System.getenv("KSCRIPT_IDEA_COMMAND") ?: "idea"
    val gradleCommand = System.getenv("KSCRIPT_GRADLE_COMMAND") ?: "gradle"

    requireInPath("$intellijCommand", "Could not find '$intellijCommand' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_IDEA_COMMAND' env property")

    infoMsg("Setting up idea project from ${scriptFile}")

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

    File(tmpProjectDir, ".idea/runConfigurations/")
        .run {
            mkdirs()
        }
    File(tmpProjectDir, ".idea/runConfigurations/Main.xml").writeText(
            """
        <component name="ProjectRunConfigurationManager">
        ${runConfig(scriptFile, tmpProjectDir, userArgs)}
        </component>
        """.trimIndent()
    )

    val stringifiedDeps = dependencies.map {
        """
|    implementation("$it")
""".trimMargin()
    }.joinToString("\n")

    fun MavenRepo.stringifiedRepoCredentials(): String{
       return  takeIf { user.isNotBlank() || password.isNotBlank() }?.let {
            """
|        credentials {
|            username = "${it.user}"
|            password = "${it.password}"
|        }
        """
        } ?: ""
    }

    val stringifiedRepos = customRepos.map {
        """
|    maven {
|        url = uri("${it.url}")
         ${it.stringifiedRepoCredentials()}
|    }
    """.trimMargin()
    }.joinToString("\n")

    // We split on space after having joined by space so we have lost some information on how
    // the options where passed. It might cause some issues if some compiler options contain spaces
    // but it's not the case of jvmTarget so we should be fine.
    val opts = compilerOpts.split(" ")
        .filter { it.isNotBlank() }

    var jvmTargetOption: String? = null
    for (i in opts.indices) {
        if (i > 0 && opts[i - 1] == "-jvm-target") {
            jvmTargetOption = opts[i]
        }
    }

    val kotlinOptions = if (jvmTargetOption != null) {
        """
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions { 
        jvmTarget = "$jvmTargetOption"
    }
}
""".trimIndent()
    } else {
        ""
    }

    val gradleScript = """
plugins {
    id("org.jetbrains.kotlin.jvm") version "${KotlinVersion.CURRENT}"
}

repositories {
    mavenLocal()
    jcenter()
$stringifiedRepos
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime")
$stringifiedDeps
}

sourceSets.getByName("main").java.srcDirs("src")
sourceSets.getByName("test").java.srcDirs("src")

$kotlinOptions
    """.trimIndent()

    File(tmpProjectDir, "build.gradle.kts").writeText(gradleScript)

    // also copy/symlink script resource in
    File(tmpProjectDir, "src").run {
        mkdir()

        // https://stackoverflow.com/questions/17926459/creating-a-symbolic-link-with-java
        createSymLink(File(this, scriptFile.name), scriptFile)
        val scriptDir = Paths.get(scriptFile.path).parent

        // also symlink all includes
        includeURLs.distinctBy { it.fileName() }
                .forEach {
                    val symlinkSrcDirAndDestination = when {
                        it.protocol == "file" -> {
                            val includeFile = File(it.toURI())
                            val includeDir = Paths.get(includeFile.path).parent
                            val symlinkRelativePathToScript = File(this, scriptDir.relativize(includeDir).toFile().path)
                            symlinkRelativePathToScript.mkdirs()
                            Pair(symlinkRelativePathToScript, includeFile)
                        }

                        else -> {
                            Pair(this, fetchFromURL(it.toString()))
                        }
                    }
                    createSymLink(File(symlinkSrcDirAndDestination.first, it.fileName()), symlinkSrcDirAndDestination.second)
                }
    }

    val projectPath = tmpProjectDir.absolutePath

    // Create gradle wrapper
    requireInPath("$gradleCommand", "Could not find '$gradleCommand' in your PATH. You must set the command used to launch your intellij as 'KSCRIPT_GRADLE_COMMAND' env property")
    runProcess("$gradleCommand wrapper", wd = tmpProjectDir)

    infoMsg("Project set up at $projectPath")

    return "$intellijCommand \"$projectPath\""
}

private fun runConfig(scriptFile: File, tmpProjectDir: File, userArgs: List<String>): String {
    return if (scriptFile.extension == "kt") {
        """
        <configuration name="${scriptFile.name.substringBeforeLast(".")}" type="JetRunConfigurationType">
            <module name="${tmpProjectDir.name}.main" />
            <option name="VM_PARAMETERS" value="" />
            <option name="PROGRAM_PARAMETERS" value="" />
            <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
            <option name="ALTERNATIVE_JRE_PATH" />
            <option name="PASS_PARENT_ENVS" value="true" />
            <option name="MAIN_CLASS_NAME" value="${scriptFile.name.substringBeforeLast(".").capitalize()}Kt" />
            <option name="WORKING_DIRECTORY" value="" />
            <method v="2">
                <option name="Make" enabled="true" />
            </method>
            </configuration>
            """.trimIndent()
    } else {
        """  
        <configuration default="false" name="Main" type="BashConfigurationType" factoryName="Bash">
            <option name="INTERPRETER_OPTIONS" value="" />
            <option name="INTERPRETER_PATH" value="kscript" />
            <option name="PROJECT_INTERPRETER" value="false" />
            <option name="WORKING_DIRECTORY" value="" />
            <option name="PARENT_ENVS" value="true" />
            <option name="SCRIPT_NAME" value="${'$'}PROJECT_DIR${'$'}/src/${scriptFile.name}" />
            <option name="PARAMETERS" value="${userArgs.joinToString(" ")}" />
            <module name="" />
            <method v="2" />
         </configuration>
         """.trimIndent()
    }
}

private fun URL.fileName() = this.toURI().path.split("/").last()

private fun createSymLink(link: File, target: File) {
    try {
        Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
    } catch (e: IOException) {
        errorMsg("Failed to create symbolic link to script. Copying instead...")
        target.copyTo(link)
    }
}


/**
 * Create and use a temporary gradle project to package the compiled script using capsule.
 * See https://github.com/puniverse/capsule
 */
fun packageKscript(scriptJar: File, wrapperClassName: String, dependencies: List<String>, customRepos: List<MavenRepo>, runtimeOptions: String, appName: String) {
    requireInPath("gradle", "gradle is required to package kscripts")

    infoMsg("Packaging script '$appName' into standalone executable...")


    val tmpProjectDir = KSCRIPT_CACHE_DIR
        .run { File(this, "kscript_tmp_project__${scriptJar.name}_${System.currentTimeMillis()}") }
        .apply { mkdir() }

    val stringifiedDeps = dependencies.map { "    compile \"$it\"" }.joinToString("\n")
    val stringifiedRepos = customRepos.map { "    maven {\n        url '${it.url}'\n    }\n" }.joinToString("\n")

    val jvmOptions = runtimeOptions.split(" ")
        .filter { it.startsWith("-J") }
        .map { it.removePrefix("-J") }
        .map { '"' + it + '"' }
        .joinToString(", ")

    // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/

    val gradleScript = """
plugins {
    id "org.jetbrains.kotlin.jvm" version "${KotlinVersion.CURRENT}"
    id "it.gianluz.capsule" version "1.0.3"
}

repositories {
    mavenLocal()
    jcenter()
$stringifiedRepos
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib"
$stringifiedDeps

    compile group: 'org.jetbrains.kotlin', name: 'kotlin-script-runtime', version: '${KotlinVersion.CURRENT}'

    // https://stackoverflow.com/questions/20700053/how-to-add-local-jar-file-dependency-to-build-gradle-file
    compile files('${scriptJar.invariantSeparatorsPath}')
}

task simpleCapsule(type: FatCapsule){
  applicationClass '$wrapperClassName'

  archiveName '$appName'

  // http://www.capsule.io/user-guide/#really-executable-capsules
  reallyExecutable

  capsuleManifest {
    jvmArgs = [$jvmOptions]
    //args = []
    //systemProperties['java.awt.headless'] = true
  }
}
    """.trimIndent()

    val pckgedJar = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile


    // create exec_header to allow for direction execution (see http://www.capsule.io/user-guide/#really-executable-capsules)
    // from https://github.com/puniverse/capsule/blob/master/capsule-util/src/main/resources/capsule/execheader.sh
    File(tmpProjectDir, "exec_header.sh").writeText("""#!/usr/bin/env bash
exec java -jar ${'$'}0 "${'$'}@"
""")

    File(tmpProjectDir, "build.gradle").writeText(gradleScript)

    val pckgResult = evalBash("cd '${tmpProjectDir}' && gradle simpleCapsule")

    with(pckgResult) {
        kscript.app.errorIf(exitCode != 0) { "packaging of '$appName' failed:\n$pckgResult" }
    }

    pckgedJar.delete()
    File(tmpProjectDir, "build/libs/${appName}").copyTo(pckgedJar, true).setExecutable(true)

    infoMsg("Finished packaging into ${pckgedJar}")
}
