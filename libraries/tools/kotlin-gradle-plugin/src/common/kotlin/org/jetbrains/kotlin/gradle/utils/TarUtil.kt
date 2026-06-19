/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.zip.GZIPInputStream
import kotlin.io.path.inputStream
import kotlin.io.use

internal fun Path.unzipTarGz(destinationDirectory: Path) {
    val targetBase = destinationDirectory.normalize().toAbsolutePath()
    GZIPInputStream(BufferedInputStream(inputStream())).use { gzipInputStream ->
        val hardLinks = HashMap<Path, Path>()

        TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
            generateSequence {
                tarInputStream.nextEntry
            }.forEach { entry: TarArchiveEntry ->
                val outputPath = validateOutputPath(targetBase, entry.name)
                val outputFile = outputPath.toFile()
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    if (entry.isSymbolicLink) {
                        validateSymlinkTarget(targetBase, outputPath, entry.linkName)
                        outputFile.parentFile?.mkdirs()
                        Files.createSymbolicLink(outputPath, Paths.get(entry.linkName))
                    } else if (entry.isLink) {
                        hardLinks[outputPath] = validateHardlinkTarget(targetBase, entry.linkName)
                    } else {
                        outputFile.parentFile?.mkdirs()
                        outputFile.outputStream().use {
                            tarInputStream.copyTo(it)
                        }
                        if (supportsPosixFilePermissions) {
                            Files.setPosixFilePermissions(outputPath, getPosixFilePermissions(entry.mode))
                        }
                    }
                }
            }
        }
        hardLinks.forEach {
            Files.createLink(it.key, it.value)
        }
    }
}

internal class TarExtractionSecurityException(message: String) : IOException(message)

private val supportsPosixFilePermissions: Boolean by lazy {
    FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
}

private fun validateOutputPath(targetBase: Path, entryName: String): Path {
    val outputPath = targetBase.resolve(entryName).normalize()
    requireInsideTarget(targetBase, outputPath, "Entry '$entryName'")
    return outputPath
}

private fun validateSymlinkTarget(targetBase: Path, outputPath: Path, linkName: String) {
    val targetPath = requireNotNull(outputPath.parent) {
        "Symlink '${outputPath.fileName}' has no parent"
    }.resolve(linkName).normalize()
    requireInsideTarget(targetBase, targetPath, "Symlink '${outputPath.fileName}' -> '$linkName'")
}

// TAR hardlink targets are archive-root-relative, not link-location-relative.
private fun validateHardlinkTarget(targetBase: Path, linkName: String): Path {
    val targetPath = targetBase.resolve(linkName).normalize()
    requireInsideTarget(targetBase, targetPath, "Hardlink target '$linkName'")
    return targetPath
}

private fun requireInsideTarget(targetBase: Path, candidate: Path, description: String) {
    if (!candidate.startsWith(targetBase)) {
        throw TarExtractionSecurityException("$description escapes target directory")
    }
}

private fun getPosixFilePermissions(mode: Int): Set<PosixFilePermission> {
    val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

    // adding owner permissions
    permissions.addPermission(mode, 0b100_000_000, PosixFilePermission.OWNER_READ)
    permissions.addPermission(mode, 0b010_000_000, PosixFilePermission.OWNER_WRITE)
    permissions.addPermission(mode, 0b001_000_000, PosixFilePermission.OWNER_EXECUTE)

    // adding group permissions
    permissions.addPermission(mode, 0b000_100_000, PosixFilePermission.GROUP_READ)
    permissions.addPermission(mode, 0b000_010_000, PosixFilePermission.GROUP_WRITE)
    permissions.addPermission(mode, 0b000_001_000, PosixFilePermission.GROUP_EXECUTE)

    // adding other permissions
    permissions.addPermission(mode, 0b000_000_100, PosixFilePermission.OTHERS_READ)
    permissions.addPermission(mode, 0b000_000_010, PosixFilePermission.OTHERS_WRITE)
    permissions.addPermission(mode, 0b000_000_001, PosixFilePermission.OTHERS_EXECUTE)

    return permissions
}

private fun MutableSet<PosixFilePermission>.addPermission(mode: Int, permissionBitMask: Int, permission: PosixFilePermission) {
    if ((mode and permissionBitMask) > 0) {
        add(permission)
    }
}
