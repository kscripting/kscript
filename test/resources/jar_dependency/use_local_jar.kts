#!/usr/bin/env kscript

@file:KotlinOptions("-cp build/libs/jartester-1.0-SNAPSHOT.jar")
@file:CompilerOptions("-cp build/libs/jartester-1.0-SNAPSHOT.jar")

import  com.github.holgerbrandl.kscript.test.SomethingCool


SomethingCool().foo()
