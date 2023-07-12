package kscript.scriplet

fun main(args: Array<String>) {
    // Let's resolve includes!

    fun include_1() = println("include_1")
    fun include_2() = println("include_2")
    fun include_3() = println("include_3")
    fun include_4() = println("include_4")
    fun include_7() = println("include_7")
    fun include_6() = println("include_6")
    fun include_5() = println("include_5")

    // also test a URL inclusion
    fun url_included_1() = println("i came from the internet")
    fun url_included_2() = println("i also came from the internet")

    include_1()
    include_2()
    include_3()
    include_4()
    include_5()
    include_6()
    include_7()
    url_included_1()
    url_included_2()

    println("wow, so many includes")
}
