#!/usr/bin/env bash

# Provide suite names which you want to execute as a parameter
# Example:
# no parameters or 'ALL' - execute all test suites
# 'junit' - execute just unit tests

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR=$(realpath "$SCRIPT_DIR/../")

REQUESTED_SUITES="${@:-ALL}"
echo "Starting KScript test suites: $REQUESTED_SUITES"
echo

########################################################################################################################

source "$SCRIPT_DIR/linux_environment.sh"
echo

########################################################################################################################

start_suite "clean" $REQUESTED_SUITES
start_suite "assemble" $REQUESTED_SUITES
start_suite "junit" $REQUESTED_SUITES
start_suite "script_input_modes" $REQUESTED_SUITES
#start_suite "cli_helper" $REQUESTED_SUITES
start_suite "environment" $REQUESTED_SUITES
start_suite "annotation_driven_configuration" $REQUESTED_SUITES
start_suite "support_api" $REQUESTED_SUITES
start_suite "kt_support" $REQUESTED_SUITES
start_suite "custom_interpreters" $REQUESTED_SUITES
start_suite "misc" $REQUESTED_SUITES
start_suite "bootstrap_headers" $REQUESTED_SUITES
start_suite "packaging" $REQUESTED_SUITES
start_suite "idea" $REQUESTED_SUITES
