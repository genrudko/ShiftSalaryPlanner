package com.vigilante.shiftsalaryplanner

data class CalendarSyncCheckResult(
    val updated: Boolean,
    val changedOnServer: Boolean,
    val dayCount: Int,
    val message: String
)
