#!/usr/bin/env kscript

//DEPS log4j:log4j:1.2.14
//ENTRY  Foo
//KOTLIN_OPTS -J-Xmx5g  -J-server
//COMPILER_OPTS -cp build/libs/kscript.jar

package test

//INCLUDE ./deprecated_report_include.kts

class Foo{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            printStuff()
            org.apache.log4j.Logger.getRootLogger()
        }
    }
}
