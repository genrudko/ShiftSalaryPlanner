package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceWorkflowStateTest {
    @Test fun `backup export staging and clear keep legacy filename behavior`() {
        val state = ServiceWorkflowState()
        state.stageBackupExport("{json}", "backup-2026.json")
        assertEquals("{json}", state.pendingBackupJsonContent)
        assertEquals("backup-2026.json", state.pendingBackupFileName)
        state.clearBackupPayload()
        assertNull(state.pendingBackupJsonContent)
        assertEquals("backup-2026.json", state.pendingBackupFileName)
    }

    @Test fun `excel workflow stages file candidates and preview without mixing payloads`() {
        val state = ServiceWorkflowState()
        val bytes = byteArrayOf(1, 2, 3)
        state.stageExcelFile(bytes, "tabel.xlsm", "selected")
        assertArrayEquals(bytes, state.pendingExcelFileBytes)
        assertEquals("tabel.xlsm", state.pendingExcelFileName)
        assertEquals("selected", state.excelImportStatusMessage)

        state.stageExcelCandidates(listOf(ExcelPersonCandidate("Ivanov I.I.")), "choose")
        assertNull(state.excelImportPreview)
        assertEquals(1, state.excelImportCandidates.size)
        assertEquals("choose", state.excelImportStatusMessage)

        val preview = ExcelImportPreview("Ivanov I.I.", 2026, emptyList(), emptyList(), emptyList(), emptyList())
        state.stageExcelPreview(preview, "preview")
        assertEquals(preview, state.excelImportPreview)
        assertTrue(state.excelImportCandidates.isEmpty())
        assertEquals("preview", state.excelImportStatusMessage)
    }

    @Test fun `saveable restore keeps statuses but resets transient payloads`() {
        val original = ServiceWorkflowState()
        original.openPostUpdateCheck()
        original.stageExcelFile(byteArrayOf(7), "saved.xlsm", "excel status")
        original.updateBackupStatus("backup status")
        original.markAutoUploadChecked("account@example")
        original.stageBackupExport("payload", "custom.json")
        original.stageExcelCandidates(listOf(ExcelPersonCandidate("Candidate")), "candidate status")

        val restored = restoreServiceWorkflowState(original.toSaveableStrings())
        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
        assertNull(restored.pendingBackupJsonContent)
        assertEquals("ShiftSalaryPlanner_backup.json", restored.pendingBackupFileName)
        assertNull(restored.pendingExcelFileBytes)
        assertNull(restored.excelImportPreview)
        assertTrue(restored.excelImportCandidates.isEmpty())
        assertNull(restored.googleSignedInAccount)
    }

    @Test fun `widget runtime refresh token starts clean and increments explicitly`() {
        val state = WidgetSettingsRuntimeState()
        assertEquals(0, state.refreshToken)
        state.refresh()
        state.refresh()
        assertEquals(2, state.refreshToken)
    }

    @Test fun `account and auto upload bookkeeping remain independent`() {
        val state = ServiceWorkflowState()
        state.markAutoUploadChecked("account@example")
        assertEquals("account@example", state.autoUploadCheckedForAccount)
        assertNull(state.googleSignedInAccount)
        state.clearAutoUploadCheck()
        assertEquals("", state.autoUploadCheckedForAccount)
        assertNull(state.googleSignedInAccount)
        assertFalse(state.showPostUpdateCheckDialog)
    }
}
