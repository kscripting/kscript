// Let's resolve includes!

fun include_1() = println("include_1")
fun include_2() = println("include_2")

fun include_3() = println("include_3")
fun include_4() = println("include_4")


// also test a URL inclusion
fun url_included() = println("i came from the internet")



include_1()
include_2()
include_3()
include_4()
url_included()

println("wow, so many includes")
