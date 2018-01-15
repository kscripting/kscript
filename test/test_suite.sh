#!/usr/bin/env bash
#/bin/bash -x

export DEBUG="--verbose"

. assert.sh


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
#assert_stderr "echo foo" "bar"

#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
#http://stackoverflow.com/questions/3005963/how-can-i-have-a-newline-in-a-string-in-sh
export NL=$'\n'


########################################################################################################################
## script_input_modes

## make sure that scripts can be piped into kscript
assert "source ${KSCRIPT_HOME}/test/resources/direct_script_arg.sh" "kotlin rocks"

## also allow for empty programs
assert "kscript ''" ""

## provide script as direct argument
assert 'kscript "println(1+1)"' '2'


##  use dashed arguments (to prevent regression from https://github.com/holgerbrandl/kscript/issues/59)
assert 'kscript "println(args.joinToString(\"\"))" --arg u ments' '--arguments'
assert 'kscript -s "println(args.joinToString(\"\"))" --arg u ments' '--arguments'


## provide script via stidin
assert "echo 'println(1+1)' | kscript -" "2"

## make sure that heredoc is accepted as argument
assert "source ${KSCRIPT_HOME}/test/resources/here_doc_test.sh" "hello kotlin"

## make sure that command substitution works as expected
assert "source ${KSCRIPT_HOME}/test/resources/cmd_subst_test.sh" "command substitution works as well"

## make sure that it runs with local script files
assert "source ${KSCRIPT_HOME}/test/resources/local_script_file.sh" "kscript rocks!"
#assert "echo foo" "bar" # known to fail

## make sure that it runs with local script files
assert "kscript ${KSCRIPT_HOME}/test/resources/multi_line_deps.kts" "kscript is  cool!"

## scripts with dashes in the file name should work as well
assert "kscript ${KSCRIPT_HOME}/test/resources/dash-test.kts" "dash alarm!"

## scripts with additional dots in the file name should work as well.
## We also test innner uppercase letters in file name here by using .*T*est
assert "kscript ${KSCRIPT_HOME}/test/resources/dot.Test.kts" "dot alarm!"


## missing script
assert_raises "kscript i_do_not_exist.kts" 1
assert "kscript i_do_not_exist.kts 2>&1" "[kscript] [ERROR] Could not read script argument 'i_do_not_exist.kts'"

## make sure that it runs with remote URLs
assert "kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts" "I came from the internet"


# repeated compilation of buggy same script should end up in error again
assert_raises "kscript '1-'; kscript '1-'" 1

assert_end script_input_modes



########################################################################################################################
## cli_helper_tests

## interactive mode without dependencies
#assert "kscript -i 'exitProcess(0)'" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"
#assert "echo '' | kscript -i -" "To create a shell with script dependencies run:\nkotlinc  -classpath ''"


## first version is disabled because support-auto-prefixing kicks in
#assert "kscript -i '//DEPS log4j:log4j:1.2.14'" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"
#assert "kscript -i <(echo '//DEPS log4j:log4j:1.2.14')" "To create a shell with script dependencies run:\nkotlinc  -classpath '${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar'"

#assert_end cli_helper_tests

########################################################################################################################
## environment_tests

## do not run interactive mode prep without script argument
assert_raises "kscript -i" 1

## make sure that KOTLIN_HOME can be guessed from kotlinc correctly
assert "unset KOTLIN_HOME; echo 'println(99)' | kscript -" "99"

## todo test what happens if kotlin/kotlinc/java/maven is not in PATH


assert_end environment_tests

########################################################################################################################
## dependency_lookup

# export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; export PATH=${KSCRIPT_HOME}:${PATH}
resolve_deps() { kotlin -classpath kscript.jar kscript.app.DependencyUtilKt "$@";}
export -f resolve_deps


assert_stderr "resolve_deps log4j:log4j:1.2.14" "${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar"

## impossible version
assert_raises "resolve_deps log4j:log4j:9.8.76" 1

## wrong format should exit with 1
assert_raises "resolve_deps log4j:1.0" 1

assert_stderr "resolve_deps log4j:1.0" "[ERROR] Invalid dependency locator: 'log4j:1.0'.  Expected format is groupId:artifactId:version[:classifier]"

## other version of wrong format should die with useful error.
assert_raises "resolve_deps log4j:::1.0" 1

## one good dependency,  one wrong
assert_raises "resolve_deps org.org.docopt:org.docopt:0.9.0-SNAPSHOT log4j:log4j:1.2.14" 1

assert_end dependency_lookup

########################################################################################################################
## annotation-drive configuration

# make sure that @file:DependsOn is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_annot.kts" "kscript with annotations rocks!"

# make sure that @file:DependsOnMaven is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_maven_annot.kts" "kscript with annotations rocks!"

# make sure that @file:MavenRepository is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/custom_mvn_repo_annot.kts" "kscript with annotations rocks!"


assert_end annotation_config


########################################################################################################################
## support_api

## make sure that one-liners include support-api
assert 'echo "foo${NL}bar" | kscript -t "stdin.print()"' $'foo\nbar'
assert 'echo "foo${NL}bar" | kscript -t "lines.print()"' $'foo\nbar'
#echo "$'foo\nbar' | kscript 'lines.print()'



assert_statement 'echo "foo${NL}bar" | kscript --text "lines.split().select(1, 2, -3)"' "" "[ERROR] Can not mix positive and negative selections" 1

assert_end support_api


########################################################################################################################
##  kt support

## run kt via interpreter mode
assert "${KSCRIPT_HOME}/test/resources/kt_tests/simple_app.kt" "main was called"

## run kt via interpreter mode with dependencies
assert "kscript ${KSCRIPT_HOME}/test/resources/kt_tests/main_with_deps.kt" "made it!"

## test misc entry point with or without package configurations

assert "kscript ${KSCRIPT_HOME}/test/resources/kt_tests/custom_entry_nopckg.kt" "foo companion was called"

assert "kscript ${KSCRIPT_HOME}/test/resources/kt_tests/custom_entry_withpckg.kt" "foo companion was called"

assert "kscript ${KSCRIPT_HOME}/test/resources/kt_tests/default_entry_nopckg.kt" "main was called"

assert "kscript ${KSCRIPT_HOME}/test/resources/kt_tests/default_entry_withpckg.kt" "main was called"


## also make sure that kts in package can be run via kscript
assert "${KSCRIPT_HOME}/test/resources/script_in_pckg.kts" "I live in a package!"


assert_end kt_support





########################################################################################################################
##  custom interpreters

export PATH=${PATH}:${KSCRIPT_HOME}/test/resources/custom_dsl

assert 'mydsl "println(foo)"' "bar"

assert '${KSCRIPT_HOME}/test/resources/custom_dsl/mydsl_test_with_deps.kts' "foobar"

assert_end custom_interpreters



########################################################################################################################
##  run junit-test suite

# exit code of `true` is expected to be 0 (see https://github.com/lehmannro/assert.sh)
assert_raises "./gradlew test"

assert_end junit_tests
