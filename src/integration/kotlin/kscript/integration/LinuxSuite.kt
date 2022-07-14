package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: ./gradlew -Dtest.single=LinuxSuite -Dos.type=linux integration
//or
//https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher
//java -jar junit-platform-console-standalone-1.8.2.jar <Options>

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix")
class LinuxSuite
