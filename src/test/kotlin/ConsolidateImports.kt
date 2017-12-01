import io.kotlintest.matchers.shouldBe
import kscript.app.*
import org.junit.Test
import java.io.File
import org.hamcrest.core.Is.*
import org.junit.Assert.*

class ConsolidateImports {

    @Test
    fun test_consolidate_imports() {
        val classLoader = javaClass.classLoader
        val file  = this.javaClass.getResource("/consolidate_includes/input.script").file
        val expected  = this.javaClass.getResource("/consolidate_includes/expected.script")
        val result = resolveIncludes(File(file))

        assertThat(result.readText(),`is`(expected.readText()))
    }
}