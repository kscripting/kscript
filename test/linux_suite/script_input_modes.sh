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

## make sure that it runs with remote URLs
assert "kscript https://raw.githubusercontent.com/holgerbrandl/kscript/master/test/resources/url_test.kts" "I came from the internet"
assert "kscript https://git.io/fxHBv" "main was called"

# repeated compilation of buggy same script should end up in error again
assert_raises "kscript '1-'; kscript '1-'" 1

## missing script gives always error on execution
assert_raises "kscript i_do_not_exist.kts" 1
assert "kscript i_do_not_exist.kts 2>&1" "[kscript] [ERROR] Could not read script from 'i_do_not_exist.kts'"
