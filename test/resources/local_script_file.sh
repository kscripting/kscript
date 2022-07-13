mkdir -p $1
scriptFile=$1/test_script.kts
echo 'println("kscript rocks!")' > $scriptFile
kscript $scriptFile
