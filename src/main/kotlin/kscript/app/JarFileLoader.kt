package kscript.app

import java.io.File
import java.net.URL
import java.net.URLClassLoader

// https://dzone.com/articles/add-jar-file-java-load-path

class JarFileLoader(urls: Array<URL> = arrayOf()) : URLClassLoader(urls) {
    fun addFile(path: String) {
        val urlPath = "jar:file://$path!/"
        addURL(URL(urlPath))
    }
    fun addFile(file: File) {
        addURL(file.toURI().toURL())
    }
}

