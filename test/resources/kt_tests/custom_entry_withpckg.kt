package test

/**
 * @author Holger Brandl
 */

class User(val age: Int)

fun findUserByName(name: String): User = null!!

fun example(names: List<String>) {
    val usersByAge = names
            .mapNotNull { findUserByName(it) }
            .groupBy { it.age }

    //    usersByAge.
}

class Foo{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            println("foo companion was called")
        }
    }
}

fun main(args: Array<String>) {
    println("main was called")

    args.forEach { println(it) }
}


@file:EntryPoint("Foo")
