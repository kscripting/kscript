// Let's resolve includes by directory!

//INCLUDE_DIRS comment
//INCLUDE_DIRS ./comment_dot

@file:IncludeDirs("annotation")
@file:IncludeDirs("./annotation_dot")

comment_include_1()
comment_include_2()
comment_include_3()
comment_dot_include_1()
comment_dot_include_2()
comment_dot_include_3()

annotation_include_1()
annotation_include_2()
annotation_include_3()
annotation_dot_include_1()
annotation_dot_include_2()
annotation_dot_include_3()

println("wow, so many includes")
