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
