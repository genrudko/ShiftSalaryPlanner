package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate

class CalendarInteractionState(
    private val selectedDateState: MutableState<LocalDate?>,
    private val dayAssignmentsPreviewDateState: MutableState<LocalDate?>,
    private val quickPickerOpenState: MutableState<Boolean>,
    private val activeBrushCodeState: MutableState<String?>,
    private val isLegendExpandedState: MutableState<Boolean>,
    private val calendarWorkplaceFilterIdState: MutableState<String>
) {
    var selectedDate: LocalDate?
        get() = selectedDateState.value
        set(value) { selectedDateState.value = value }

    var dayAssignmentsPreviewDate: LocalDate?
        get() = dayAssignmentsPreviewDateState.value
        set(value) { dayAssignmentsPreviewDateState.value = value }

    var quickPickerOpen: Boolean
        get() = quickPickerOpenState.value
        set(value) { quickPickerOpenState.value = value }

    var activeBrushCode: String?
        get() = activeBrushCodeState.value
        set(value) { activeBrushCodeState.value = value }

    var isLegendExpanded: Boolean
        get() = isLegendExpandedState.value
        set(value) { isLegendExpandedState.value = value }

    var calendarWorkplaceFilterId: String
        get() = calendarWorkplaceFilterIdState.value
        set(value) { calendarWorkplaceFilterIdState.value = value }

    fun toggleQuickPicker() {
        quickPickerOpen = !quickPickerOpen
    }

    fun closeQuickPicker() {
        quickPickerOpen = false
    }

    fun selectBrush(code: String) {
        activeBrushCode = code
    }

    fun clearBrush() {
        activeBrushCode = null
    }

    fun toggleLegend() {
        isLegendExpanded = !isLegendExpanded
    }
}

@Composable
fun rememberCalendarInteractionState(
    workplaceFilterKey: String,
    initialWorkplaceFilterId: String
): CalendarInteractionState {
    val selectedDateState = remember { mutableStateOf<LocalDate?>(null) }
    val dayAssignmentsPreviewDateState = remember { mutableStateOf<LocalDate?>(null) }
    val quickPickerOpenState = rememberSaveable { mutableStateOf(false) }
    val activeBrushCodeState = rememberSaveable { mutableStateOf<String?>(null) }
    val isLegendExpandedState = rememberSaveable { mutableStateOf(false) }
    val calendarWorkplaceFilterIdState = rememberSaveable(workplaceFilterKey) {
        mutableStateOf(initialWorkplaceFilterId)
    }

    return remember(
        selectedDateState,
        dayAssignmentsPreviewDateState,
        quickPickerOpenState,
        activeBrushCodeState,
        isLegendExpandedState,
        calendarWorkplaceFilterIdState
    ) {
        CalendarInteractionState(
            selectedDateState = selectedDateState,
            dayAssignmentsPreviewDateState = dayAssignmentsPreviewDateState,
            quickPickerOpenState = quickPickerOpenState,
            activeBrushCodeState = activeBrushCodeState,
            isLegendExpandedState = isLegendExpandedState,
            calendarWorkplaceFilterIdState = calendarWorkplaceFilterIdState
        )
    }
}
