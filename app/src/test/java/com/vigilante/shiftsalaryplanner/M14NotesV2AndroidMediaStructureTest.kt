package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2AndroidMediaStructureTest {
    @Test
    fun `notes file provider exposes only notes media roots`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val paths = File("src/main/res/xml/notes_file_paths.xml").readText()
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("${'$'}{applicationId}.notes-files"))
        assertTrue(paths.contains("note_media_v2/"))
        assertTrue(paths.contains("note_audio/"))
        assertFalse(paths.contains("path=\".\""))
    }

    @Test
    fun `android editor injects lifecycle safe interactive media renderer`() {
        val editor = File("src/main/java/com/vigilante/shiftsalaryplanner/feature/notes/ui/NotesV2AndroidEditorScreen.kt").readText()
        val media = File("src/main/java/com/vigilante/shiftsalaryplanner/feature/notes/ui/NotesV2AndroidMediaBlock.kt").readText()
        assertTrue(editor.contains("mediaBlockContent = { block -> NotesV2AndroidMediaBlock(block) }"))
        assertTrue(media.contains("MediaPlayer"))
        assertTrue(media.contains("VideoView"))
        assertTrue(media.contains("DisposableEffect"))
        assertTrue(media.contains("FileProvider.getUriForFile"))
    }
}
