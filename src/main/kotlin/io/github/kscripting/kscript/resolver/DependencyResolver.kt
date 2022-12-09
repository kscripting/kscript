package io.github.kscripting.kscript.resolver

import io.github.kscripting.kscript.model.Dependency
import io.github.kscripting.kscript.model.Repository
import io.github.kscripting.kscript.util.Logger.devMsg
import io.github.kscripting.kscript.util.Logger.infoMsg
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.extension
import io.github.kscripting.shell.model.toOsPath
import kotlinx.coroutines.runBlocking
import kotlin.collections.set
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

class DependencyResolver(private val customRepos: Set<Repository>) {
    private val mvnResolver = MavenDependenciesResolver().apply {
        customRepos.map {
            val options = mutableMapOf<String, String>()

            if (it.password.isNotBlank() && it.user.isNotBlank()) {
                options["username"] = it.user
                options["password"] = it.password
            }

            infoMsg("Adding repository: $it")

            //Adding custom repository removes MavenCentral, so it must be re-added below
            addRepository(RepositoryCoordinates(it.url), makeExternalDependenciesResolverOptions(options))
        }

        addRepository(RepositoryCoordinates("https://repo.maven.apache.org/maven2"))
    }

    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), mvnResolver)

    fun resolve(depIds: Set<Dependency>): Set<OsPath> {
        val resolvedDependencies = runBlocking {
            depIds.map {
                infoMsg("Resolving ${it.value}...")
                val start = System.currentTimeMillis()
                val resolved = resolver.resolve(it.value)
                devMsg("Resolved in: ${System.currentTimeMillis() - start}")
                resolved
            }
        }.asSequence().map { result ->
            result.valueOr { failure ->
                val details = failure.reports.joinToString("\n") { scriptDiagnostic ->
                    scriptDiagnostic.exception?.stackTraceToString() ?: scriptDiagnostic.message
                }

                val firstException =
                    failure.reports.find { scriptDiagnostic -> scriptDiagnostic.exception != null }?.exception

                throw IllegalStateException(exceptionMessage + "\n" + details, firstException)
            }
        }.flatten().map {
            it.toOsPath()
        }.filter {
            supportedExtensions.contains(it.extension)
        }.toSet()

        return resolvedDependencies
    }

    companion object {
        val supportedExtensions = listOf("jar", "aar")

        private val exceptionMessage =
            """|Artifact resolution failure. Check the connection (http/https, port, proxy, credentials, etc.) of your
               |maven dependency locators. If you suspect this is a bug, you can create an issue on:
               |https://github.com/kscripting/kscript
               |""".trimMargin()
    }
}
