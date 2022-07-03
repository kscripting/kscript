## do not run interactive mode prep without script argument
assert_raises "kscript -i" 1

## make sure that KOTLIN_HOME can be guessed from kotlinc correctly
assert "unset KOTLIN_HOME; echo 'println(99)' | kscript -" "99"

## todo test what happens if kotlin/kotlinc/java/maven is not in PATH

## run script that tries to find out its own filename via environment variable
f="${PROJECT_DIR}/test/resources/uses_self_file_name.kts"
assert "$f" "Usage: $f [-ae] [--foo] file+"
