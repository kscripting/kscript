package kscript.app.resolver

import kotlinx.coroutines.runBlocking
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import kscript.app.shell.OsPath
import kscript.app.shell.extension
import kscript.app.shell.toOsPath
import kotlin.collections.set
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

class DependencyResolver(private val customRepos: Set<Repository>) {
    private val mvnResolver = MavenDependenciesResolver().apply {
        addRepository(RepositoryCoordinates("https://repo.maven.apache.org/maven2"))

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
    }

    private val resolver =
        CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver(), mvnResolver)

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
            it.extension == "jar"
        }.toSet()

        return resolvedDependencies
    }

    companion object {
        //@formatter:off
        private const val exceptionMessage =
         "Failed while connecting to the server. Check the connection (http/https, port, proxy, credentials, etc.)" +
         "of your maven dependency locators. If you suspect this is a bug, " +
         "you can create an issue on https://github.com/holgerbrandl/kscript"
        //@formatter:on
    }
}
