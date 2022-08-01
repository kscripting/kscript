import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer

val kotlinVersion: String = "1.6.21"

plugins {
    kotlin("jvm") version "1.6.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.adarshr.test-logger") version "3.2.0"
}

repositories {
    mavenCentral()
}

group = "com.github.holgerbrandl.kscript.launcher"

sourceSets {
    create("integration") {
//        test {  //With that idea can understand that 'integration' is test source set and do not complain about test
//        names starting with upper case, but it doesn't compile correctly with it
            java.srcDir("$projectDir/src/integration/kotlin")
            resources.srcDir("$projectDir/src/integration/resources")
            compileClasspath += main.get().output + test.get().output
            runtimeClasspath += main.get().output + test.get().output
        }
//    }
}

configurations {
    get("integrationImplementation").apply { extendsFrom(get("testImplementation")) }
}

tasks.create<Test>("integration") {
    useJUnitPlatform()

    systemProperty("os.type", System.getProperty("os.type"))
    systemProperty("shell.path", System.getProperty("shell.path"))
    systemProperty("project.path", projectDir.absolutePath)

    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    outputs.upToDateWhen { false }
    mustRunAfter(tasks["test"])
    dependsOn(tasks["assemble"])
}

tasks.create<Task>("printIntegrationClasspath") {
    doLast {
        println(sourceSets["integration"].runtimeClasspath.asPath)
    }
}

testlogger {
    showStandardStreams = true
    showFullStackTraces = false
}

tasks.test {
    useJUnitPlatform()
}

val launcherClassName: String = "kscript.app.KscriptKt"

dependencies {
    implementation("com.offbytwo:docopt:0.6.0.20150202")

    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven-all:$kotlinVersion")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.15")

    implementation("net.igsoft:tablevis:0.6.0")
    implementation("io.arrow-kt:arrow-core:1.1.2")

    implementation("org.slf4j:slf4j-nop:1.7.36")


    testImplementation("org.junit.platform:junit-platform-suite-engine:1.8.2")
    testImplementation("org.junit.platform:junit-platform-suite-api:1.8.2")
    testImplementation("org.junit.platform:junit-platform-suite-commons:1.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.mockk:mockk:1.12.4")

    testImplementation(kotlin("script-runtime"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    // set empty string to classifier and version to get predictable jar file name: build/libs/kscript.jar
    archiveFileName.set("kscript.jar")
    transform(ComponentsXmlResourceTransformer())

    doLast {
        copy {
            from(File(projectDir, "src/kscript"))
            from(File(projectDir, "src/kscript.bat"))
            into(archiveFile.get().asFile.parentFile)
        }
    }
}

application {
    mainClass.set(launcherClassName)
}

// Disable standard jar task to avoid building non-shadow jars
val jar: Task by tasks.getting {
    enabled = false
}
// Build shadowJar when
val assemble: Task by tasks.getting {
    dependsOn(shadowJar)
}

val test: Task by tasks.getting {
    inputs.dir("${project.projectDir}/test/resources")
}
