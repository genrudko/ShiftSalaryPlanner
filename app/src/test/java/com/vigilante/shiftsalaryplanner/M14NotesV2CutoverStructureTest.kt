package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2CutoverStructureTest {
    private val main = File("src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").readText()
    private val wear = File("src/main/java/com/vigilante/shiftsalaryplanner/wearsync/WearSyncBridge.kt").readText()

    @Test
    fun `phone notes root and editor cut over to v2 repository`() {
        assertTrue(main.contains("createNotesV2Repository("))
        assertTrue(main.contains("NotesV2LibraryScreen("))
        assertTrue(main.contains("NotesV2AndroidEditorScreen("))
        assertTrue(main.contains("notesV2Repository.save("))
        assertTrue(main.contains("notesV2Repository.moveToTrash("))
        assertFalse(main.contains("AppNotesTabScreen("))
        assertFalse(main.contains("AppNoteEditorScreen("))
    }

    @Test
    fun `legacy assistant and wear note creation write through v2 authority`() {
        assertTrue(main.contains("notesV2Repository.saveLegacy("))
        assertTrue(wear.contains("createNotesV2Repository(appContext).saveLegacy("))
        assertTrue(wear.split("createNotesV2Repository(appContext).saveLegacy(").size - 1 >= 2)
    }

    @Test
    fun `legacy calendar projection remains available during cutover`() {
        assertTrue(main.contains("notesData.notesForDate(date)"))
        assertTrue(main.contains("val appNotes by notesData.notes.collectAsState"))
    }
}
