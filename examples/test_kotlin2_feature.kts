#!/usr/bin/env kscript

// This script uses a 'value class', a feature refined in modern Kotlin.
@JvmInline
value class UserId(val id: String) {
    fun greet(): String = "Hello, User '${id}' from a value class!"
}

fun main() {
    val userId = UserId("KTS_User_123")
    val greeting = userId.greet()
    println(greeting)
    println("Kotlin 2.x feature (value class) test successful!")
}

main()
