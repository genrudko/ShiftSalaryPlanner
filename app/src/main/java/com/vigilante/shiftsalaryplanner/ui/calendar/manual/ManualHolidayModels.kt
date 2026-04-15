package com.vigilante.shiftsalaryplanner


const val MANUAL_HOLIDAY_SCOPE = "MANUAL"
const val MANUAL_HOLIDAY_SEPARATOR = "\u001F"
const val PREFS_MANUAL_HOLIDAYS = "manual_holidays"

data class ManualHolidayRecord(
    val date: String,
    val title: String,
    val kind: String,
    val isNonWorking: Boolean
)
