package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPatternWorkflowStateTest {
    @Test
    fun `create and edit pattern set editor identity`() {
        val state = CalendarPatternWorkflowState()
        state.beginCreatePattern()
        assertTrue(state.showPatternEditDialog)
        assertNull(state.editingPatternId)
        state.closePatternEditor()
        state.beginEditPattern("pattern-7")
        assertTrue(state.showPatternEditDialog)
        assertEquals("pattern-7", state.editingPatternId)
    }

    @Test
    fun `apply pattern opens and cancel clears dialog identity`() {
        val state = CalendarPatternWorkflowState()
        state.beginApplyPattern("pattern-3")
        assertTrue(state.showPatternApplyDialog)
        assertEquals("pattern-3", state.applyingPatternId)
        state.closePatternApply()
        assertFalse(state.showPatternApplyDialog)
        assertNull(state.applyingPatternId)
    }

    @Test
    fun `select quick pattern activates pattern clears range and closes picker`() {
        val state = CalendarPatternWorkflowState().apply {
            showPatternQuickPicker = true
            patternRangeStartIso = "2026-09-10"
        }
        state.selectQuickPattern("night-cycle")
        assertEquals("night-cycle", state.activePatternId)
        assertNull(state.patternRangeStartIso)
        assertFalse(state.showPatternQuickPicker)
    }

    @Test
    fun `open manager from quick picker swaps modal ownership`() {
        val state = CalendarPatternWorkflowState().apply { showPatternQuickPicker = true }
        state.openPatternManagerFromQuickPicker()
        assertFalse(state.showPatternQuickPicker)
        assertTrue(state.showPatternListDialog)
    }

    @Test
    fun `begin clear range clears pattern and pending clear selection`() {
        val state = CalendarPatternWorkflowState().apply {
            activePatternId = "pattern"
            patternRangeStartIso = "2026-09-01"
            pendingPatternRangeStartIso = "2026-09-02"
            pendingPatternRangeEndIso = "2026-09-03"
            clearRangeStartIso = "2026-09-04"
            pendingClearRangeStartIso = "2026-09-05"
            pendingClearRangeEndIso = "2026-09-06"
        }
        state.beginClearRangeMode()
        assertTrue(state.clearRangeModeActive)
        assertNull(state.activePatternId)
        assertNull(state.patternRangeStartIso)
        assertNull(state.pendingPatternRangeStartIso)
        assertNull(state.pendingPatternRangeEndIso)
        assertNull(state.clearRangeStartIso)
        assertNull(state.pendingClearRangeStartIso)
        assertNull(state.pendingClearRangeEndIso)
    }

    @Test
    fun `reset clear range clears all clear selection state`() {
        val state = CalendarPatternWorkflowState().apply {
            clearRangeModeActive = true
            clearRangeStartIso = "2026-09-01"
            pendingClearRangeStartIso = "2026-09-02"
            pendingClearRangeEndIso = "2026-09-03"
        }
        state.resetClearRangeMode()
        assertFalse(state.clearRangeModeActive)
        assertNull(state.clearRangeStartIso)
        assertNull(state.pendingClearRangeStartIso)
        assertNull(state.pendingClearRangeEndIso)
    }

    @Test
    fun `close preview clears only preview and pending pattern range`() {
        val state = CalendarPatternWorkflowState().apply {
            showPatternPreviewDialog = true
            activePatternId = "pattern"
            patternRangeStartIso = "2026-09-01"
            pendingPatternRangeStartIso = "2026-09-02"
            pendingPatternRangeEndIso = "2026-09-03"
        }
        state.closePatternPreview()
        assertFalse(state.showPatternPreviewDialog)
        assertNull(state.pendingPatternRangeStartIso)
        assertNull(state.pendingPatternRangeEndIso)
        assertEquals("pattern", state.activePatternId)
        assertEquals("2026-09-01", state.patternRangeStartIso)
    }

    @Test
    fun `saveable representation restores every workflow field`() {
        val original = CalendarPatternWorkflowState().apply {
            showPatternListDialog = true
            showPatternEditDialog = true
            editingPatternId = "edit,id"
            showPatternApplyDialog = true
            applyingPatternId = "apply"
            showPatternQuickPicker = true
            activePatternId = "active"
            patternRangeStartIso = "2026-09-01"
            pendingPatternRangeStartIso = "2026-09-02"
            pendingPatternRangeEndIso = "2026-09-03"
            showPatternPreviewDialog = true
            clearRangeModeActive = true
            clearRangeStartIso = "2026-09-04"
            pendingClearRangeStartIso = "2026-09-05"
            pendingClearRangeEndIso = "2026-09-06"
            showClearMonthConfirm = true
            showClearAllCalendarConfirm = true
        }
        val restored = restoreCalendarPatternWorkflowState(original.toSaveableStrings())
        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
    }
}
