package io.github.kscripting.kscript.util

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object UriUtils {
    fun isUrl(string: String): Boolean {
        val normalizedString = string.lowercase().trim()
        return normalizedString.startsWith("http://") || normalizedString.startsWith("https://")
    }

    fun isUrl(uri: URI) = uri.scheme.equals("http") || uri.scheme.equals("https")

    fun isRegularFile(uri: URI) = uri.scheme.startsWith("file")

    fun resolveRedirects(url: URL): URL {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.instanceFollowRedirects = false
        con.connect()

        if (con.responseCode == HttpURLConnection.HTTP_MOVED_PERM || con.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            val redirectUrl = URL(con.getHeaderField("Location"))
            return resolveRedirects(redirectUrl)
        }

        return url
    }
}
