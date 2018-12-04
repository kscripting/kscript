#!/usr/bin/env kscript

if (System.console() == null) {
    print(generateSequence(::readLine).joinToString("\n") + " | ")
}
println("script ${args.toList().joinToString(" ")}")