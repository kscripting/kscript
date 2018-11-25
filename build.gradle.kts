import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.2.70"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

group = "com.github.holgerbrandl.kscript.launcher"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

    compile("com.offbytwo:docopt:0.6.0.20150202")

    compile("com.jcabi:jcabi-aether:0.10.1") {
        exclude("org.hibernate", "hibernate-validator")
        exclude("org.slf4j", "slf4j-api")
        exclude("org.slf4j", "jcl-over-slf4j")
        exclude("org.apache.commons", "commons-lang3")
        exclude("cglib", "cglib")
        exclude("org.kuali.maven.wagons", "maven-s3-wagon")
    }
    compile("org.apache.maven:maven-core:3.0.3")
    compile("org.slf4j:slf4j-nop:1.7.25")

    testCompile("junit:junit:4.12")
    testCompile( "io.kotlintest:kotlintest:2.0.7")
    testImplementation("io.mockk:mockk:1.8.13")
    testCompile(kotlin("script-runtime"))
}

repositories {
    jcenter()
}

val shadowJar by tasks.getting(ShadowJar::class) {
    // set empty string to classifier and version to get predictable jar file name: build/libs/kscript.jar
    archiveName = "kscript.jar"
    doLast {
        copy {
            from(File(projectDir, "src/kscript"))
            into(archivePath.parentFile)
        }
    }
}

// Disable standard jar task to avoid building non-shadow jars
val jar by tasks.getting {
    enabled = false
}
// Build shadowJar when
val assemble by tasks.getting {
    dependsOn(shadowJar)
}