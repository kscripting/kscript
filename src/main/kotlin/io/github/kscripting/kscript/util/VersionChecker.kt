package io.github.kscripting.kscript.util

import io.github.kscripting.kscript.BuildConfig
import kong.unirest.Unirest
import org.semver4j.Semver

class VersionChecker(private val executor: Executor) {
    val localKscriptVersion: String = BuildConfig.APP_VERSION
    val remoteKscriptVersion: String by lazy { parseRemoteKscriptVersion(retrieveRemoteKscriptVersion()) }

    val localKotlinVersion: String by lazy { kotlinInfo.first }
    val localJreVersion: String by lazy { kotlinInfo.second }

    fun isThereANewKscriptVersion(): Boolean = remoteKscriptVersion != "-" &&
            Semver(localKscriptVersion).isLowerThan(remoteKscriptVersion)

    internal val kotlinInfo: Pair<String, String> by lazy {
        parseLocalKotlinAndJreVersion(executor.retrieveLocalKotlinAndJreVersion())
    }

    internal fun parseRemoteKscriptVersion(githubResponse: String): String {
        return githubResponse.substringAfter("\"tag_name\":\"v", "").substringBefore("\"", "").trim().ifBlank { "-" }
    }

    internal fun retrieveRemoteKscriptVersion(): String {
        //https://api.github.com/repos/kscripting/kscript/releases/latest
        // "tag_name":"v4.1.1",

        val body = try {
            Unirest.config()
                .socketTimeout(1000)
                .connectTimeout(2000)

            Unirest.get("https://api.github.com/repos/kscripting/kscript/releases/latest")
                .header("Accept", "application/vnd.github+json").asString().body
        } catch (e: Exception) {
            ""
        } finally {
            Unirest.shutDown()
        }

        return body
    }

    internal fun parseLocalKotlinAndJreVersion(processOutput: String): Pair<String, String> {
        val kotlinAndJreVersion = processOutput.split('(')

        if (kotlinAndJreVersion.size != 2) {
            return Pair("-", "-")
        }

        val kotlinVersion = kotlinAndJreVersion[0].removePrefix("Kotlin version").trim()
        val jreVersion = kotlinAndJreVersion[1].split('-', ')')[0].trim()
        return Pair(kotlinVersion, jreVersion)
    }
}
