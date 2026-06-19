/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.utils.TarExtractionSecurityException
import org.jetbrains.kotlin.gradle.utils.unzipTarGz
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinNativeBundleTarExtractionSecurityTest : WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    // CWE-22: entry name with ../ escapes targetDir.
    @Test
    fun `path traversal entry is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val escapedFile = temporaryFolder.resolve("tar_traversal_KT86605.txt")
        val archive = createTarGz {
            directory("kotlin-native/")
            file("kotlin-native/../../tar_traversal_KT86605.txt", "traversal")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(escapedFile.exists(), "Entry escaped targetDir: $escapedFile")
    }

    // CWE-59: symlink target points outside targetDir.
    @Test
    fun `escaping symlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        temporaryFolder.resolve("outside").createDirectories()
        val symlink = targetDir.resolve("kotlin-native/link")
        val archive = createTarGz {
            directory("kotlin-native/")
            symlink("kotlin-native/link", "../../outside")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(Files.exists(symlink, LinkOption.NOFOLLOW_LINKS), "Escaping symlink was created: $symlink")
    }

    // CWE-59: hardlink target resolves outside targetDir.
    @Test
    fun `escaping hardlink is rejected`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val outsideFile = temporaryFolder.resolve("outside").createDirectories().resolve("hardlink_target.txt")
        outsideFile.writeText("outside")
        val hardlink = targetDir.resolve("kotlin-native/link.txt")
        val archive = createTarGz {
            directory("kotlin-native/")
            hardlink("kotlin-native/link.txt", "../outside/hardlink_target.txt")
        }

        assertThrows<TarExtractionSecurityException> {
            archive.toPath().unzipTarGz(targetDir)
        }
        assertFalse(hardlink.exists(), "Escaping hardlink was created: $hardlink")
    }

    // Control: a symlink that stays inside targetDir should still extract.
    @Test
    fun `intra-boundary symlink extracts successfully`() {
        val targetDir = temporaryFolder.resolve("target").createDirectories()
        val archive = createTarGz {
            directory("kotlin-native/")
            directory("kotlin-native/inside/")
            symlink("kotlin-native/link", "inside")
            file("kotlin-native/link/payload.txt", "ok")
        }

        archive.toPath().unzipTarGz(targetDir)

        val extractedFile = targetDir.resolve("kotlin-native/inside/payload.txt")
        assertTrue(extractedFile.exists())
        assertEquals("ok", extractedFile.toFile().readText())
    }

    private fun createTarGz(build: TarArchiveOutputStream.() -> Unit): File {
        val tarFile = temporaryFolder.resolve("archive.tar.gz").toFile()
        TarArchiveOutputStream(
            GZIPOutputStream(
                BufferedOutputStream(
                    FileOutputStream(tarFile)
                )
            )
        ).use {
            it.apply(build)
        }
        return tarFile
    }

    private fun TarArchiveOutputStream.directory(name: String) {
        putArchiveEntry(TarArchiveEntry(name))
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.file(name: String, contents: String) {
        val bytes = contents.toByteArray()
        val entry = TarArchiveEntry(name)
        entry.size = bytes.size.toLong()
        putArchiveEntry(entry)
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.symlink(name: String, linkName: String) {
        val entry = TarArchiveEntry(name, TarConstants.LF_SYMLINK)
        entry.linkName = linkName
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.hardlink(name: String, linkName: String) {
        val entry = TarArchiveEntry(name, TarConstants.LF_LINK)
        entry.linkName = linkName
        putArchiveEntry(entry)
        closeArchiveEntry()
    }
}
