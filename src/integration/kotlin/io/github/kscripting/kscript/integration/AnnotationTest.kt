package io.github.kscripting.kscript.integration

import io.github.kscripting.kscript.integration.tools.TestAssertion.any
import io.github.kscripting.kscript.integration.tools.TestAssertion.startsWith
import io.github.kscripting.kscript.integration.tools.TestAssertion.verify
import io.github.kscripting.kscript.integration.tools.TestContext.projectDir
import io.github.kscripting.kscript.integration.tools.TestContext.resolvePath
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

class AnnotationTest : TestBase {
    @Test
    @Tag("posix")
    @Tag("windows")
    fun `There are some dependencies which are not jar, but maybe pom, aar and others - make sure they work, too`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/depends_on_with_type.kts")}", 0, "getBigDecimal(1L): 1\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that DependsOn is parsed correctly`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/depends_on_annot.kts")}", 0, "kscript with annotations rocks!\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that DependsOnMaven is parsed correctly`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/depends_on_maven_annot.kts")}", 0, "kscript with annotations rocks!\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Make sure that dynamic versions are matched properly`() {
        verify("kscript ${resolvePath("$projectDir/test/resources/depends_on_dynamic.kts")}", 0, "dynamic kscript rocks!\n", any())
    }

    @Test
    @Tag("posix")
    //TODO: @Tag("windows") - batch file doesn't pass correctly error exitCode
    fun `Make sure that MavenRepository is parsed correctly`() {
        verify(
            "kscript ${resolvePath("$projectDir/test/resources/custom_mvn_repo_annot.kts")}",
            0,
            "kscript with annotations rocks!\n",
            startsWith("[kscript] Adding repository: Repository(id=, url=http://maven.imagej.net/content/repositories/releases, user=, password=)\n")
        )
        verify(
            "kscript ${resolvePath("$projectDir/test/resources/illegal_depends_on_arg.kts")}",
            1,
            "",
            "[kscript] [ERROR] Artifact locators must be provided as separate annotation arguments and not as comma-separated list: [com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0]\n"
        )
        verify("kscript $projectDir/test/resources/script_with_compile_flags.kts", 0, "hoo_ray\n", any())
    }

    @Test
    @Tag("posix")
    @Tag("windows")
    fun `Ensure dependencies are solved correctly #345`() {
        val dependencyDirectory = File(System.getProperty("user.home") + "/.m2/repository/com/beust")
        if (dependencyDirectory.exists()) {
            FileUtils.cleanDirectory(dependencyDirectory)
        }

        verify(
            "kscript ${resolvePath("$projectDir/test/resources/depends_on_klaxon.kts")}",
            0,
            "Successfully resolved klaxon\n",
            startsWith("[kscript] Resolving com.beust:klaxon:5.5...\n")
        )
    }
}
