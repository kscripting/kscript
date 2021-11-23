package kscript.app.resolver

import kscript.app.appdir.UriCache
import kscript.app.creator.JarArtifact
import kscript.app.model.Config
import kscript.app.model.Dependency
import kscript.app.parser.Parser

import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.createDirectories

internal class CommandResolverTest {
    private val testHome = Paths.get("build/tmp/script_resolver_test")
    private val config = Config.builder().apply { homeDir = testHome.resolve("home") }.build()
    private val uriCache = UriCache(testHome.resolve("cache").createDirectories())
    private val sectionResolver = SectionResolver(Parser(), uriCache, config)
    private val scriptResolver = ScriptResolver(sectionResolver, uriCache)
    private val commandResolver = CommandResolver(Config.builder().build(), scriptResolver.resolve("println(\"Kotlin rocks\")"))

    @Test
    fun executeKotlin() {
        val jarArtifact = JarArtifact(Paths.get("/home/vagrant/test.jar"), "Main_Scriplet")
        val dependencies = setOf(Paths.get("/home/vagrant/deps/test1.jar"), Paths.get("/home/vagrant/deps/test2.jar"))
        println(commandResolver.executeKotlin(jarArtifact, dependencies, emptyList()))
    }
}