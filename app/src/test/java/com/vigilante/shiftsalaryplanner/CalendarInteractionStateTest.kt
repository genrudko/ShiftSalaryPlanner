package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarInteractionStateTest {
    private fun state(): CalendarInteractionState = CalendarInteractionState(
        selectedDateState = mutableStateOf(null),
        dayAssignmentsPreviewDateState = mutableStateOf(null),
        quickPickerOpenState = mutableStateOf(false),
        activeBrushCodeState = mutableStateOf(null),
        isLegendExpandedState = mutableStateOf(false),
        calendarWorkplaceFilterIdState = mutableStateOf("main")
    )

    @Test
    fun `quick picker toggles and closes`() {
        val state = state()
        state.toggleQuickPicker()
        assertTrue(state.quickPickerOpen)
        state.closeQuickPicker()
        assertFalse(state.quickPickerOpen)
    }

    @Test
    fun `brush selection and clear are explicit`() {
        val state = state()
        state.selectBrush("N")
        assertEquals("N", state.activeBrushCode)
        state.clearBrush()
        assertNull(state.activeBrushCode)
    }

    @Test
    fun `legend toggle preserves current UI behavior`() {
        val state = state()
        state.toggleLegend()
        assertTrue(state.isLegendExpanded)
        state.toggleLegend()
        assertFalse(state.isLegendExpanded)
    }

    @Test
    fun `selected date and preview date remain independently mutable`() {
        val state = state()
        val selected = LocalDate.of(2026, 9, 11)
        val preview = LocalDate.of(2026, 9, 12)
        state.selectedDate = selected
        state.dayAssignmentsPreviewDate = preview
        assertEquals(selected, state.selectedDate)
        assertEquals(preview, state.dayAssignmentsPreviewDate)
        state.selectedDate = null
        assertNull(state.selectedDate)
        assertEquals(preview, state.dayAssignmentsPreviewDate)
    }

    @Test
    fun `calendar workplace filter remains mutable within calendar feature`() {
        val state = state()
        assertEquals("main", state.calendarWorkplaceFilterId)
        state.calendarWorkplaceFilterId = "all"
        assertEquals("all", state.calendarWorkplaceFilterId)
    }
}
