package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: .\gradlew -Dtest.single=WindowsSuite -Dos.type=windows -Dshell.path="C:\Windows\System32\cmd.exe" integration

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("windows")
class WindowsSuite
