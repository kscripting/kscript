#!/usr/bin/env bash

# Provide suite names which you want to execute as a parameter
# Example:
# no parameters or 'ALL' - execute all test suites
# '-' (dash) - just compile - no suites executed
# 'junit' - execute just unit tests

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR=$(realpath "$SCRIPT_DIR/../")

REQUESTED_SUITES="${@:-ALL}"
echo "Starting KScript test suites: $REQUESTED_SUITES"
echo

kscript --clear-cache
echo

########################################################################################################################

source "$SCRIPT_DIR/setup_environment.sh"
echo

########################################################################################################################
SUITE="assemble"
if start_suite $SUITE $REQUESTED_SUITES; then
  cd $PROJECT_DIR
  ./gradlew clean assemble
  EXIT_CODE="$?"
  cd -

  assert "echo $EXIT_CODE" "0"
  assert_end "$SUITE"
fi

if [[ "$EXIT_CODE" -ne "0" ]]; then
  echo
  echo "KScript build terminated with invalid exit code $EXIT_CODE..."
  exit 1
fi

########################################################################################################################
SUITE="junit"
if start_suite $SUITE $REQUESTED_SUITES; then
  cd $PROJECT_DIR
  ./gradlew test
  EXIT_CODE="$?"
  cd -

  assert "echo $EXIT_CODE" "0"

  assert_end "$SUITE"
fi

########################################################################################################################

start_suite "script_input_modes" $REQUESTED_SUITES

########################################################################################################################
#SUITE="cli_helper"
#echo
#echo "Starting $SUITE tests:"

## interactive mode without dependencies
#assert "kscript -i 'exitProcess(0)'" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"
#assert "echo '' | kscript -i -" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


## first version is disabled because support-auto-prefixing kicks in
#assert "kscript -i '//DEPS log4j:log4j:1.2.14'" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"
#assert "kscript -i <(echo '//DEPS log4j:log4j:1.2.14')" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"

#assert_end "$SUITE"

########################################################################################################################

start_suite "environment" $REQUESTED_SUITES
start_suite "annotation_driven_configuration" $REQUESTED_SUITES
start_suite "support_api" $REQUESTED_SUITES
start_suite "kt_support" $REQUESTED_SUITES
start_suite "custom_interpreters" $REQUESTED_SUITES
start_suite "misc" $REQUESTED_SUITES
start_suite "bootstrap_headers" $REQUESTED_SUITES
start_suite "packaging" $REQUESTED_SUITES
start_suite "idea" $REQUESTED_SUITES
