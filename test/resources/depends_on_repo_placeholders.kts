#!/usr/bin/env kscript

@file:Repository("{{KSCRIPT_REPOSITORY_URL}}", user="{{KSCRIPT_REPOSITORY_USER}}-suffix", password="prefix-{{KSCRIPT_REPOSITORY_PASSWORD}}")
@file:DependsOn("com.eclipsesource.minimal-json:minimal-json:0.9.4")
@file:DependsOn("log4j:log4j:1.2.14")

println("Hello, World!")
