package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: ./gradlew -Dtest.single=LinuxSuite -Dos.type=linux integration

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix")
class LinuxSuite
