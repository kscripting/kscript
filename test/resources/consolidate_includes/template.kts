#!/usr/bin/env kscript

@file:DependsOn("com.eclipsesource.minimal-json:minimal-json:0.9.4")

@file:Import("file1.kts")
@file:Import("file2.kts")
@file:Import("file3.kts")

@file:DependsOn("log4j:log4j:1.2.14")

include_5()
importMoreStuff()
