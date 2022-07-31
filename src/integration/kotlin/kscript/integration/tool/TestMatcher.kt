package kscript.integration.tool

interface TestMatcher {
    fun matches(string: String): Boolean
    fun normalize(string: String) = string.replace("\n", TestContext.nl)
}

class AnyMatch : TestMatcher {
    override fun matches(string: String): Boolean = true
}

class StartsWith(private val expectedString: String, private val ignoreCase: Boolean) : TestMatcher {
    override fun matches(string: String): Boolean = string.startsWith(normalize(expectedString), ignoreCase)
}

class Contains(private val expectedString: String, private val ignoreCase: Boolean) : TestMatcher {
    override fun matches(string: String): Boolean = string.contains(normalize(expectedString), ignoreCase)
}
