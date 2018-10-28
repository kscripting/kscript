#!/usr/bin/env bash

# Requires kotlin & wget in PATH

export KSCRIPT_HOME=$(pwd)
export PATH=${KSCRIPT_HOME}/build/libs:$PATH
cd $KSCRIPT_HOME && ./gradlew assemble

export PATH=$(pwd):$PATH

mkdir -p it && cd it
if [ ! -f assert.sh ]; then
  wget --no-check-certificate https://raw.github.com/lehmannro/assert.sh/v1.1/assert.sh && chmod +x assert.sh
fi

if [ ! -f idea ]; then
  touch idea && chmod +x idea
fi

which kscript
source $KSCRIPT_HOME/test/test_suite.sh