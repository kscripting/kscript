#!/usr/bin/env kscript

@file:Repository("$REPO", user="$USER", password="$PASS")
@file:DependsOn("com.eclipsesource.minimal-json:minimal-json:0.9.4")
@file:DependsOn("log4j:log4j:1.2.14")

println("Hello, World!")
