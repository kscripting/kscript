package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: ./gradlew -Dtest.single=LinuxSuite -Dos.type=linux integration
//or
//https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher
//java -jar junit-platform-console-standalone-1.8.2.jar <Options>
//or
//gradle -Dos.type=linux integration --tests CustomInterpretersTest

//Way to provide tags from command line:
//https://javabydeveloper.com/run-tag-specific-junit-5-tests-from-gradle-command/

//Starting Junit from code (switch can be done in code not in bash):
//https://stackoverflow.com/questions/24943839/how-to-run-entire-junit-test-suite-from-within-code

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix")
class LinuxSuite
