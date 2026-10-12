package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.backup.NotesV2BackupPackageCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2BackupPackageTest {
    @Test
    fun `zip package roundtrip carries json and note media without base64`() {
        val source = Files.createTempDirectory("notes-v2-media-src").toFile()
        val noteDir = source.resolve("n1").apply { mkdirs() }
        noteDir.resolve("photo.jpg").writeBytes(byteArrayOf(1, 2, 3, 4))
        noteDir.resolve("voice.m4a").writeBytes(byteArrayOf(9, 8, 7))
        val json = "{\"version\":1,\"marker\":\"notes-v2\"}"
        val output = ByteArrayOutputStream()

        NotesV2BackupPackageCodec.write(output, json, source)
        val raw = output.toByteArray()
        assertTrue(raw[0] == 'P'.code.toByte() && raw[1] == 'K'.code.toByte())
        assertFalse(raw.toString(Charsets.ISO_8859_1).contains("AQIDBA=="))

        val restoredRoot = Files.createTempDirectory("notes-v2-media-dst").toFile().resolve("note_media_v2")
        val restoredJson = NotesV2BackupPackageCodec.readJsonAndRestoreMedia(ByteArrayInputStream(raw), restoredRoot)

        assertEquals(json, restoredJson)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), restoredRoot.resolve("n1/photo.jpg").readBytes())
        assertArrayEquals(byteArrayOf(9, 8, 7), restoredRoot.resolve("n1/voice.m4a").readBytes())
    }

    @Test
    fun `legacy json import remains readable and does not replace existing media`() {
        val restoredRoot = Files.createTempDirectory("notes-v2-legacy").toFile().resolve("note_media_v2")
        restoredRoot.mkdirs()
        restoredRoot.resolve("keep.bin").writeBytes(byteArrayOf(5, 4, 3))
        val json = "{\"version\":1}"

        val restoredJson = NotesV2BackupPackageCodec.readJsonAndRestoreMedia(
            ByteArrayInputStream(json.toByteArray()),
            restoredRoot,
        )

        assertEquals(json, restoredJson)
        assertArrayEquals(byteArrayOf(5, 4, 3), restoredRoot.resolve("keep.bin").readBytes())
    }

    @Test
    fun `local app backup exports media package while restore still accepts legacy json`() {
        val main = File("src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
        val coordinator = File("src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/backup/BackupRestoreCoordinator.kt").readText()
        assertTrue(main.contains("CreateDocument(\"application/zip\")"))
        assertTrue(main.contains("NotesV2BackupPackageCodec.write("))
        assertTrue(main.contains("ShiftSalaryPlanner_backup_\${LocalDate.now()}.zip"))
        assertTrue(coordinator.contains("NotesV2BackupPackageCodec.prepareJsonAndMedia("))
    }

    @Test
    fun `google drive backup writes zip and still discovers legacy json`() {
        val drive = File("src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/backup/GoogleDriveBackupSync.kt").readText()
        assertTrue(drive.contains("shift_salary_planner_backup.zip"))
        assertTrue(drive.contains("application/zip"))
        assertTrue(drive.contains("NotesV2BackupPackageCodec.write("))
        assertTrue(drive.contains("shift_salary_planner_backup.json"))
        assertTrue(drive.contains("NotesV2BackupPackageCodec.prepareJsonAndMedia("))
    }

    @Test
    fun `prepared zip does not change live media until explicit installation`() {
        val source = Files.createTempDirectory("notes-v2-staged-src").toFile()
        source.resolve("new.bin").writeBytes(byteArrayOf(1, 2, 3))
        val mediaRoot = Files.createTempDirectory("notes-v2-staged-dst").toFile().resolve("note_media_v2")
        mediaRoot.mkdirs()
        mediaRoot.resolve("existing.bin").writeBytes(byteArrayOf(7, 8))
        val output = ByteArrayOutputStream()
        NotesV2BackupPackageCodec.write(output, "{}", source)

        NotesV2BackupPackageCodec.prepareJsonAndMedia(ByteArrayInputStream(output.toByteArray()), mediaRoot).use { staged ->
            assertEquals("{}", staged.backupJson)
            assertTrue(mediaRoot.resolve("existing.bin").exists())
            assertFalse(mediaRoot.resolve("new.bin").exists())
            staged.install()
            assertTrue(mediaRoot.resolve("new.bin").exists())
            assertArrayEquals(byteArrayOf(7, 8), mediaRoot.resolve("existing.bin").readBytes())
        }
    }

    @Test
    fun `discarding prepared zip keeps existing media untouched`() {
        val source = Files.createTempDirectory("notes-v2-abort-src").toFile()
        source.resolve("new.bin").writeBytes(byteArrayOf(1))
        val mediaRoot = Files.createTempDirectory("notes-v2-abort-dst").toFile().resolve("note_media_v2")
        mediaRoot.mkdirs()
        mediaRoot.resolve("existing.bin").writeBytes(byteArrayOf(2))
        val output = ByteArrayOutputStream()
        NotesV2BackupPackageCodec.write(output, "{}", source)

        NotesV2BackupPackageCodec.prepareJsonAndMedia(ByteArrayInputStream(output.toByteArray()), mediaRoot).use {
            assertEquals("{}", it.backupJson)
            // Simulate database restore failure: never commit staged media.
        }
        assertArrayEquals(byteArrayOf(2), mediaRoot.resolve("existing.bin").readBytes())
        assertFalse(mediaRoot.resolve("new.bin").exists())
    }

    @Test
    fun `invalid backup json is rejected before replacing live notes media`() {
        val parent = Files.createTempDirectory("notes-v2-corrupt-data").toFile()
        val restoredRoot = parent.resolve("note_media_v2").apply { mkdirs() }
        restoredRoot.resolve("existing.txt").writeText("keep")
        val raw = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("note_media_v2/new.txt"))
                zip.write("do-not-install".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val result = runCatching {
            NotesV2BackupPackageCodec.readJsonAndRestoreMedia(
                input = ByteArrayInputStream(raw),
                mediaRoot = restoredRoot,
                validateJson = { parseAppBackupJson(it) },
            )
        }

        assertTrue(result.isFailure)
        assertEquals("keep", restoredRoot.resolve("existing.txt").readText())
        assertFalse(restoredRoot.resolve("new.txt").exists())
    }

    @Test
    fun `malicious zip path cannot escape media restore root`() {
        val parent = Files.createTempDirectory("notes-v2-zipslip").toFile()
        val restoredRoot = parent.resolve("note_media_v2")
        val raw = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write("{\"version\":1}".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("note_media_v2/../../escaped.txt"))
                zip.write("bad".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val result = runCatching {
            NotesV2BackupPackageCodec.readJsonAndRestoreMedia(ByteArrayInputStream(raw), restoredRoot)
        }

        assertTrue(result.isFailure)
        assertFalse(parent.resolve("escaped.txt").exists())
    }
}
