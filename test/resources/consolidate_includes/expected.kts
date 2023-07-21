package kscript.scriplet

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.net.URL

fun importStuff() = File(".")

importStuff()
fun importMoreStuff() = File(".")

fun foo() = File(".").toURL()

fun include_4() = println("include_4")
fun include_5() = BufferedReader::class
fun include_6() = InputStream::class

include_5()
importMoreStuff()
