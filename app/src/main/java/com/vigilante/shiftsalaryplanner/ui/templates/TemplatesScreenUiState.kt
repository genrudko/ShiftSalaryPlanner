package com.vigilante.shiftsalaryplanner

data class TemplatesScreenUiState(
    val pendingDeletePatternId: String? = null,
    val showSystemStatuses: Boolean = false
)

sealed interface TemplatesScreenUiAction {
    data class SetPendingDeletePatternId(val id: String?) : TemplatesScreenUiAction
    data class SetShowSystemStatuses(val value: Boolean) : TemplatesScreenUiAction
}

fun reduceTemplatesScreenUiState(
    state: TemplatesScreenUiState,
    action: TemplatesScreenUiAction
): TemplatesScreenUiState {
    return when (action) {
        is TemplatesScreenUiAction.SetPendingDeletePatternId ->
            state.copy(pendingDeletePatternId = action.id)

        is TemplatesScreenUiAction.SetShowSystemStatuses ->
            state.copy(showSystemStatuses = action.value)
    }
}
