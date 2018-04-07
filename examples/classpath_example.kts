#!/usr/bin/env kscript

//DEPS com.offbytwo:docopt:0.6.0.20150202,log4j:log4j:1.2.14

//#!/usr/bin/env kotlinc -script -classpath /Users/brandl/.m2/repository/org/docopt/docopt/0.6.0-SNAPSHOT/docopt-0.6.0-SNAPSHOT.jar

import org.docopt.Docopt
import java.io.File
import java.util.*


// woraround for https://youtrack.jetbrains.com/issue/KT-13347
//val args = listOf("foo", "bar")

val usage ="""
kscript  - Enhanced scripting support for Kotlin on *nix-based systems.

Usage:
    kscript ( -t | --text ) <version>
    kscript [ --interactive | --idea | --package ] [--] ( - | <file or URL> ) [<args>]...
    kscript (-h | --help)
    kscript ( --self-update | --clear-cache )

Options:
    -t, --text         Enable stdin support API for more streamlined text processing  [default: latest]
    --package          Package script and dependencies into self-dependent binary
    --idea             boostrap IDEA from a kscript
    -i, --interactive  Create interactive shell with dependencies as declared in script
    -                  Read script from the STDIN
    -h, --help         Print this text
    --self-update      Update kscript to the latest version
    --clear-cache      Wipe cached script jars and urls
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
