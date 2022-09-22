package io.github.kscripting.kscript.resolver

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import io.github.kscripting.kscript.model.Dependency
import io.github.kscripting.kscript.model.Repository
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType
import io.github.kscripting.shell.model.leaf
import io.github.kscripting.shell.model.toNativePath
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString

class DependencyResolverTest {
    private val repositoryPathString =
        Paths.get(System.getProperty("user.home") + "/.m2/repository").toAbsolutePath().invariantSeparatorsPathString

    private val dependencyResolver = DependencyResolver(
        setOf(
            Repository(
                "imagej-releases", "http://maven.imagej.net/content/repositories/releases"
            )
        )
    )

    @ParameterizedTest
    @ValueSource(strings = ["log4j:log4j:1.2.14", "net.clearvolume:cleargl:jar:2.0.1"])
    fun `Resolve dependencies`(dependencyString: String) {
        val dependency = Dependency(dependencyString)
        val calculatedPath = calculateArtifactPath(dependency, true)
        assertThat(dependencyResolver.resolve(setOf(dependency))).contains(calculatedPath)
        assertThat(calculatedPath.toNativePath()).exists()
    }

    @Test
    fun `Non-existing dependency`() {
        assertThat { dependencyResolver.resolve(setOf(Dependency("log4j:log4j:9.8.76"))) }.isFailure().isInstanceOf(
            IllegalStateException::class.java
        )
    }

    @Test
    // prevent regressions on https://github.com/holgerbrandl/kscript/issues/337
    fun `It should resolve a pom dependency into jars`() {
        val resolve = dependencyResolver.resolve(setOf(Dependency("org.javamoney:moneta:pom:1.3")))
        apply {
            assertEquals(6, resolve.size)
            assertThat(resolve.none { it.leaf.endsWith(".pom") })
        }
    }

    private fun calculateArtifactPath(dependency: Dependency, cleanupFirst: Boolean = false): OsPath {
        val parts = dependency.value.split(":")
        require(parts.size == 3 || parts.size == 4)

        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val extension = if (parts.size == 4) parts[2] else "jar"
        val version = parts[parts.size - 1]

        val calculatedPath =
            OsPath.createOrThrow(
                OsType.native,
                "$repositoryPathString/$group/$artifact/$version/$artifact-$version.$extension"
            )

        if (cleanupFirst) {
            val cleanupPath = Paths.get("$repositoryPathString/$group")
            if (cleanupPath.exists()) {
                FileUtils.cleanDirectory(cleanupPath.toFile())
            }
        }

        return calculatedPath
    }
}
