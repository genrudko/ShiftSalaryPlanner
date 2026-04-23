package com.vigilante.shiftsalaryplanner

enum class PayrollViewMode {
    COMPACT,
    DETAILED
}

enum class PayrollAmountViewMode {
    GROSS,
    NET
}

data class PayrollTabUiState(
    val viewMode: PayrollViewMode = PayrollViewMode.DETAILED,
    val amountViewMode: PayrollAmountViewMode = PayrollAmountViewMode.NET
)

sealed interface PayrollTabUiAction {
    data class SetViewMode(val viewMode: PayrollViewMode) : PayrollTabUiAction
    data class SetAmountViewMode(val amountViewMode: PayrollAmountViewMode) : PayrollTabUiAction
}

fun reducePayrollTabUiState(
    state: PayrollTabUiState,
    action: PayrollTabUiAction
): PayrollTabUiState {
    return when (action) {
        is PayrollTabUiAction.SetViewMode -> state.copy(viewMode = action.viewMode)
        is PayrollTabUiAction.SetAmountViewMode -> state.copy(amountViewMode = action.amountViewMode)
    }
}
