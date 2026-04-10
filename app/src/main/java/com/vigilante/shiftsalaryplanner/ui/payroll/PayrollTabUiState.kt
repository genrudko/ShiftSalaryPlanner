package com.vigilante.shiftsalaryplanner

enum class PayrollViewMode { SUMMARY, SHEET }

data class PayrollTabUiState(
    val viewMode: PayrollViewMode = PayrollViewMode.SUMMARY
)

sealed interface PayrollTabUiAction {
    data class SetViewMode(val viewMode: PayrollViewMode) : PayrollTabUiAction
}

fun reducePayrollTabUiState(
    state: PayrollTabUiState,
    action: PayrollTabUiAction
): PayrollTabUiState {
    return when (action) {
        is PayrollTabUiAction.SetViewMode -> state.copy(viewMode = action.viewMode)
    }
}
