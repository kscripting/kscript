echo "fun foo() = 42" > ~/.kscript_home_include.kts

echo '
@file:Include("~/.kscript_home_include.kts")
println(foo())
' > $1/home_dir_master.kts

kscript $1/home_dir_master.kts
