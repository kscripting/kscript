package io.github.kscripting.kscript.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.kscripting.shell.model.OsPath
import io.github.kscripting.shell.model.OsType
import io.github.kscripting.shell.model.readText
import io.github.kscripting.shell.model.toNativeFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.apache.commons.io.FileUtils as ApacheFileUtils

class FileUtilsTest {
    private val path = OsPath.createOrThrow(OsType.native, "build/tmp/file_utils_test")
    private val newFile1 = path.resolve("test1.txt")
    private val newFile2 = path.resolve("test2.txt")
    private val newFile3 = path.resolve("firstDir", "secondDir", "test3.txt")
    private val newFile4 = path.resolve("dir1", "dir2", "test4.txt")
    private val content1 = "Test\nContent\n"

    @BeforeEach
    fun setUp() {
        ApacheFileUtils.deleteDirectory(path.toNativeFile())
    }

    @Test
    fun `Test create file`() {
        FileUtils.createFile(newFile1, content1)
        assertThat(newFile1.readText()).isEqualTo(content1)
    }

    @Test
    //TODO: re-enable it
    @Disabled
    fun `Test symlink file`() {
        FileUtils.createFile(newFile1, content1)
        FileUtils.symLinkOrCopy(newFile2, newFile1)

        assertThat(newFile2.readText()).isEqualTo(content1)
    }

    @Test
    //TODO: re-enable it
    @Disabled
    fun `Create dirs if needed`() {
        FileUtils.createFile(newFile3, content1)
        FileUtils.symLinkOrCopy(newFile4, newFile3)

        assertThat(newFile4.readText()).isEqualTo(content1)
    }

    @Test
    fun `Assert that getArtifactsRecursively finds all the artifacts in the path`() {
        val artifactsPath = OsPath.createOrThrow(OsType.native, "test/resources/config/jarfiles/")

        assertThat(FileUtils.getArtifactsRecursively(artifactsPath)).transform {
            it.map { it.stringPath().substringAfterLast("jarfiles").replace("\\", "/") }
        }.isEqualTo(listOf("/jar_file_1.jar", "/subdir/jar_file_2.jar"))
    }
}
