package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaFileStore
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2MediaStoreTest {
    @Test
    fun `import copies source into managed note directory and returns stable file uri`() {
        val root = Files.createTempDirectory("notes-media").toFile()
        try {
            val store = NoteMediaFileStore(root)
            val source = "image-bytes".encodeToByteArray()
            val stored = store.import(
                noteId = "note-1",
                mediaId = "media-1",
                displayName = "scheme.jpg",
                mimeType = "image/jpeg",
                input = ByteArrayInputStream(source),
            )

            assertEquals("scheme.jpg", stored.displayName)
            assertEquals("image/jpeg", stored.mimeType)
            assertEquals(source.size.toLong(), stored.byteSize)
            assertTrue(store.isManagedUri(stored.uri))
            val file = store.resolveManagedFile(stored.uri)
            assertNotNull(file)
            assertTrue(file!!.isFile)
            assertArrayEquals(source, file.readBytes())
            assertTrue(file.canonicalPath.startsWith(root.canonicalPath))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `ids and names cannot escape managed root`() {
        val root = Files.createTempDirectory("notes-media-safe").toFile()
        try {
            val store = NoteMediaFileStore(root)
            val stored = store.import(
                noteId = "../../note",
                mediaId = "../media",
                displayName = "../../evil.png",
                mimeType = "image/png",
                input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
            )
            val file = store.resolveManagedFile(stored.uri)
            assertNotNull(file)
            assertTrue(file!!.canonicalPath.startsWith(root.canonicalPath + java.io.File.separator))
            assertFalse(file.canonicalPath.contains("../"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `failed import leaves no final or part file`() {
        val root = Files.createTempDirectory("notes-media-fail").toFile()
        try {
            val store = NoteMediaFileStore(root)
            val failing = object : InputStream() {
                private var reads = 0
                override fun read(): Int {
                    if (reads++ > 3) throw IOException("boom")
                    return 7
                }
            }
            runCatching {
                store.import("note", "broken", "broken.bin", "application/octet-stream", failing)
            }
            assertTrue(root.walkTopDown().filter { it.isFile }.toList().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `recording target lives in managed root and can be resolved after file exists`() {
        val root = Files.createTempDirectory("notes-audio").toFile()
        try {
            val store = NoteMediaFileStore(root)
            val target = store.createRecordingTarget("note-audio", "voice-1")
            assertTrue(target.parentFile?.isDirectory == true)
            target.writeBytes(byteArrayOf(4, 5, 6))
            val uri = target.toURI().toString()
            assertTrue(store.isManagedUri(uri))
            assertEquals(target.canonicalFile, store.resolveManagedFile(uri)?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }
}
