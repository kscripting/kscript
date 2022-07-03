assert "mydsl \"println(foo)\"" "bar"
assert "${PROJECT_DIR}/test/resources/custom_dsl/mydsl_test_with_deps.kts" "foobar"
