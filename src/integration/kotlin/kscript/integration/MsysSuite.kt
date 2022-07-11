package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: ./gradlew -Dtest.single=MsysSuite -Dos.type=msys integration

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix", "msys")
class MsysSuite
