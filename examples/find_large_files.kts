#!/usr/bin/env kscript
@file:DependsOn("com.github.ajalt:clikt:3.2.0") // For command line parsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FindLargeFiles : CliktCommand(help = "Finds files larger than a specified size and optionally zips them.") {
    val directory: Path by option("-d", "--directory", help = "Directory to search (default: current directory)")
        .path(mustExist = true, canBeFile = false, mustBeReadable = true)
        .default(Path.of("."))

    val minSize: Long by option("-s", "--size", help = "Minimum file size in MB")
        .long()
        .default(100L)

    val archivePath: Path? by option("-a", "--archive", help = "Optional ZIP file path to archive found files")
        .path(canBeDir = false) // Removed mustBeWritable as it causes issues if file doesn't exist for parent dir check

    val quiet: Boolean by option("-q", "--quiet", help = "Suppress listing of found files").flag(default = false)

    val force: Boolean by option("-f", "--force", help="Force overwrite of existing archive").flag(default = false)


    override fun run() {
        val minSizeBytes = minSize * 1024 * 1024
        if (!quiet) {
            echo("Searching for files larger than $minSize MB ($minSizeBytes bytes) in directory: $directory")
        }

        val largeFiles = mutableListOf<File>()

        directory.toFile().walkTopDown().forEach { file ->
            if (file.isFile && file.length() >= minSizeBytes) {
                largeFiles.add(file)
                if (!quiet) {
                    echo("Found: ${file.absolutePath} (${file.length() / (1024 * 1024)} MB)")
                }
            }
        }

        if (largeFiles.isEmpty()) {
            if (!quiet) {
                echo("No files found larger than $minSize MB.")
            }
            return
        }

        if (!quiet) {
            echo("\nFound ${largeFiles.size} file(s) larger than $minSize MB.")
        }

        archivePath?.let { zipPath ->
            if (Files.exists(zipPath) && !force) {
                 // Simplified prompt for non-interactive, or use a different Clikt mechanism for actual prompting if needed
                 val shouldOverwrite = System.getenv("KSCRIPT_OVERWRITE_ARCHIVE") == "true" // Example: control via env var
                 if (!shouldOverwrite) {
                    echo("Archive '$zipPath' exists. Overwrite not forced and not confirmed. Archiving cancelled.", err = true)
                    return
                 }
            }

            if (!quiet) {
                echo("Archiving ${largeFiles.size} file(s) to $zipPath ...")
            }
            try {
                ZipOutputStream(Files.newOutputStream(zipPath)).use { zos ->
                    largeFiles.forEach { file ->
                        // Ensure relative paths for zip entries if directory is not current
                        val entryPath = if (directory.toFile().absolutePath == Path.of(".").toFile().absolutePath) {
                            file.toPath() // Use relative path if searching "."
                        } else {
                            directory.relativize(file.toPath())
                        }
                        val zipEntry = ZipEntry(entryPath.toString())
                        zos.putNextEntry(zipEntry)
                        Files.copy(file.toPath(), zos)
                        zos.closeEntry()
                        if (!quiet) {
                            echo("  Added: ${file.name}")
                        }
                    }
                }
                if (!quiet) {
                    echo("Archiving complete: $zipPath")
                }
            } catch (e: Exception) {
                echo("Error during archiving: ${e.message}", err = true)
                // e.printStackTrace() // for more detailed debug if necessary
            }
        }
    }
}

fun main(args: Array<String>) = FindLargeFiles().main(args)
