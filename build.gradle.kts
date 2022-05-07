import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

val kotlinVersion: String = "1.6.21"

plugins {
    kotlin("jvm") version "1.6.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

group = "com.github.holgerbrandl.kscript.launcher"

tasks.test {
    useJUnitPlatform()

    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<Test> {
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {
            logger.quiet("\nTest class: ${suite.displayName}")
        }

        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            logger.quiet("${String.format("%-60s - %-10s", testDescriptor.name, result.resultType)} ")
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
    })
}

val launcherClassName: String = "kscript.app.KscriptKt"

dependencies {
    implementation("com.offbytwo:docopt:0.6.0.20150202")

    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven-all:$kotlinVersion")

    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.15")

    implementation("org.slf4j:slf4j-nop:1.7.36")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.mockk:mockk:1.12.3")

    testImplementation(kotlin("script-runtime"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    // set empty string to classifier and version to get predictable jar file name: build/libs/kscript.jar
    archiveFileName.set("kscript.jar")
    transform(ComponentsXmlResourceTransformer())

    doLast {
        copy {
            from(File(projectDir, "src/kscript"))
            into(archiveFile.get().asFile.parentFile)
        }
    }
}

application {
    mainClass.set(launcherClassName)
}

// Disable standard jar task to avoid building non-shadow jars
val jar by tasks.getting {
    enabled = false
}
// Build shadowJar when
val assemble by tasks.getting {
    dependsOn(shadowJar)
}

val test by tasks.getting {
    inputs.dir("${project.projectDir}/test/resources")
}
