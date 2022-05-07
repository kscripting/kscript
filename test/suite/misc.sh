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
