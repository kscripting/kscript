import kscript.app.main
import org.junit.Test

class KscriptTest {

    @Test
    fun `should run a piped script`() {
        main(arrayOf("println(\"kotlin rocks\")"))
    }

    @Test
    fun `should run a script with only println`() {
        main(arrayOf("test/resources/dot.Test.kts"))
    }

    @Test
    fun `should run a script with multiple dependencies as comments`() {
        main(arrayOf("test/resources/multi_line_deps.kts"))
    }

    @Test
    fun `should run a script with dependencies as annotations`() {
        main(arrayOf("test/resources/depends_on_with_type.kts"))
    }

}
