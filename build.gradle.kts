import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.time.ZoneOffset
import java.time.ZonedDateTime

val kotlinVersion: String = "1.7.21"

plugins {
    kotlin("jvm") version "1.7.21"
    application
    id("com.adarshr.test-logger") version "3.2.0"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
    signing
    idea
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

group = "io.github.kscripting"
version = "4.2.0"

buildConfig {
    packageName(project.group.toString() + "." + project.name)
    useKotlinOutput()

    val dateTime = ZonedDateTime.now(ZoneOffset.UTC)

    buildConfigField("String", "APP_NAME", "\"${project.name}\"")
    buildConfigField("String", "APP_VERSION", provider { "\"${project.version}\"" })
    buildConfigField(
        "java.time.ZonedDateTime",
        "APP_BUILD_TIME",
        provider { "java.time.ZonedDateTime.parse(\"$dateTime\")" })
    buildConfigField("String", "KOTLIN_VERSION", provider { "\"${kotlinVersion}\"" })
}

sourceSets {
    create("integration") {
        kotlin.srcDir("$projectDir/src/integration/kotlin")
        resources.srcDir("$projectDir/src/integration/resources")

        compileClasspath += main.get().output + test.get().output
        runtimeClasspath += main.get().output + test.get().output
    }
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

configurations {
    get("integrationImplementation").extendsFrom(get("testImplementation"))
    get("integrationRuntimeOnly").extendsFrom(get("testRuntimeOnly"))
}

idea {
    module {
        @Suppress("UnstableApiUsage")
        testSources.from(sourceSets["integration"].kotlin.srcDirs)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.create<Test>("integrationTest") {
    val itags = System.getProperty("includeTags") ?: ""
    val etags = System.getProperty("excludeTags") ?: ""

    useJUnitPlatform {
        if (itags.isNotBlank()) {
            includeTags(itags)
        }

        if (etags.isNotBlank()) {
            excludeTags(etags)
        }
    }

    systemProperty("osType", System.getProperty("osType"))
    systemProperty("projectPath", projectDir.absolutePath)
    systemProperty("shellPath", System.getProperty("shellPath"))

    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    outputs.upToDateWhen { false }
    //mustRunAfter(tasks["test"])
    //dependsOn(tasks["assemble"], tasks["test"])

    doLast {
        println("Include tags: $itags")
        println("Exclude tags: $etags")
    }
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

val createKscriptLayout by tasks.register<Copy>("createKscriptLayout") {
    dependsOn(shadowJar)

    into(layout.projectDirectory)

    from(shadowJar) {
        into("build/kscript/bin")
    }

    from("src/kscript") {
        into("build/kscript/bin")
    }

    from("src/kscript.bat") {
        into("build/kscript/bin")
    }
}

val packageKscriptDistribution by tasks.register<Zip>("packageKscriptDistribution") {
    dependsOn(createKscriptLayout)

    from(layout.buildDirectory.dir("kscript")) {
        into("kscript-${project.version}")
    }

    archiveFileName.set("kscript-${project.version}-bin.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.buildDirectory.dir("kscript-${project.version}"))
}

val shadowJar by tasks.getting(ShadowJar::class) {
    // set empty string to classifier and version to get predictable jar file name: build/libs/kscript.jar
    archiveFileName.set("kscript.jar")
    transform(ComponentsXmlResourceTransformer())
}

application {
    mainClass.set(project.group.toString() + ".KscriptKt")
}

fun adjustVersion(archiveVersion: String): String {
    var newVersion = archiveVersion.toLowerCaseAsciiOnly()

    val temporaryVersion = newVersion.substringBeforeLast(".")

    if (temporaryVersion.endsWith("-RC", true) || temporaryVersion.endsWith("-BETA", true) || temporaryVersion.endsWith("-ALPHA", true) ||
        temporaryVersion.endsWith("-SNAPSHOT", true)
    ) {
        newVersion = temporaryVersion.substringBeforeLast("-") + "-SNAPSHOT"
    }

    return newVersion
}

val jar: Jar by tasks.getting(Jar::class) {
    archiveVersion.set(adjustVersion(archiveVersion.get()))
}

val sourcesJar: Jar by tasks.getting(Jar::class) {
    archiveVersion.set(adjustVersion(archiveVersion.get()))
}

val javadocJar: Jar by tasks.getting(Jar::class) {
    archiveVersion.set(adjustVersion(archiveVersion.get()))
}

val shadowDistTar: Task by tasks.getting {
    enabled = false
}

val shadowDistZip: Task by tasks.getting {
    enabled = false
}

val distTar: Task by tasks.getting {
    enabled = false
}

val distZip: Task by tasks.getting {
    enabled = false
}

val assemble: Task by tasks.getting {
    dependsOn(packageKscriptDistribution)
}

val test: Task by tasks.getting {
    inputs.dir("${project.projectDir}/test/resources")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = project.name
            version = adjustVersion(project.version.toString())

            artifact(jar)
            artifact(javadocJar)
            artifact(sourcesJar)

            pom {
                name.set("kscript")
                description.set("KScript - easy scripting with Kotlin")
                url.set("https://github.com/kscripting/kscript")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("holgerbrandl")
                        name.set("Holger Brandl")
                        email.set("holgerbrandl@gmail.com")
                    }

                    developer {
                        id.set("aartiPl")
                        name.set("Marcin Kuszczak")
                        email.set("aarti@interia.pl")
                    }
                }
                scm {
                    connection.set("scm:git:git://https://github.com/kscripting/kscript.git")
                    developerConnection.set("scm:git:ssh:https://github.com/kscripting/kscript.git")
                    url.set("https://github.com/kscripting/kscript")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            val adjustedVersion = adjustVersion(project.version.toString())
            url = uri(if (adjustedVersion.endsWith("-SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = project.findProperty("sonatype.user") as String? ?: System.getenv("SONATYPE_USER")
                password = project.findProperty("sonatype.password") as String? ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

dependencies {
    //compileOnly(fileTree("libs"))
    implementation("commons-cli:commons-cli:1.5.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven-all:$kotlinVersion")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("com.konghq:unirest-java:3.13.13")

    implementation("net.igsoft:tablevis:0.6.0")
    implementation("io.arrow-kt:arrow-core:1.1.2")

    implementation("io.github.kscripting:shell:0.5.0")

    implementation("org.slf4j:slf4j-nop:2.0.5")

    implementation("org.semver4j:semver4j:4.1.1")


    testImplementation("org.junit.platform:junit-platform-suite-engine:1.9.2")
    testImplementation("org.junit.platform:junit-platform-suite-api:1.9.2")
    testImplementation("org.junit.platform:junit-platform-suite-commons:1.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.mockk:mockk:1.13.2")

    testImplementation(kotlin("script-runtime"))
}
