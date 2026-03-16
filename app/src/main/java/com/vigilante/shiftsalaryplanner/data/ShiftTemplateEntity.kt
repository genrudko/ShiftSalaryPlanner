package com.vigilante.shiftsalaryplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_templates")
data class ShiftTemplateEntity(
    @PrimaryKey
    val code: String,
    val title: String,
    val iconKey: String,
    val totalHours: Double,
    val breakHours: Double,
    val nightHours: Double,
    val colorHex: String,
    val isWeekendPaid: Boolean,
    val active: Boolean,
    val sortOrder: Int
)
