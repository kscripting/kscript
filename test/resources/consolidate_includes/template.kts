#!/usr/bin/env kscript

@file:DependsOn("com.eclipsesource.minimal-json:minimal-json:0.9.4")

@file:Include("file1.kts")
@file:Include("file2.kts")
@file:Include("file3.kts")

@file:DependsOn("log4j:log4j:1.2.14")

include_5()
importMoreStuff()
