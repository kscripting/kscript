package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: ./gradlew -Dtest.single=MsysSuite -Dos.type=msys -Dshell.path="C:/Programy/Programowanie/Git/usr/bin/bash.exe" integration

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix")
class MsysSuite
