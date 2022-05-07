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
