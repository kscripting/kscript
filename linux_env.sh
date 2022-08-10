#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR=$(realpath "$SCRIPT_DIR")
KSCRIPT_EXEC_DIR="$PROJECT_DIR/build/libs"
KSCRIPT_TEST_DIR="$PROJECT_DIR/build/tmp/test"

mkdir -p $KSCRIPT_EXEC_DIR
mkdir -p $KSCRIPT_TEST_DIR

echo "Setting up environment..."
echo "SCRIPT_DIR :        $SCRIPT_DIR"
echo "PROJECT_DIR:        $PROJECT_DIR"
echo "KSCRIPT_EXEC_DIR:   $KSCRIPT_EXEC_DIR"
echo "KSCRIPT_TEST_DIR:   $KSCRIPT_TEST_DIR"
echo

if [[ "$PATH" != *"$KSCRIPT_EXEC_DIR"* ]]; then
  export PATH=$KSCRIPT_EXEC_DIR:$PATH
fi

echo  "KScript path for testing: $(which kscript)"

alias cdk="cd $PROJECT_DIR"

alias switchPath='
if [[ "$PATH" != *"$KSCRIPT_EXEC_DIR"* ]]; then
  export PATH="$KSCRIPT_EXEC_DIR:$PATH"
  echo "Project path set."
else
  export PATH=$(echo $PATH | tr ":" "\n" | grep -v "$KSCRIPT_EXEC_DIR" | grep -v "^$" | tr "\n" ":")
  echo "Generic path set."
fi
'

alias help-dev="cat $SCRIPT_DIR/test/help-dev.txt"
