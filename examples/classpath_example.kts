#!/usr/bin/env kscript

//DEPS com.offbytwo:docopt:0.6.0.20150202,log4j:log4j:1.2.14

//#!/usr/bin/env kotlinc -script -classpath /Users/brandl/.m2/repository/org/docopt/docopt/0.6.0-SNAPSHOT/docopt-0.6.0-SNAPSHOT.jar

import org.docopt.Docopt
import java.io.File
import java.util.*


// woraround for https://youtrack.jetbrains.com/issue/KT-13347
//val args = listOf("foo", "bar")

val usage ="""
kscript is a wrapper to "interpret" the Kotlin source file in the way similar to SHELL script or "kotlinc -script"

Usage:
    kscript ( -t | --text ) <text>
    kscript [ --interactive | --idea | --package ] [--] [ - | <file or URL> ]...
    kscript (-h | --help)
    kscript --self-update

Options:
    -t, --text      text processing mode
    --package       deploy scripts as standalone binaries
    --idea          boostrap IDEA from a kscript
    --interactive   treat yourself a REPL
    -               to read script from the STDIN
    -h, --help      this screen
    --self-update   kscript updates itself
"""

val doArgs = Docopt(usage).parse(args.toList())

println("parsed args are: \n$doArgs (${doArgs.javaClass.simpleName})\n")

doArgs.forEach { (key: Any, value: Any) ->
    println("$key:\t$value\t(${value?.javaClass?.canonicalName})")
}

println("\nHello from Kotlin!")
for (arg in args) {
    println("arg: $arg")
}
