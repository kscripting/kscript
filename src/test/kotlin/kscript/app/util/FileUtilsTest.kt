package kscript.app.util

import kscript.app.model.OsType
import kscript.app.shell.FileUtils
import kscript.app.shell.OsPath
import kscript.app.shell.toNativeFile
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
