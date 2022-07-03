cd $PROJECT_DIR
./gradlew test
EXIT_CODE="$?"
cd -

assert "echo $EXIT_CODE" "0"
