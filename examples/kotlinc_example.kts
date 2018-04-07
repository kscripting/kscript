#!/usr/bin/env kotlinc -script

// This is NOT a kscript example but demonstrates how kotlin scripting would work without kscript.
// In the shebang line we point to kotlinc in scripting mode. Note, that this will just work in some flavors of bash.

// If `args` are showing up as red in Intellij, you've stumbled over an intellij bug.
// See https://youtrack.jetbrains.com/issue/KT-15019

println("Hello from Kotlin!")
for (arg in args) {
    println("arg: $arg")
}