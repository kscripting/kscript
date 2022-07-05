package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

//Execution: gradle -Dtest.single=LinuxSuite integration
//More generic: 'gradle integration --tests LinuxSuite', doesn't work

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix")
class LinuxSuite
