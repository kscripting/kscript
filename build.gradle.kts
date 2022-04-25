import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.32"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

repositories {
    mavenCentral()
}

group = "com.github.holgerbrandl.kscript.launcher"

//val kotlinVersion: String by rootProject.extra
val kotlinVersion: String ="1.4.32"
val launcherClassName: String ="kscript.app.KscriptKt"

dependencies {
    compile("com.offbytwo:docopt:0.6.0.20150202")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:$kotlinVersion")

    compile("org.slf4j:slf4j-nop:1.7.30")

    testImplementation("junit:junit:4.12")
    testImplementation( "io.kotlintest:kotlintest:2.0.7")
    testImplementation(kotlin("script-runtime"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    // set empty string to classifier and version to get predictable jar file name: build/libs/kscript.jar
    archiveFileName.set("kscript.jar")
    doLast {
        copy {
            from(File(projectDir, "src/kscript"))
            into(archiveFile.get().asFile.parentFile)
        }
    }
}

application {
    mainClassName = launcherClassName
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
