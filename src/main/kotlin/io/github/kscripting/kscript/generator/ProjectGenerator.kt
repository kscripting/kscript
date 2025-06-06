package io.github.kscripting.kscript.generator

import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
package io.github.kscripting.kscript.generator

import io.github.kscripting.kscript.cache.Cache
import io.github.kscripting.kscript.model.Config
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.parser.Parser
import io.github.kscripting.kscript.resolver.InputOutputResolver
import io.github.kscripting.kscript.resolver.ScriptResolver
import io.github.kscripting.kscript.resolver.SectionResolver
import io.github.kscripting.kscript.util.Logger.errorMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
import java.io.File // Keep this for scriptFile parameter if needed by Kscript.kt, but internally use scriptFilePath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths // Added for Paths.get()
import java.util.Locale // Added for titlecase

// Changed signature: scriptFile: File -> scriptFilePath: String, added config: Config
fun exportToGradleProject(scriptFilePathString: String, outputDir: Path, cliOptions: Map<String, String?>, config: Config) {
    // Remove duplicate package declaration that might have been introduced by previous edits
    // package io.github.kscripting.kscript.generator // This line is effectively a comment if it was duplicated

    val scriptFile = File(scriptFilePathString) // Create File object for name and extension access
    infoMsg("Attempting to export script '$scriptFilePathString' to new Gradle project at '$outputDir'...")
    infoMsg("Output directory: ${outputDir.toAbsolutePath()}")

    try {
        // Instantiate Script object
        val cache = Cache(config.osConfig.cacheDir)
        val inputOutputResolver = InputOutputResolver(config.osConfig, cache)
        val sectionResolver = SectionResolver(inputOutputResolver, Parser(), config.scriptingConfig)
        val scriptResolver = ScriptResolver(inputOutputResolver, sectionResolver, config.scriptingConfig)

        // Resolve the script (this reads content, parses annotations, etc.)
        // For project generation, we typically don't want to include external preambles like text mode.
        val script = scriptResolver.resolve(scriptFilePathString, preambles = emptyList())
        infoMsg("Successfully parsed script: ${script.scriptLocation.scriptName}")

        // Ensure output directory exists
        if (Files.exists(outputDir)) {
            infoMsg("Output directory '$outputDir' already exists. Files may be overwritten.")
        } else {
            Files.createDirectories(outputDir)
            infoMsg("Successfully created directory: $outputDir")
        }

        // 1. Determine Project Properties
        val projectName = outputDir.fileName.toString()
        // Try to use script package name for group, fallback to default
        val projectGroup = if (script.packageName.value.isNotBlank()) script.packageName.value else "com.example"
        val projectVersion = "1.0-SNAPSHOT" // Default version

        infoMsg("Project Name: $projectName")
        infoMsg("Project Group: $projectGroup")
        infoMsg("Project Version: $projectVersion")

        // 2. Create standard project directories
        val srcMainKotlinPath = projectGroup.replace(".", "/")
        val srcMainKotlinDir = outputDir.resolve("src/main/kotlin").resolve(srcMainKotlinPath)
        val srcMainResourcesDir = outputDir.resolve("src/main/resources")
        val srcTestKotlinDir = outputDir.resolve("src/test/kotlin").resolve(srcMainKotlinPath)
        val srcTestResourcesDir = outputDir.resolve("src/test/resources")

        Files.createDirectories(srcMainKotlinDir)
        infoMsg("Successfully created directory: $srcMainKotlinDir")
        Files.createDirectories(srcMainResourcesDir)
        infoMsg("Successfully created directory: $srcMainResourcesDir")
        Files.createDirectories(srcTestKotlinDir)
        infoMsg("Successfully created directory: $srcTestKotlinDir")
        Files.createDirectories(srcTestResourcesDir)
        infoMsg("Successfully created directory: $srcTestResourcesDir")

        // 3. Generate settings.gradle.kts
        val settingsFile = outputDir.resolve("settings.gradle.kts")
        val settingsContent = """
            |rootProject.name = "$projectName"
            |""".trimMargin()
        Files.writeString(settingsFile, settingsContent)
        infoMsg("Successfully generated: $settingsFile")

        // 4. Construct build.gradle.kts Content
        val dependenciesString = script.dependencies.joinToString("\n            ") { "implementation(\"${it.value}\")" }
        val customReposString = script.repositories.joinToString("\n            ") { "maven { url = uri(\"${it.value}\") }" }

        // Determine main class name
        val scriptBaseName = scriptFile.nameWithoutExtension.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val mainClassName = script.entryPoint?.value ?: "${projectGroup}.${scriptBaseName}Kt"

        // Kotlin options and compiler options (basic for now)
        // More complex mapping can be done later. For now, just ensure jvmTarget.
        // val joinedCompilerOpts = script.compilerOpts.joinToString(", ") { "\"${it.value}\"" } // Example if needed

        val buildGradleKtsContent = """
        |plugins {
        |    kotlin("jvm") version "${config.scriptingConfig.kotlinVersion}" // Use kscript's current Kotlin version
        |    application
        |}
        |
        |group = "$projectGroup"
        |version = "$projectVersion"
        |
        |repositories {
        |    mavenCentral()
        |    $customReposString
        |}
        |
        |dependencies {
        |    implementation(kotlin("stdlib")) // Or kotlin("stdlib-jdk8") depending on preference
        |    $dependenciesString
        |}
        |
        |application {
        |    mainClass.set("$mainClassName")
        |    // applicationDefaultJvmArgs = listOf(${script.kotlinOpts.joinToString(", ") { "\"${it.value}\"" }}) // Example
        |}
        |
        |tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        |    compilerOptions {
        |        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) // Match kscript's build
        |        // if (listOf($joinedCompilerOpts).isNotEmpty()) {
        |        //    freeCompilerArgs.addAll(listOf($joinedCompilerOpts))
        |        // }
        |    }
        |}
        |
        |java {
        |    toolchain {
        |        languageVersion.set(JavaLanguageVersion.of(21)) // Match kscript's build
        |    }
        |}
        |""".trimMargin()

        // 5. Write build.gradle.kts File
        val buildGradleKtsFile = outputDir.resolve("build.gradle.kts")
        Files.writeString(buildGradleKtsFile, buildGradleKtsContent)
        infoMsg("Successfully generated: $buildGradleKtsFile")

        // TODO (remaining from original list):
        // 7. Generate .gitignore
        // 8. Add Gradle Wrapper (./gradlew files)

        // 6. Place and Transform Script Content
        val targetKtFile = srcMainKotlinDir.resolve("${scriptBaseName}.kt")
        val originalLines = script.resolvedCode.lines()
        val contentLines = mutableListOf<String>()

        // Add package declaration
        if (projectGroup.isNotBlank()) {
            contentLines.add("package $projectGroup")
            contentLines.add("") // Add a blank line after package
        }

        var firstLineSkipped = false
        for (line in originalLines) {
            if (!firstLineSkipped && line.trim().startsWith("#!/")) {
                firstLineSkipped = true
                continue // Skip shebang
            }

            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("@file:DependsOn") ||
                trimmedLine.startsWith("@file:Repository") || // Matches Script.kt model
                trimmedLine.startsWith("@file:KotlinOptions") ||
                trimmedLine.startsWith("@file:CompilerOptions") ||
                trimmedLine.startsWith("@file:EntryPoint")) {
                // Skip kscript-specific file-level annotations
                continue
            }
            contentLines.add(line)
        }

        Files.writeString(targetKtFile, contentLines.joinToString("\n"))
        infoMsg("Successfully transformed and placed script content at: $targetKtFile")

        // 7. Generate .gitignore
        val gitignoreFile = outputDir.resolve(".gitignore")
        // Simplified .gitignore content to avoid issues with complex multiline strings in the tool
        // In a real scenario, this would be more comprehensive.
        val gitignoreContent = """
            |/build/
            |/.gradle/
            |*.iml
            |.idea/
            |*~
            |out/
            """.trimMargin()
        Files.writeString(gitignoreFile, gitignoreContent)
        infoMsg("Successfully generated: $gitignoreFile")

        // 8. Add Gradle Wrapper
        val kscriptProjectRoot = Paths.get(".").toAbsolutePath().normalize()
        val gradlewFileInKscript = kscriptProjectRoot.resolve("gradlew")
        val gradlewBatFileInKscript = kscriptProjectRoot.resolve("gradlew.bat")
        val gradleWrapperDirInKscript = kscriptProjectRoot.resolve("gradle/wrapper")

        val gradlewFileInProject = outputDir.resolve("gradlew")
        val gradlewBatFileInProject = outputDir.resolve("gradlew.bat")
        val gradleWrapperDirInProject = outputDir.resolve("gradle/wrapper")

        if (Files.exists(gradlewFileInKscript)) {
            Files.copy(gradlewFileInKscript, gradlewFileInProject)
            gradlewFileInProject.toFile().setExecutable(true, false) // ownerOnly = false
            infoMsg("Copied gradlew and set executable.")
        } else {
            warnMsg("gradlew script not found in kscript project root, skipping copy.")
        }

        if (Files.exists(gradlewBatFileInKscript)) {
            Files.copy(gradlewBatFileInKscript, gradlewBatFileInProject)
            infoMsg("Copied gradlew.bat.")
        } else {
            warnMsg("gradlew.bat script not found in kscript project root, skipping copy.")
        }

        Files.createDirectories(gradleWrapperDirInProject)
        val wrapperJarInKscript = gradleWrapperDirInKscript.resolve("gradle-wrapper.jar")
        val wrapperPropsInKscript = gradleWrapperDirInKscript.resolve("gradle-wrapper.properties")

        if (Files.exists(wrapperJarInKscript)) {
            Files.copy(wrapperJarInKscript, gradleWrapperDirInProject.resolve("gradle-wrapper.jar"))
            infoMsg("Copied gradle-wrapper.jar.")
        } else {
            warnMsg("gradle-wrapper.jar not found in kscript project, skipping copy. Project will need 'gradle wrapper' task run manually.")
        }

        if (Files.exists(wrapperPropsInKscript)) {
            Files.copy(wrapperPropsInKscript, gradleWrapperDirInProject.resolve("gradle-wrapper.properties"))
            infoMsg("Copied gradle-wrapper.properties.")
        } else {
            warnMsg("gradle-wrapper.properties not found in kscript project, skipping copy. Project will need 'gradle wrapper' task run manually.")
        }

        infoMsg("Project generation for '$projectName' is complete.")

    } catch (e: Exception) {
        errorMsg("Error during project generation for $scriptFilePathString: ${e.message}")
        e.printStackTrace() // For more detailed debugging during development
        // Consider rethrowing or using a specific return type to indicate failure to the caller
    }
}
