package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftFeatureStateTest {
    @Test
    fun `mode changes preserve selected templates mode`() {
        val state = ShiftFeatureState()
        state.setMode(TemplateMode.CYCLES)
        assertEquals(TemplateMode.CYCLES.name, state.templateModeName)
        state.setMode(TemplateMode.SHIFTS)
        assertEquals(TemplateMode.SHIFTS.name, state.templateModeName)
    }

    @Test
    fun `new shift opens clean non system editor`() {
        val state = ShiftFeatureState(editingShiftTemplateCode = "OLD", creatingSystemStatus = true)
        state.openNewShift()
        assertNull(state.editingShiftTemplateCode)
        assertFalse(state.creatingSystemStatus)
    }

    @Test
    fun `new system status opens clean system editor`() {
        val state = ShiftFeatureState(editingShiftTemplateCode = "OLD", creatingSystemStatus = false)
        state.openNewSystemStatus()
        assertNull(state.editingShiftTemplateCode)
        assertTrue(state.creatingSystemStatus)
    }

    @Test
    fun `existing shift keeps exact editor identity and status kind`() {
        val state = ShiftFeatureState()
        state.openExistingShift("N", isSystemStatus = false)
        assertEquals("N", state.editingShiftTemplateCode)
        assertFalse(state.creatingSystemStatus)
        state.openExistingShift("VAC", isSystemStatus = true)
        assertEquals("VAC", state.editingShiftTemplateCode)
        assertTrue(state.creatingSystemStatus)
    }

    @Test
    fun `save cleanup preserves system status until special rule save finishes`() {
        val state = ShiftFeatureState(editingShiftTemplateCode = "VAC", creatingSystemStatus = true)

        state.clearEditingCode()
        assertNull(state.editingShiftTemplateCode)
        assertTrue(state.creatingSystemStatus)

        state.finishSpecialRuleSave()
        assertFalse(state.creatingSystemStatus)
    }

    @Test
    fun `clear editor clears code and system status flag`() {
        val state = ShiftFeatureState(editingShiftTemplateCode = "VAC", creatingSystemStatus = true)
        state.clearEditor()
        assertNull(state.editingShiftTemplateCode)
        assertFalse(state.creatingSystemStatus)
    }

    @Test
    fun `saveable representation restores all three fields`() {
        val original = ShiftFeatureState(
            editingShiftTemplateCode = "VAC",
            creatingSystemStatus = true,
            templateModeName = TemplateMode.CYCLES.name
        )
        val restored = restoreShiftFeatureState(original.toSaveableStrings())
        assertEquals(original.toSaveableStrings(), restored.toSaveableStrings())
    }
}
