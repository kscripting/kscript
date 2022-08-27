package kscript.app.util

import kscript.app.util.Logger.info
import java.net.URL
import java.util.*

object VersionChecker {
    /** Determine the latest version by checking GitHub repo and print info if newer version is available. */
    private val versionRegex = "(\\d+)(.\\d+)?(.\\d+)?".toRegex()

    fun versionCheck(currentVersion: String) {
        //<title>Release v4.1.0 · kscripting/kscript · GitHub</title>
        val resolvedUrl = UriUtils.resolveRedirects(URL("https://github.com/kscripting/kscript/releases/latest"))
        val latestVersionMatch =
            resolvedUrl.readText()
                .lines()
                .filter { it.contains("<title>") }
                .map { versionRegex.find(it, 0) }
                .getOrNull(0)

        if (latestVersionMatch == null) {
            info("Could not find information about new version of kscript.")
            return
        }

        val latestKscriptVersion = latestVersionMatch.groupValues[0]

        if (padVersion(latestKscriptVersion) > padVersion(currentVersion)) {
            info("""A new version (v${latestKscriptVersion}) of kscript is available.""")
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
