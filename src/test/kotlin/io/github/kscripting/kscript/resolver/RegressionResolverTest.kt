package io.github.kscripting.kscript.resolver

import java.io.File
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

suspend fun main() {

    // first clean up the .m2 cache (partially at least)
    val log4jCached = File(System.getProperty("user.home"), ".m2/repository/log4j/log4j/1.2.14/")
    if(log4jCached.isDirectory) {
        println("cleaning up cached .m2 copy of log4j")
        log4jCached.deleteRecursively()
    }

    val mvnResolver = MavenDependenciesResolver().apply {
        addRepository(RepositoryCoordinates("https://repo.maven.apache.org/maven2"))
    }

    val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), mvnResolver)

    val resolve = resolver.resolve("log4j:log4j:1.2.14")

    println(resolve.valueOrNull())

    require(File(System.getProperty("user.home"), ".m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar").exists()){
        "failed to resolve dependency"
    }
}
