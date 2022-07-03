# Clearing kscript app directory
rm -rf ~/.kscript

cd $PROJECT_DIR
./gradlew clean
EXIT_CODE="$?"
cd -

assert "echo $EXIT_CODE" "0"

if [[ "$EXIT_CODE" -ne "0" ]]; then
  assert_end "clean"

  echo
  echo "KScript build terminated with invalid exit code $EXIT_CODE..."
  exit 1
fi

copy_executables
