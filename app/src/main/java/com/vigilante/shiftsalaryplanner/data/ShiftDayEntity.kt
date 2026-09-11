package com.vigilante.shiftsalaryplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_days")
data class ShiftDayEntity(
    @PrimaryKey
    val date: String,
    val shiftCode: String,
    val overrideStartTime: String? = null,
    val overrideEndTime: String? = null,
    val overrideTotalHours: Double? = null,
    val overrideBreakHours: Double? = null,
    val overrideNightHours: Double? = null,
    val overridePaidHours: Double? = null,
    val overrideShiftPayAmount: Double? = null,
    val overrideNote: String? = null
)
