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
echo "Compiling KScript... Please wait..."

cd $PROJECT_DIR
./gradlew clean assemble
EXIT_CODE="$?"
cd -

if [[ "$EXIT_CODE" -ne "0" ]]; then
  echo
  echo "KScript build terminated with invalid exit code $EXIT_CODE..."
  exit 1
fi

########################################################################################################################

source "$SCRIPT_DIR/setup_environment.sh"
echo

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
SUITE="script_input_modes"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## make sure that scripts can be piped into kscript
  assert "source ${PROJECT_DIR}/test/resources/direct_script_arg.sh" "kotlin rocks"

  ## also allow for empty programs
  assert "kscript ''" ""

  ## provide script as direct argument
  assert 'kscript "println(1+1)"' '2'

  ##  use dashed arguments (to prevent regression from https://github.com/holgerbrandl/kscript/issues/59)
  assert 'kscript "println(args.joinToString(\"\"))" --arg u ments' '--arguments'
  assert 'kscript -s "println(args.joinToString(\"\"))" --arg u ments' '--arguments'

  ## provide script via stidin
  assert "echo 'println(1+1)' | kscript -" "2"

  ## provide script via stidin with further switch (to avoid regressions of #94)
  assert "echo 'println(1+3)' | kscript - --foo"  "4"

  ## make sure that heredoc is accepted as argument
  assert "source ${PROJECT_DIR}/test/resources/here_doc_test.sh" "hello kotlin"

  ## make sure that command substitution works as expected
  assert "source ${PROJECT_DIR}/test/resources/cmd_subst_test.sh" "command substitution works as well"

  ## make sure that it runs with local script files
  assert "source ${PROJECT_DIR}/test/resources/local_script_file.sh" "kscript rocks!"
  #assert "echo foo" "bar" # known to fail

  ## make sure that it runs with local script files
  assert "kscript ${PROJECT_DIR}/test/resources/multi_line_deps.kts" "kscript is  cool!"

  ## scripts with dashes in the file name should work as well
  assert "kscript ${PROJECT_DIR}/test/resources/dash-test.kts" "dash alarm!"

  ## scripts with additional dots in the file name should work as well.
  ## We also test innner uppercase letters in file name here by using .*T*est
  assert "kscript ${PROJECT_DIR}/test/resources/dot.Test.kts" "dot alarm!"

  ## missing script
  assert_raises "kscript i_do_not_exist.kts" 1
  assert "kscript i_do_not_exist.kts 2>&1" "[kscript] [ERROR] Could not read script from 'i_do_not_exist.kts'"

  ## make sure that it runs with remote URLs
  assert "kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts" "I came from the internet"
  assert "kscript https://git.io/fxHBv" "main was called"

  # repeated compilation of buggy same script should end up in error again
  assert_raises "kscript '1-'; kscript '1-'" 1

  assert_end "$SUITE"
fi

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
SUITE="environment"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## do not run interactive mode prep without script argument
  assert_raises "kscript -i" 1

  ## make sure that KOTLIN_HOME can be guessed from kotlinc correctly
  assert "unset KOTLIN_HOME; echo 'println(99)' | kscript -" "99"

  ## todo test what happens if kotlin/kotlinc/java/maven is not in PATH

  ## run script that tries to find out its own filename via environment variable
  f="${PROJECT_DIR}/test/resources/uses_self_file_name.kts"
  assert "$f" "Usage: $f [-ae] [--foo] file+"

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="annotation_driven_configuration"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## there are some dependencies which are not jar, but maybe pom, aar,... make sure they work, too
  assert "kscript ${PROJECT_DIR}/test/resources/depends_on_with_type.kts" "getBigDecimal(1L): 1"

  # make sure that @file:DependsOn is parsed correctly
  assert "kscript ${PROJECT_DIR}/test/resources/depends_on_annot.kts" "kscript with annotations rocks!"

  # make sure that @file:DependsOnMaven is parsed correctly
  assert "kscript ${PROJECT_DIR}/test/resources/depends_on_maven_annot.kts" "kscript with annotations rocks!"

  # make sure that dynamic versions are matched properly
  assert "kscript ${PROJECT_DIR}/test/resources/depends_on_dynamic.kts" "dynamic kscript rocks!"

  # make sure that @file:MavenRepository is parsed correctly
  assert "kscript ${PROJECT_DIR}/test/resources/custom_mvn_repo_annot.kts" "kscript with annotations rocks!"
  assert_stderr "kscript ${PROJECT_DIR}/test/resources/illegal_depends_on_arg.kts" '[kscript] [ERROR] Artifact locators must be provided as separate annotation arguments and not as comma-separated list: [com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0]'

  # make sure that @file:MavenRepository is parsed correctly
  assert "kscript ${PROJECT_DIR}/test/resources/script_with_compile_flags.kts" "hoo_ray"

  ## Ensure dependencies are solved correctly #345
    rm -rf ~/.m2/repository/com/beust
    assert "kscript ${PROJECT_DIR}/test/resources/depends_on_klaxon.kts" "Successfully resolved klaxon"

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="support_api"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## make sure that one-liners include support-api
  assert 'echo "foo${NL}bar" | kscript -t "stdin.print()"' $'foo\nbar'
  assert 'echo "foo${NL}bar" | kscript -t "lines.print()"' $'foo\nbar'
  #echo "$'foo\nbar' | kscript 'lines.print()'

  assert_statement 'echo "foo${NL}bar" | kscript -s --text "lines.split().select(1, 2, -3)"' "" "[ERROR] Can not mix positive and negative selections" 1

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="kt_support"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## run kt via interpreter mode
  assert "${PROJECT_DIR}/test/resources/kt_tests/simple_app.kt" "main was called"

  ## run kt via interpreter mode with dependencies
  assert "kscript ${PROJECT_DIR}/test/resources/kt_tests/main_with_deps.kt" "made it!"

  ## test misc entry point with or without package configurations

  assert "kscript ${PROJECT_DIR}/test/resources/kt_tests/custom_entry_nopckg.kt" "foo companion was called"

  assert "kscript ${PROJECT_DIR}/test/resources/kt_tests/custom_entry_withpckg.kt" "foo companion was called"

  assert "kscript ${PROJECT_DIR}/test/resources/kt_tests/default_entry_nopckg.kt" "main was called"

  assert "kscript ${PROJECT_DIR}/test/resources/kt_tests/default_entry_withpckg.kt" "main was called"

  ## also make sure that kts in package can be run via kscript
  assert "${PROJECT_DIR}/test/resources/script_in_pckg.kts" "I live in a package!"

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="custom_interpreters"
if start_suite $SUITE $REQUESTED_SUITES; then
  export PATH=${PATH}:${PROJECT_DIR}/test/resources/custom_dsl

  assert "mydsl \"println(foo)\"" "bar"

  assert "${PROJECT_DIR}/test/resources/custom_dsl/mydsl_test_with_deps.kts" "foobar"

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="misc"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## prevent regressions of #98 (it fails to process empty or space-containing arguments)
  assert 'kscript "println(args.size)" foo bar' 2         ## regaular args
  assert 'kscript "println(args.size)" "" foo bar' 3      ## accept empty args
  assert 'kscript "println(args.size)" "--params foo"' 1  ## make sure dash args are not confused with options
  assert 'kscript "println(args.size)" "foo bar"' 1       ## allow for spaces
  assert 'kscript "println(args[0])" "foo bar"' "foo bar" ## make sure quotes are not propagated into args

  ## prevent regression of #181
  assert "echo \"println(123)\" > $KSCRIPT_TEST_DIR/123foo.kts; kscript $KSCRIPT_TEST_DIR/123foo.kts" "123"

  ## prevent regression of #185
  assert "source ${PROJECT_DIR}/test/resources/home_dir_include.sh" "42"

  ## prevent regression of #173
  assert "source ${PROJECT_DIR}/test/resources/compiler_opts_with_includes.sh" "hello42"

  ## Ensure relative includes with in shebang mode
  assert_raises "${PROJECT_DIR}/test/resources/includes/shebang_mode_includes" 0

  ## Ensure that compilation errors are not cached #349
  # first run (not yet cached)
  kscript ${PROJECT_DIR}/test/resources/invalid_script.kts &> /dev/null
  # real test
  assert "kscript ${PROJECT_DIR}/test/resources/invalid_script.kts 2>&1 | grep \"Compilation of scriplet failed\"" "[kscript] [ERROR] Compilation of scriplet failed:"

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="bootstrap_headers"
if start_suite $SUITE $REQUESTED_SUITES; then
  f=/tmp/echo_stdin_args.kts
  cp "${PROJECT_DIR}/test/resources/echo_stdin_args.kts" $f

  # ensure script works as is
  assert 'echo stdin | '$f' --foo bar' "stdin | script --foo bar"

  # add bootstrap header
  assert 'kscript --add-bootstrap-header '$f ''

  # ensure adding it again raises an error
  assert_raises 'kscript --add-bootstrap-header '$f 1

  # ensure scripts works with header, including stdin
  assert 'echo stdin | '$f' --foo bar' "stdin | script --foo bar"

  # ensure scripts works with header invoked with explicit `kscript`
  assert 'echo stdin | kscript '$f' --foo bar' "stdin | script --foo bar"

  rm $f

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="packaging"
if start_suite $SUITE $REQUESTED_SUITES; then
  ## can we resolve relative imports when using tmp-scripts  (see #95)
  assert "rm -f ${PROJECT_DIR}/test/package_example && kscript --package ${PROJECT_DIR}/test/resources/package_example.kts &>/dev/null && ${PROJECT_DIR}/test/package_example 1" "package_me_args_1_mem_5368709120"

  ## https://unix.stackexchange.com/questions/17064/how-to-print-only-last-column
  assert 'rm -f kscriptlet* && cmd=$(kscript --package "println(args.size)" 2>&1 | tail -n1 | cut -f 5 -d " ") && $cmd three arg uments' "3"

  #assert "kscript --package test/resources/package_example.kts" "foo"
  #assert "./package_example 1" "package_me_args_1_mem_4772593664"da
  #assert "echo 1" "package_me_args_1_mem_4772593664"
  #assert_statement 'rm -f kscriptlet* && kscript --package "println(args.size)"' "foo" "bar" 0

  # ensure that the jar file is executable
  assert_raises "java -jar build/lib/kscript.jar" 0

  assert_end "$SUITE"
fi

########################################################################################################################
SUITE="idea"
if start_suite $SUITE $REQUESTED_SUITES; then
  kscript_nocall() { kotlin -classpath ${PROJECT_DIR}/build/libs/kscript.jar kscript.app.KscriptKt "$@";}
  export -f kscript_nocall

  ## temp projects with include symlinks
  assert_raises "tmpDir=$(kscript_nocall --idea ${PROJECT_DIR}/test/resources/includes/include_variations.kts | cut -f2 -d ' ' | xargs echo); cd $tmpDir && gradle build" 0

  ## support diamond-shaped include schemes (see #133)
  assert_raises "tmpDir=$(kscript_nocall --idea ${PROJECT_DIR}/test/resources/includes/diamond.kts | cut -f2 -d ' ' | xargs echo); cd $tmpDir && gradle build" 0

  ## todo re-enable interactive mode tests using kscript_nocall

  assert_end "$SUITE"
fi
