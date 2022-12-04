package io.github.kscripting.kscript.util

object Logger {
    var devMode = false
    var silentMode = false

    fun info(msg: String = "") = printer(msg)
    fun infoMsg(message: String = "") = if (!silentMode) printer(message, KSCRIPT_NAME) else ""
    fun devMsg(message: String = "") = if (devMode) printer(message, KSCRIPT_NAME, "DEV") else ""
    fun warnMsg(msg: String = "") = printer(msg, KSCRIPT_NAME, "WARN")
    fun errorMsg(exception: Exception): String {
        var message = exception.message ?: exception.javaClass.simpleName

        if (devMode) {
            message += "\n\n" + exception.stackTraceToString()
        }

        return errorMsg(message)
    }
    fun errorMsg(msg: String = "") = printer(msg, KSCRIPT_NAME, "ERROR")

    private fun printer(message: String, vararg tags: String): String {
        var prefix = tags.joinToString(" ") { "[$it]" }

        if (prefix.isNotBlank()) {
            prefix += " "
        }

        val msg = message.lines().joinToString("\n") { it.prependIndent(prefix) }
        System.err.println(msg)
        return msg
    }

    private const val KSCRIPT_NAME = "kscript"
}
