package io.github.kscripting.kscript.code

import io.github.kscripting.kscript.creator.JarArtifact
import io.github.kscripting.kscript.model.CompilerOpt
import io.github.kscripting.kscript.model.Dependency
import io.github.kscripting.kscript.model.Repository
import io.github.kscripting.kscript.model.Script
import io.github.kscripting.kscript.util.Logger.errorMsg
import kotlin.onFailure
import kotlin.script.experimental.api.*

object GradleTemplates {
    fun createGradleIdeaScript(script: Script): String {
        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion"),
            Dependency("io.github.kscripting:kscript-annotations:1.5.0"),
        ) + script.dependencies

        return """
            |plugins {
            |    id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            |}
            |
            |repositories {
            |    mavenLocal()
            |    mavenCentral()
            |${createGradleRepositoriesSection(script.repositories).prependIndent()}
            |}
            |
            |dependencies {
            |${createGradleDependenciesSection(extendedDependencies).prependIndent()}
            |}
            |
            |sourceSets.getByName("main").java.srcDirs("src")
            |sourceSets.getByName("test").java.srcDirs("src")
            |
            |${createCompilerOptionsSection(script.compilerOpts)}
            |""".trimMargin()
    }

    fun createGradlePackageScript(script: Script, jarArtifact: JarArtifact): String {
        val kotlinOptions = createCompilerOptionsSection(script.compilerOpts)

        val kotlinVersion = KotlinVersion.CURRENT
        val extendedDependencies = setOf(
            Dependency("org.jetbrains.kotlin:kotlin-stdlib"),
            Dependency("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
        ) + script.dependencies

        val capsuleApp = jarArtifact.execClassName
        val baseName = script.scriptLocation.scriptName

        return """
            |import java.io.*
            |import java.lang.System
            |import java.nio.file.Files
            |import java.nio.file.Paths
            |
            |plugins {
            |    id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
            |    application
            |}
            |
            |repositories {
            |${createGradleRepositoriesSection(script.repositories).prependIndent()}
            |    mavenCentral()
            |    mavenLocal()
            |}
            |
            |tasks.jar {
            |    manifest {
            |        attributes["Main-Class"] = "$capsuleApp"
            |    }
            |    archiveBaseName.set("$baseName")
            |    configurations["compileClasspath"].forEach { file: File ->
            |        from(zipTree(file.absoluteFile))
            |    }
            |    duplicatesStrategy = DuplicatesStrategy.INCLUDE
            |}
            |
            |tasks.register("makeScript") {
            |    dependsOn(":jar")
            |    doLast {
            |        val headerDir = layout.projectDirectory.toString()
            |        val jarFileName = layout.buildDirectory.file("libs/$baseName.jar").get().toString()
            |        val outFileName = layout.buildDirectory.file("libs/$baseName").get().toString()
            |        val lineSeparator = System.getProperty("line.separator").encodeToByteArray()
            |        val headerPath = Paths.get(headerDir).resolve("exec_header.sh")
            |        val headerBytes = Files.readAllBytes(headerPath)
            |        val jarBytes = Files.readAllBytes(Paths.get(jarFileName))
            |        val outFile = Paths.get(outFileName).toFile()
            |        val fileStream = FileOutputStream(outFile)
            |
            |        fileStream.write(headerBytes)
            |        fileStream.write(lineSeparator)
            |        fileStream.write(jarBytes)
            |        fileStream.close()
            |    }
            |}
            |
            |dependencies {
            |    implementation(files("${jarArtifact.path.stringPath().replace("\\", "\\\\")}"))
            |${createGradleDependenciesSection(extendedDependencies).prependIndent()}
            |}
            |
            |$kotlinOptions
            """.trimStart().trimMargin()
    }

    private fun createGradleRepositoryCredentials(repository: Repository): String {
        if (repository.user.isNotBlank() && repository.password.isNotBlank()) {
            fun getFinalValue(optionName: String, rawValue: String?): String? =
                tryResolveEnvironmentVariable(rawValue, optionName)

            val username = getFinalValue("username", repository.user)
            val password = getFinalValue("password", repository.password)

            return """|credentials {
                      |    username = "$username"
                      |    password = "$password"
                      |}""".trimMargin()
        }

        return ""
    }

    private fun createGradleDependenciesSection(dependencies: Set<Dependency>) = dependencies.joinToString("\n") {
        "implementation(\"${it.value}\")"
    }

    private fun createGradleRepositoriesSection(repositories: Set<Repository>) = repositories.joinToString("\n") {
        """|maven {
           |    url = uri("${it.url}")
           |    isAllowInsecureProtocol = true
           |${createGradleRepositoryCredentials(it).prependIndent()}
           |}
        """.trimMargin()
    }

    private fun createCompilerOptionsSection(compilerOpts: Set<CompilerOpt>): String {
        if (compilerOpts.isEmpty()) {
            return ""
        }

        var jvmTarget = ""
        val freeCompilerArgs = mutableListOf<String>()

        for (opt in compilerOpts) {
            when {
                opt.value.startsWith("-jvm-target") -> {
                    jvmTarget = "jvmTarget = \"" + opt.value.drop(11).trim() + "\""
                }

                else -> {
                    freeCompilerArgs.add(opt.value)
                }
            }
        }

        return """|tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                  |    kotlinOptions {
                  |        $jvmTarget
                  |        freeCompilerArgs = listOf(${freeCompilerArgs.joinToString(", ") { "\"$it\"" }})
                  |    }
                  |}""".trimMargin()
    }

    /**
     * This is a variant of [kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver.tryResolveEnvironmentVariable].
     */
    private fun tryResolveEnvironmentVariable(
        str: String?,
        optionName: String,
    ): String? {
        if (str == null) return null
        if (!str.startsWith("$")) return str
        val envName = str.substring(1)
        val envValue: String? = System.getenv(envName)
        if (envValue.isNullOrEmpty()) {
            errorMsg("Environment variable '$envName' is not defined for option '$optionName'")
            return null
        }
        return envValue
    }
}
