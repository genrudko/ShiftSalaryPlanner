package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.feature.notes.domain.NoteBlock
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaBlockFactory
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaDescriptor
import com.vigilante.shiftsalaryplanner.feature.notes.media.NoteMediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2MediaBlockFactoryTest {
    private fun descriptor(mime: String, ratio: Float = 0f) = NoteMediaDescriptor(
        uri = "file:/notes/media",
        displayName = "sample",
        mimeType = mime,
        byteSize = 42L,
        aspectRatio = ratio,
        createdAtMillis = 123L,
    )

    @Test
    fun `mime maps to first class media block with presentation`() {
        val image = NoteMediaBlockFactory.create("img", descriptor("image/jpeg", 1.5f))
        val video = NoteMediaBlockFactory.create("vid", descriptor("video/mp4", 16f / 9f))
        val audio = NoteMediaBlockFactory.create("aud", descriptor("audio/mp4"))
        val file = NoteMediaBlockFactory.create("file", descriptor("application/pdf"))

        assertTrue(image is NoteBlock.Image)
        assertTrue(video is NoteBlock.Video)
        assertTrue(audio is NoteBlock.Audio)
        assertTrue(file is NoteBlock.FileAttachment)
        assertEquals(1.5f, (image as NoteBlock.Image).presentation.aspectRatio)
        assertEquals(123L, image.presentation.createdAtMillis)
        assertEquals("sample", image.label)
    }

    @Test
    fun `preferred kind wins when provider mime is generic`() {
        val block = NoteMediaBlockFactory.create(
            mediaId = "img",
            descriptor = descriptor("application/octet-stream", 1.25f),
            preferredKind = NoteMediaKind.IMAGE,
        )
        assertTrue(block is NoteBlock.Image)
        assertEquals(1.25f, (block as NoteBlock.Image).presentation.aspectRatio)
    }

    @Test
    fun `unknown mime defaults to generic file`() {
        val block = NoteMediaBlockFactory.create("unknown", descriptor("application/x-custom"))
        assertTrue(block is NoteBlock.FileAttachment)
    }
}
