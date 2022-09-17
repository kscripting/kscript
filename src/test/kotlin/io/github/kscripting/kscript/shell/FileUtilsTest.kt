package io.github.kscripting.kscript.shell

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.apache.commons.io.FileUtils as ApacheFileUtils

@Disabled
class FileUtilsTest {
    private val path = OsPath.createOrThrow(OsType.native, "build/tmp/file_utils_test")

    @BeforeEach
    fun setUp() {
        ApacheFileUtils.deleteDirectory(path.toNativeFile())
    }

    @Test
    fun `Test create file`() {
        FileUtils.createFile(path.resolve("test1"), "Test")
    }

    @Test
    fun `Test symlink file`() {
        FileUtils.createFile(path.resolve("test1"), "Test")
        FileUtils.symLinkOrCopy(path.resolve("test2"), path.resolve("test1"))
    }

    @Test
    fun `Create dirs if needed`() {
        FileUtils.createFile(path.resolve("test1"), "Test")
        FileUtils.symLinkOrCopy(path.resolve("test2"), path.resolve("test1"))
    }
}
