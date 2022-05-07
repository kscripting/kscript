kscript_nocall() { kotlin -classpath ${PROJECT_DIR}/build/libs/kscript.jar kscript.app.KscriptKt "$@";}
export -f kscript_nocall

## temp projects with include symlinks
assert_raises "tmpDir=$(kscript_nocall --idea ${PROJECT_DIR}/test/resources/includes/include_variations.kts | cut -f2 -d ' ' | xargs echo); cd $tmpDir && gradle build" 0

## support diamond-shaped include schemes (see #133)
assert_raises "tmpDir=$(kscript_nocall --idea ${PROJECT_DIR}/test/resources/includes/diamond.kts | cut -f2 -d ' ' | xargs echo); cd $tmpDir && gradle build" 0

## todo re-enable interactive mode tests using kscript_nocall
