## can we resolve relative imports when using tmp-scripts  (see #95)
assert "rm -f ${PROJECT_DIR}/test/package_example && kscript --package ${PROJECT_DIR}/test/resources/package_example.kts &>/dev/null && ${PROJECT_DIR}/test/package_example 1" "package_me_args_1_mem_536870912"
rm -f "${PROJECT_DIR}/test/package_example"
## https://unix.stackexchange.com/questions/17064/how-to-print-only-last-column
assert 'rm -f scriplet* && kscript --package "println(args.size)" >/dev/null 2>&1 && ${PROJECT_DIR}/test/scriplet three arg uments' "3"
rm -f "${PROJECT_DIR}/test/scriplet"
#assert "kscript --package test/resources/package_example.kts" "foo"
#assert "./package_example 1" "package_me_args_1_mem_4772593664"da
#assert "echo 1" "package_me_args_1_mem_4772593664"
#assert_statement 'rm -f kscriptlet* && kscript --package "println(args.size)"' "foo" "bar" 0

# ensure that the jar file is executable
# This test doesn't appear to be needed as we're not executing jars directly
#assert_raises "java -jar build/lib/kscript.jar" 0
