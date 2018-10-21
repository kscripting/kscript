#!/usr/bin/env kscript
// TODO fill here what your script does

import kotlin.system.exitProcess

class AnsiColors { companion object { const val ANSI_RESET = "\u001B[0m"; const val ANSI_RED = "\u001B[31m"; const val ANSI_GREEN = "\u001B[32m"; const val ANSI_YELLOW = "\u001B[33m"; const val ANSI_BLUE = "\u001B[34m"; const val ANSI_PURPLE = "\u001B[35m"; const val ANSI_CYAN = "\u001B[36m"; const val ANSI_WHITE = "\u001B[37m"; } }

fun logInfo(message: String) = println("${AnsiColors.ANSI_BLUE}$message${AnsiColors.ANSI_RESET}")
fun logWarn(message: String) = println("${AnsiColors.ANSI_YELLOW}$message${AnsiColors.ANSI_RESET}")
fun logError(message: String) = println("${AnsiColors.ANSI_RED}$message${AnsiColors.ANSI_RESET}")

val usage = """
Use this tool to... <TODO>
<TODO> required params info
"""

if (args.size < 2) {
    logWarn(usage)
    exitProcess(-1)
}

val arg1 = args.get(0)
val arg2 = args.get(1)

// <script>
logInfo("Hello world")
// </script>
