echo "fun foo() = 42" > ~/.kscript_home_include.kts

echo '
//INCLUDE ~/.kscript_home_include.kts
println(foo())
' > $KSCRIPT_TEST_DIR/home_dir_master.kts

kscript $KSCRIPT_TEST_DIR/home_dir_master.kts
