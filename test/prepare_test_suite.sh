#!/usr/bin/env bash

# Requires kotlin & wget in PATH
function clean_generate_test_files () {
  rm -f idea
  rm -f 123foo.kts
  rm -f compiler_opts_with_includes_dep.kts
  rm -f compiler_opts_with_includes_master.kt
  rm -f home_dir_master.kts
  rm -f test_script.kts
  rm -f kscriptlet.*
  rm -f package_example
}

export KSCRIPT_HOME=$(pwd)
export PATH=${KSCRIPT_HOME}/build/libs:$PATH
IT_PATH="$KSCRIPT_HOME"

clean_generate_test_files

cd $KSCRIPT_HOME && ./gradlew clean assemble

export PATH=$(pwd):$PATH

if [[ ! -f  "$IT_PATH/assert.sh" ]]; then
  wget --no-check-certificate https://raw.github.com/lehmannro/assert.sh/v1.1/assert.sh && chmod +x assert.sh
fi

if [[ ! -f idea ]]; then
  touch idea && chmod +x idea
fi

which kscript
source $KSCRIPT_HOME/test/test_suite.sh

# clean
clean_generate_test_files
