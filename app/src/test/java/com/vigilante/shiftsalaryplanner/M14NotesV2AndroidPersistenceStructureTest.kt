package com.vigilante.shiftsalaryplanner

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class M14NotesV2AndroidPersistenceStructureTest {
    private val source = File("src/main/java/com/vigilante/shiftsalaryplanner/feature/notes/persistence/SharedPreferencesNotesV2Backend.kt")

    @Test
    fun `v2 backend is profile scoped separate from legacy and uses synchronous commit`() {
        assertTrue("Missing SharedPreferencesNotesV2Backend.kt", source.isFile)
        val text = source.readText()
        assertTrue(text.contains("profileSharedPreferences(AppNotesStore.PREFS_NAME)"))
        assertTrue(text.contains("notes_v2_json"))
        assertTrue(text.contains(".commit()"))
        assertTrue(text.contains("AppNotesStore(appContext)"))
        assertTrue(text.contains("allNotesSnapshot()"))
        assertTrue(text.contains("NotesV2Store("))
    }
}
