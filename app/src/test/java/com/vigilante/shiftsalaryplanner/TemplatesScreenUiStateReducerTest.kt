package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatesScreenUiStateReducerTest {

    @Test
    fun `set pending delete id updates state`() {
        val initial = TemplatesScreenUiState()

        val updated = reduceTemplatesScreenUiState(
            state = initial,
            action = TemplatesScreenUiAction.SetPendingDeletePatternId("pattern-1")
        )

        assertEquals("pattern-1", updated.pendingDeletePatternId)
    }

    @Test
    fun `set show system statuses updates state`() {
        val initial = TemplatesScreenUiState(showSystemStatuses = false)

        val updated = reduceTemplatesScreenUiState(
            state = initial,
            action = TemplatesScreenUiAction.SetShowSystemStatuses(true)
        )

        assertTrue(updated.showSystemStatuses)
    }
}
