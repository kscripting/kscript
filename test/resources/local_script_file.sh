scriptFile=$KSCRIPT_TEST_DIR/test_script.kts
echo 'println("kscript rocks!")' > ${scriptFile}
kscript $scriptFile
