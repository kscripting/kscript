// Let's resolve includes!

@file:Import("rel_includes/include_1.kt")
@file:Import("./rel_includes//include_2.kt")

@file:Import("./include_3.kt")
@file:Import("include_4.kt")

@file:Import("rel_includes/include_5.kt")


// also test a URL inclusion
//@file:Import("https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/includes/include_by_url.kt")



include_1()
include_2()
include_3()
include_4()
include_5()
include_6()
include_7()
//url_included_1()
//url_included_2()

println("wow, so many includes")
