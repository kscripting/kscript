#######################################################################################################################
### jar script embedding https://stackoverflow.com/questions/17339631/creating-shell-script-from-executable-java-jar-file

cd /Users/brandl/projects/kotlin/kscript/misc/experimental

#cp kscript2 kscriptJarStub
(cd ../../ && ./gradlew shadowJar)

cat kscriptJarStub /Users/brandl/projects/kotlin/kscript/build/libs/kscript-0.1-SNAPSHOT-all.jar > kscriptJar && chmod +x kscriptJar

./kscriptJar --help
./kscriptJar "println(1+1)"
./kscriptJar "1-"

## References
#https://stackoverflow.com/questions/17583578/what-command-means-do-nothing-in-a-conditional-in-bash
#https://github.com/megastep/makeself
#https://stackoverflow.com/questions/10491704/embed-a-executable-binary-in-a-shell-script
