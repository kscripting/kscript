package io.github.kscripting.kscript.code

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.kscripting.kscript.model.*
import io.github.kscripting.shell.model.ScriptLocation
import io.github.kscripting.shell.model.ScriptSource
import io.github.kscripting.shell.model.ScriptType
import org.junit.jupiter.api.Test
import java.net.URI

class GradleTemplatesTest {

    @Test
    fun `Create Idea script without any Gradle additions`() {
        val scriptLocation = ScriptLocation(0, ScriptSource.HTTP, ScriptType.KT, null, URI("."), "script")
        val script = Script(
            scriptLocation = scriptLocation,
            resolvedCode = "code",
            packageName = PackageName("package"),
            entryPoint = null,
            importNames = setOf(),
            includes = setOf(),
            dependencies = setOf(),
            repositories = setOf(),
            kotlinOpts = setOf(),
            compilerOpts = setOf(),
            scriptNodes = setOf(),
            deprecatedItems = setOf(),
            rootNode = ScriptNode(scriptLocation, listOf()),
            digest = "w4r53453"
        )

        val scriptText = GradleTemplates.createGradleIdeaScript(script)

        assertThat(scriptText).isEqualTo(
            """
            |plugins {
            |    id("org.jetbrains.kotlin.jvm") version "1.7.21"
            |}
            |
            |repositories {
            |    mavenLocal()
            |    mavenCentral()
            |    
            |}
            |
            |dependencies {
            |    implementation("org.jetbrains.kotlin:kotlin-stdlib")
            |    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.21")
            |    implementation("io.github.kscripting:kscript-annotations:1.5.0")
            |}
            |
            |sourceSets.getByName("main").java.srcDirs("src")
            |sourceSets.getByName("test").java.srcDirs("src")
            |
            |
            |""".trimMargin()
        )
    }

    @Test
    fun `Create Idea script with all Gradle additions`() {
        val scriptLocation = ScriptLocation(0, ScriptSource.HTTP, ScriptType.KT, null, URI("."), "script")
        val script = Script(
            scriptLocation = scriptLocation,
            resolvedCode = "code",
            packageName = PackageName("package"),
            entryPoint = null,
            importNames = setOf(),
            includes = setOf(),
            dependencies = setOf(),
            repositories = setOf(
                Repository("id1", "https://url1", "user1", "pass1"),
                Repository("id2", "https://url2", "user2", "pass2")
            ),
            kotlinOpts = setOf(KotlinOpt("-J-Xmx5g"), KotlinOpt("-J-server")),
            compilerOpts = setOf(CompilerOpt("-progressive"), CompilerOpt("-verbose"), CompilerOpt("-jvm-target 1.8")),
            scriptNodes = setOf(),
            deprecatedItems = setOf(),
            rootNode = ScriptNode(scriptLocation, listOf()),
            digest = "w4r53453"
        )

        val scriptText = GradleTemplates.createGradleIdeaScript(script)

        assertThat(scriptText).isEqualTo(
            """
            |plugins {
            |    id("org.jetbrains.kotlin.jvm") version "1.7.21"
            |}
            |
            |repositories {
            |    mavenLocal()
            |    mavenCentral()
            |    maven {
            |        url = uri("https://url1")
            |        isAllowInsecureProtocol = true
            |        credentials {
            |            username = "user1"
            |            password = "pass1"
            |        }
            |    }
            |    maven {
            |        url = uri("https://url2")
            |        isAllowInsecureProtocol = true
            |        credentials {
            |            username = "user2"
            |            password = "pass2"
            |        }
            |    }
            |}
            |
            |dependencies {
            |    implementation("org.jetbrains.kotlin:kotlin-stdlib")
            |    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.21")
            |    implementation("io.github.kscripting:kscript-annotations:1.5.0")
            |}
            |
            |sourceSets.getByName("main").java.srcDirs("src")
            |sourceSets.getByName("test").java.srcDirs("src")
            |
            |tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            |    kotlinOptions {
            |        jvmTarget = "1.8"
            |        freeCompilerArgs = listOf("-progressive", "-verbose")
            |    }
            |}
            |""".trimMargin()
        )
    }
}
