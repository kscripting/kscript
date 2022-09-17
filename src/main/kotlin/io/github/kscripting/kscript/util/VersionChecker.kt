package io.github.kscripting.kscript.util

import io.github.kscripting.kscript.util.Logger.info
import java.net.URL
import java.util.*

object VersionChecker {
    /** Determine the latest version by checking GitHub repo and print info if newer version is available. */
    fun versionCheck(currentVersion: String) {
        //https://api.github.com/repos/kscripting/kscript/releases/latest
        // "tag_name":"v4.1.1",
        val resolvedUrlText =
            UriUtils.resolveRedirects(URL("https://api.github.com/repos/kscripting/kscript/releases/latest")).readText()
        val latestKscriptVersion = resolvedUrlText.substringAfter("\"tag_name\":\"v").substringBefore("\"")

        if (latestKscriptVersion.isBlank()) {
            info("Could not find information about new version of kscript.")
            return
        }

        if (padVersion(latestKscriptVersion) > padVersion(currentVersion)) {
            info("A new version (v${latestKscriptVersion}) of kscript is available.")
        }
    }

    private fun padVersion(version: String) = try {
        var versionNumbers = version.split(".").map { Integer.valueOf(it) }
        // adjust versions without a patch-release
        while (versionNumbers.size < 3) {
            versionNumbers = versionNumbers + 0
        }

        String.format("%03d%03d%03d", *versionNumbers.toTypedArray())
    } catch (e: MissingFormatArgumentException) {
        throw IllegalArgumentException("Could not pad version $version", e)
    }
}
