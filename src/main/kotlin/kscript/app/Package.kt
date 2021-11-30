package kscript.app

import java.io.File
import java.nio.file.Paths

class KScriptPackager(
    val scriptJar : File,
    val wrapperClassName : String,
    val dependencies : List<String>,
    val customRepos: List<MavenRepo>,
    val runtimeOptions: List<String>,
    val appName: String
) {
    val timestamp = System.currentTimeMillis()
    /**
     * Create and use a temporary gradle project to package the compiled script
     */
    fun makeExecutableJar() {
        val fatJar = packageAndExtract(
            taskName = "shadowJar",
            gradleScript = packagingGradleScript(
                pluginRequire = """id "com.github.johnrengelman.shadow" version "7.0.0"""",
                pluginConfigure = """
                    shadowJar {
                       archiveBaseName.set('shadow')
                       archiveClassifier.set('')
                       archiveVersion.set('')
                    }
                """.trimIndent()
            )
        ).resolve("build/libs/shadow.jar")
        val resultFile = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile
        resultFile.outputStream().use { fos ->
            fos.write("#!/usr/bin/java ${runtimeOptions.joinToString(" ")} -jar\n".toByteArray())
            fatJar.inputStream().use { fis ->
                fis.copyTo(fos)
            }
        }
        resultFile.setExecutable(true)
        infoMsg("Finished packaging into $resultFile")
    }

    fun makeSelfExtractingRuntime() {
        errorIf(getJavaVersion() < 11) { "A runtime of Java 11 or greater is required to create a self-extracting runtime." }
        val tarzip = packageAndExtract(
            taskName = "tarzipImage",
            gradleScript = packagingGradleScript(
                pluginRequire = """id "org.beryx.runtime" version "1.12.7"""",
                pluginConfigure = """
                    runtime {
                        options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
                    }
                    
            
                    task tarzipImage(type: Tar, dependsOn: tasks.getByName("runtime")){
                        archiveFileName = "temparchive.tar.gz"
                        destinationDirectory = file("${"$"}buildDir/bundled")
                        compression = Compression.GZIP
                        from file("${"$"}buildDir/image/")
                    }
                """.trimIndent()
            ),
        ).resolve("build/bundled/temparchive.tar.gz")

        val scriptTempFolder = "/tmp/$appName-$timestamp"
        val resultFile = File(Paths.get("").toAbsolutePath().toFile(), appName).absoluteFile
        resultFile.outputStream().use { fos ->
            fos.write("""
                #!/bin/sh
                if [ ! -d "$scriptTempFolder" ]; then
                    mkdir "$scriptTempFolder"
                    sed -e '1,/^#EOF#${'$'}/d' "${'$'}0" | tar -C "$scriptTempFolder" -kzxf -
                fi
                cd $scriptTempFolder
                exec "./bin/$appName" "$@"
                exit
                #EOF#
            """.trimIndent().toByteArray())
            fos.write("\n".toByteArray())
            tarzip.inputStream().use { fis ->
                fis.copyTo(fos)
            }
        }
        resultFile.setExecutable(true)
        infoMsg("Finished packaging into $resultFile")
    }

    private fun packageAndExtract(
        taskName : String,
        gradleScript : String,
    ) : File {
        ShellUtils.requireInPath("gradle", "gradle is required to package kscripts")
        infoMsg("Packaging script '$appName' into standalone executable...")

        val tempProjectDirectory = KSCRIPT_CACHE_DIR
            .run { File(this, "kscript_tmp_project__${scriptJar.name}_${timestamp}") }
            .apply { mkdir() }

        tempProjectDirectory.resolve("build.gradle").writeText(gradleScript)

        val packageResult = evalBash("cd '${tempProjectDirectory}' && gradle $taskName")

        errorIf(packageResult.exitCode != 0) { "packaging of '$appName' failed:\n$packageResult" }

        return tempProjectDirectory
    }


    /**
     * Generates a gradle script for packaging.
     * Note that this is templated differently for shadow jar vs bundled runtime instead of one script containing both
     * because of interaction effects between the shadow and badass-runtime plugin.
     */
    private fun packagingGradleScript(pluginRequire : String, pluginConfigure : String) : String {
        // https://shekhargulati.com/2015/09/10/gradle-tip-using-gradle-plugin-from-local-maven-repository/
        return """
            plugins {
                id "org.jetbrains.kotlin.jvm" version "${KotlinVersion.CURRENT}"
                id 'application'
                $pluginRequire
            }
            
            repositories {
                mavenLocal()
                mavenCentral()
                ${customRepos.joinToString("\n    ") { "maven { url '${it.url}'}" }}
            }
            
            dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib"
                ${dependencies.joinToString("\n    ") { "implementation \"$it\"" }}
            
                implementation group: 'org.jetbrains.kotlin', name: 'kotlin-script-runtime', version: '${KotlinVersion.CURRENT}'
            
                // https://stackoverflow.com/questions/20700053/how-to-add-local-jar-file-dependency-to-build-gradle-file
                implementation files('${scriptJar.invariantSeparatorsPath}')
            }
            
            application {
                mainClass = '$wrapperClassName'
                applicationName = '$appName'
                applicationDefaultJvmArgs =[${runtimeOptions.joinToString(", "){"'$it'"}}]
            }
            
            $pluginConfigure
        """.trimIndent()
    }
}

