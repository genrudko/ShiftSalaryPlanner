package com.vigilante.shiftsalaryplanner

import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollTabUiStateReducerTest {

    @Test
    fun `set view mode updates state`() {
        val initial = PayrollTabUiState(viewMode = PayrollViewMode.DETAILED)

        val updated = reducePayrollTabUiState(
            state = initial,
            action = PayrollTabUiAction.SetViewMode(PayrollViewMode.COMPACT)
        )

        assertEquals(PayrollViewMode.COMPACT, updated.viewMode)
    }
}
