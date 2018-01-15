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

## provide script via stidin with further switch (to avoid regressions of #94)
assert "echo 'println(1+3)' | kscript - --foo"  "4"

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
assert "kscript https://git.io/fxHBv" "main was called"

## there are some dependencies which are not jar, but maybe pom, aar, ..
## make sure they work, too
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_with_type.kts" "getBigDecimal(1L): 1"

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

## run script that tries to find out its own filename via environment variable
f="${KSCRIPT_HOME}/test/resources/uses_self_file_name.kts"
assert "$f" "Usage: $f [-ae] [--foo] file+"


assert_end environment_tests

########################################################################################################################
## dependency_lookup

# export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"; export PATH=${KSCRIPT_HOME}:${PATH}
resolve_deps() { kotlin -classpath ${KSCRIPT_HOME}/build/libs/kscript.jar kscript.app.DependencyUtil "$@";}
export -f resolve_deps


assert_stderr "resolve_deps log4j:log4j:1.2.14" "${HOME}/.m2/repository/log4j/log4j/1.2.14/log4j-1.2.14.jar"

## impossible version
assert_raises "resolve_deps log4j:log4j:9.8.76" 1

## wrong format should exit with 1
assert_raises "resolve_deps log4j:1.0" 1

assert_stderr "resolve_deps log4j:1.0" "[ERROR] Invalid dependency locator: 'log4j:1.0'.  Expected format is groupId:artifactId:version[:classifier][@type]"

## other version of wrong format should die with useful error.
assert_raises "resolve_deps log4j:::1.0" 1

## one good dependency,  one wrong
assert_raises "resolve_deps org.org.docopt:org.docopt:0.9.0-SNAPSHOT log4j:log4j:1.2.14" 1

assert_end dependency_lookup

########################################################################################################################
## annotation-driven configuration

# make sure that @file:DependsOn is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_annot.kts" "kscript with annotations rocks!"

# make sure that @file:DependsOnMaven is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_maven_annot.kts" "kscript with annotations rocks!"

# make sure that dynamic versions are matched properly
assert "kscript ${KSCRIPT_HOME}/test/resources/depends_on_dynamic.kts" "dynamic kscript rocks!"

# make sure that @file:MavenRepository is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/custom_mvn_repo_annot.kts" "kscript with annotations rocks!"


assert_stderr "kscript ${KSCRIPT_HOME}/test/resources/illegal_depends_on_arg.kts" '[kscript] [ERROR] Artifact locators must be provided as separate annotation arguments and not as comma-separated list: [com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0]'


# make sure that @file:MavenRepository is parsed correctly
assert "kscript ${KSCRIPT_HOME}/test/resources/script_with_compile_flags.kts" "hoo_ray"


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



## can we resolve relative imports when using tmp-scripts  (see #95)
assert "rm -f ./package_example && kscript --package test/resources/package_example.kts &>/dev/null && ./package_example 1" "package_me_args_1_mem_4772593664"

## https://unix.stackexchange.com/questions/17064/how-to-print-only-last-column
assert 'rm -f kscriptlet* && cmd=$(kscript --package "println(args.size)" 2>&1 | tail -n1 | cut -f 5 -d " ")  && $cmd three arg uments' "3"

#assert "kscript --package test/resources/package_example.kts" "foo"
#assert "./package_example 1" "package_me_args_1_mem_4772593664"da
#assert "echo 1" "package_me_args_1_mem_4772593664"
#assert_statement 'rm -f kscriptlet* && kscript --package "println(args.size)"' "foo" "bar" 0



########################################################################################################################
##  custom interpreters

export PATH=${PATH}:${KSCRIPT_HOME}/test/resources/custom_dsl

assert 'mydsl "println(foo)"' "bar"

assert '${KSCRIPT_HOME}/test/resources/custom_dsl/mydsl_test_with_deps.kts' "foobar"

assert_end custom_interpreters



########################################################################################################################
##  misc

## prevent regressions of #98 (it fails to process empty or space-containing arguments)
assert 'kscript "println(args.size)" foo bar' 2         ## regaular args
assert 'kscript "println(args.size)" "" foo bar' 3      ## accept empty args
assert 'kscript "println(args.size)" "--params foo"' 1  ## make sure dash args are not confused with options
assert 'kscript "println(args.size)" "foo bar"' 1       ## allow for spaces
assert 'kscript "println(args[0])" "foo bar"' "foo bar" ## make sure quotes are not propagated into args

## prevent regression of #181
assert 'echo "println(123)" > 123foo.kts; kscript 123foo.kts' "123"


## prevent regression of #185
assert "source ${KSCRIPT_HOME}/test/resources/home_dir_include.sh" "42"

## prevent regression of #173
assert "source ${KSCRIPT_HOME}/test/resources/compiler_opts_with_includes.sh" "hello42"

## KOTLIN_OPTS
assert "kscript ${KSCRIPT_HOME}/test/resources/kotlin_opts_test.kts" "mem_4772593664_1_foo_bar"

## kscript.jar class access test
assert_raises 'kscript "println(org.docopt.Docopt::class)"' 1


##
## --idea tests were commented out as they do not make sense anymore

# kscript_nocall() { kotlin -classpath ${KSCRIPT_HOME}/build/libs/kscript.jar kscript.app.KscriptKt "$@";}
# export -f kscript_nocall

# ## temp projects with include symlinks
# assert_raises 'tmpDir=$(kscript_nocall --idea test/resources/includes/include_variations.kts | cut -f2 -d" " | xargs echo); cd $tmpDir && gradle build' 0

# ## support diamond-shaped include schemes (see #133)
# assert_raises 'tmpDir=$(kscript_nocall --idea test/resources/includes/diamond.kts | cut -f2 -d" " | xargs echo); cd $tmpDir && gradle build' 0

## todo reenable interactive mode tests using kscript_nocall

assert_end misc


########################################################################################################################
##  run junit-test suite

# exit code of `true` is expected to be 0 (see https://github.com/lehmannro/assert.sh)
assert_raises "./gradlew test"

assert_end junit_tests


########################################################################################################################
##  bootstrap header

f=/tmp/echo_stdin_args.kts
cp ${KSCRIPT_HOME}/test/resources/echo_stdin_args.kts $f

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

assert_end bootstrap_header
