package kscript.integration

import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

@Suite
@SelectPackages("kscript.integration.test.*")
@IncludeTags("posix", "cygwin")
class CygwinSuite
