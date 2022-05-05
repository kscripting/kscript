@file:Repository("https://repo.maven.apache.org/maven2")

@file:DependsOn("org.jetbrains.kotlin:kotlin-scripting-jvm:1.6.20")
@file:DependsOn("org.jetbrains.kotlin:kotlin-scripting-dependencies:1.6.20")
@file:DependsOn("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:1.6.20")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
//@file:DependsOn("org.jetbrains.kotlin:kotlin-scripting-jvm")

import java.io.File
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlinx.coroutines.runBlocking

val cachedM2 = File(System.getProperty("user.home"), ".m2/repository/com/beust")

if (cachedM2.isDirectory) {
    System.err.println("Cleaning up cached .m2 copy of klaxon")
    cachedM2.deleteRecursively()
}

val mvnResolver = MavenDependenciesResolver().apply {
    addRepository(RepositoryCoordinates("https://repo.maven.apache.org/maven2"))
}

val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), mvnResolver)

runBlocking {
    val resolve = resolver.resolve("com.beust:klaxon:5.5")

    println(resolve.valueOrNull())

    require(File(System.getProperty("user.home"), ".m2/repository/com/beust/klaxon/5.5/klaxon-5.5.jar").exists()) {
        "failed to resolve dependency"
    }
}
