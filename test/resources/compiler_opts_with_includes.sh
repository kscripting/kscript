## Prevent regressions of https://github.com/holgerbrandl/kscript/issues/173

echo "fun foo() = 42" > $1/compiler_opts_with_includes_dep.kts

cat <<"EOF" > $1/compiler_opts_with_includes_master.kt
@file:CompilerOptions("-jvm-target 1.8")
@file:Include("compiler_opts_with_includes_dep.kts")
@file:DependsOnMaven("log4j:log4j:1.2.14")

import java.io.File

fun main(args: Array<String>) {
    println("hello"+foo())
}
EOF

kscript $1/compiler_opts_with_includes_master.kt
