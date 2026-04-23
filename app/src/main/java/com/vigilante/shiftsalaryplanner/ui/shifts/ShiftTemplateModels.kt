package com.vigilante.shiftsalaryplanner

data class ShiftSpecialRule(
    val specialDayTypeName: String,
    val specialDayCompensationName: String,
    val isSystemStatus: Boolean = false
)

enum class TemplateMode {
    SHIFTS,
    CYCLES
}
const val KEY_EMPTY_DAY = "__EMPTY_DAY__"
const val BRUSH_CLEAR = "__BRUSH_CLEAR__"
