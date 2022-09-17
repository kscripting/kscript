package org.docopt

import io.github.kscripting.kscript.shell.ShellUtils.quit

/** Simple Kotlin facade for org.org.docopt.Docopt.Docopt(java.lang.String) .*/
class DocOptWrapper(args: Iterable<String>, usage: String) {

    val parsedArgs = try {
        Docopt(usage).withExit(false).parse(args.toList())
    } catch (e: DocoptExitException) {
        // mimic docopts exit behavior but using process-substituion handler at the end
        val ps = System.err

        val message = e.message

        if (message != null) {
            ps.println(message)
        }

        if (e.printUsage) {
            ps.println(usage)
        }

        quit(e.exitCode)
    }

    private val docOptMap by lazy {
        parsedArgs.map {
            it.key.removePrefix("--").replace("[<>]".toRegex(), "") to it.value
        }.toMap()
    }

    fun getString(key: String) = docOptMap[key]!!.toString()
    fun getBoolean(key: String) = docOptMap[key]!!.toString().toBoolean()

    override fun toString(): String {
        return parsedArgs.toString()
    }
}
