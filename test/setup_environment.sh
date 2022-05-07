#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR=$(realpath "$SCRIPT_DIR/../")
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

if [[ ! -f "$KSCRIPT_EXEC_DIR/assert.sh" ]]; then
  echo "Installing assert.sh"
  curl --silent --show-error -L -o "$KSCRIPT_EXEC_DIR/assert.sh" https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
  chmod u+x "$KSCRIPT_EXEC_DIR/assert.sh"
fi

export DEBUG="--verbose"
. assert.sh

# Fake idea binary just printing passed arguments...
if [[ ! -f "$KSCRIPT_EXEC_DIR/idea" ]]; then
  echo "#!/usr/bin/env bash" > "${KSCRIPT_EXEC_DIR}/idea"
  echo "echo \$*" >> "${KSCRIPT_EXEC_DIR}/idea"
  chmod +x "${KSCRIPT_EXEC_DIR}/idea"
fi

## define test helper, see https://github.com/lehmannro/assert.sh/issues/24
assert_statement(){
    # usage cmd exp_stout exp_stder exp_exit_code
    assert "$1" "$2"
    assert "( $1 ) 2>&1 >/dev/null" "$3"
    assert_raises "$1" "$4"
}
#assert_statment "echo foo; echo bar  >&2; exit 1" "foo" "bar" 1


assert_stderr(){
    assert "( $1 ) 2>&1 >/dev/null" "$2"
}

# $1 - suite name; $2... - requested suites
start_suite() {
  PARAM_SUITE="$1"
  PARAM_REQUESTED_SUITES="${@:2}"

  if [[ "${PARAM_REQUESTED_SUITES}" =~ "${PARAM_SUITE}" ]] || [[ "${PARAM_REQUESTED_SUITES}" == "ALL" ]]; then
    echo
    echo "Starting '$PARAM_SUITE' tests:"

    SUITE_FILE="${PROJECT_DIR}/test/suite/${PARAM_SUITE}.sh"
    if [[ -f ${SUITE_FILE} ]]; then
      source ${SUITE_FILE}

      assert_end "$PARAM_SUITE"
    fi

    return 0
  fi

  echo "Skipping '$PARAM_SUITE' tests..."
  return 1
}

#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
export NL=$'\n'

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
alias help-dev="cat $SCRIPT_DIR/help-dev.txt"
