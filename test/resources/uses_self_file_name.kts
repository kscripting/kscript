#!/usr/bin/env kscript
val fileName = System.getenv("KSCRIPT_FILE").split('/', '\\').last()
println("Usage: $fileName [-ae] [--foo] file+")
