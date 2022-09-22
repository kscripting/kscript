val kotlinVersion: String = "1.7.10"

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("com.adarshr.test-logger") version "3.2.0"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "io.github.kscripting"
version = "4.2.0"

buildConfig {
    packageName(project.group.toString() + "." + project.name)
    useKotlinOutput()

    buildConfigField("String", "APP_NAME", "\"${project.name}\"")
    buildConfigField("String", "APP_VERSION", provider { "\"${project.version}\"" })
    buildConfigField("String", "KOTLIN_VERSION", provider { "\"${kotlinVersion}\"" })
}

sourceSets {
    create("integration") {
//        test {  //With that idea can understand that 'integration' is a test source set and do not complain about test
//        names starting with upper case, but it doesn't compile correctly with it
            java.srcDir("$projectDir/src/integration/kotlin")
            resources.srcDir("$projectDir/src/integration/resources")
            compileClasspath += main.get().output + test.get().output
            runtimeClasspath += main.get().output + test.get().output

//        }
    }
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
}

configurations {
    get("integrationImplementation").apply { extendsFrom(get("testImplementation")) }
}

tasks.create<Test>("integration") {
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
    mustRunAfter(tasks["test"])
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

dependencies {
    //implementation(fileTree("libs"))
    implementation("com.offbytwo:docopt:0.6.0.20150202")

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

    implementation("net.igsoft:tablevis:0.6.0")
    implementation("io.arrow-kt:arrow-core:1.1.2")

    implementation("io.github.kscripting:shell:0.5.0-SNAPSHOT")

    implementation("org.slf4j:slf4j-nop:2.0.0")


    testImplementation("org.junit.platform:junit-platform-suite-engine:1.9.0")
    testImplementation("org.junit.platform:junit-platform-suite-api:1.9.0")
    testImplementation("org.junit.platform:junit-platform-suite-commons:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.mockk:mockk:1.12.4")

    testImplementation(kotlin("script-runtime"))
}

val createKscriptLayout by tasks.register<Copy>("createKscriptLayout") {
    dependsOn(jar)

    into(layout.projectDirectory)

    from(configurations.runtimeClasspath.get()) {
        into("build/kscript/lib")
    }

    from(jar) {
        into("build/kscript/lib")
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

application {
    mainClass.set(project.group.toString() + ".KscriptKt")
}

val jar: Task by tasks.getting {
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
            artifactId = "kscript"
            from(components["java"])

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
            url = uri(if (project.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

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
