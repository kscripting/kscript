#!/usr/bin/env bash


if [ $# -ne 1 ]; then
    echo "Usage: kshell_kts.sh <kscript.kts>"
    exit 0
fi

tmpfile=$(mktemp).kts

#echo '@file:Import("https://git.io/fAJ5h")' >> $tmpfile
echo '
@file:DependsOn("org.apache.hadoop:hadoop-common:2.7.0")

// should be now on maven central
@file:DependsOn("com.github.khud:kshell-repl-api:0.2.4-1.2.60")

@file:DependsOn("sparklin:jline3-shaded:0.2")

//@file:DependsOn("sparklin:kshell:0.2-SNAPSHOT")
@file:DependsOn("sparklin:kshell:0.2.5")

' > $tmpfile
echo '' >> $tmpfile

argScript=$1
#argScript=krangl_example.kts

cat $argScript | grep '@file' >> $tmpfile

#cat $tmpfile


echo "Preparing interactive session by resolving script dependencies..."

## resolve dependencies without running the kscript
KSCRIPT_DIR=$(dirname $(which kscript))
kscript_nocall() { kotlin -classpath ${KSCRIPT_DIR}/kscript.jar kscript.app.KscriptKt "$@";}

kshellCP=$(kscript_nocall $tmpfile | cut -d' ' -f4)

## create new
java -classpath "${kshellCP}" com.github.khud.sparklin.kshell.KotlinShell $@
