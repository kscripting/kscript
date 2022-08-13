package kscript.app.shell

import kscript.app.model.OsType
import java.io.File
import java.nio.charset.Charset
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

//Path is always native, so implicit conversion to native path
fun OsPath.toNativePath(): Path = Paths.get(toNativeOsPath().stringPath())

fun OsPath.toNativeOsPath() = if (osType.isPosixHostedOnWindows()) convert(OsType.WINDOWS) else this

fun OsPath.exists() = toNativePath().exists()

fun OsPath.createDirectories(): OsPath = OsPath.createOrThrow(nativeType, toNativePath().createDirectories().pathString)

fun OsPath.writeText(text: CharSequence, charset: Charset = Charsets.UTF_8, vararg options: OpenOption): Unit =
    toNativePath().writeText(text, charset, *options)

fun OsPath.readText(charset: Charset = Charsets.UTF_8): String = toNativePath().readText(charset)

fun OsPath.toNativeFile(): File = toNativePath().toFile()

fun OsPath.copyTo(target: OsPath, overwrite: Boolean = false): OsPath =
    OsPath.createOrThrow(nativeType, toNativePath().copyTo(target.toNativePath(), overwrite).pathString)

fun File.toOsPath() : OsPath = OsPath.createOrThrow(OsType.native, absolutePath)

fun Path.toOsPath(): OsPath = OsPath.createOrThrow(OsType.native, absolutePathString())

val OsPath.leaf
    get() = if (pathParts.isEmpty()) "" else pathParts.last()

val OsPath.root
    get() = if (pathParts.isEmpty()) "" else pathParts.first()

val OsPath.rootOsPath
    get() = OsPath.createOrThrow(osType, root)

val OsPath.parent
    get() = toNativePath().parent

val OsPath.nativeType
    get() = if (osType.isPosixHostedOnWindows()) OsType.WINDOWS else osType

val OsPath.extension
    get() = leaf.substringAfterLast('.', "")
