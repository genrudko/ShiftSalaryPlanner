package com.vigilante.shiftsalaryplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_days")
data class ShiftDayEntity(
    @PrimaryKey
    val date: String,
    val shiftCode: String
)
