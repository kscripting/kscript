package kscript.app.resolver

import kotlinx.coroutines.runBlocking
import kscript.app.model.Dependency
import kscript.app.model.Repository
import kscript.app.util.Logger.devMsg
import kscript.app.util.Logger.infoMsg
import java.nio.file.Path
import kotlin.collections.set
import kotlin.io.path.extension
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

    fun resolve(depIds: Set<Dependency>): Set<Path> {
        val resolvedDependencies = runBlocking {
            depIds.map {
                infoMsg("Resolving ${it.value}...")
                val start = System.currentTimeMillis()
                val resolved = resolver.resolve(it.value)
                devMsg("Resolved in: ${System.currentTimeMillis() - start}")
                resolved
            }
        }.map {
            it.valueOr {
                throw IllegalStateException("Failed while connecting to the server. Check the connection (http/https, port, proxy, credentials, etc.) of your maven dependency locators. If you suspect this is a bug, you can create an issue on https://github.com/holgerbrandl/kscript\n" + it.reports.joinToString(
                    "\n"
                ) { it.exception?.toString() ?: it.message }, it.reports.find { it.exception != null }?.exception
                )
            }
        }.flatten().map {
            it.toPath()
        }.filter {
            it.extension == "jar"
        }.toSet()

        return resolvedDependencies
    }
}
