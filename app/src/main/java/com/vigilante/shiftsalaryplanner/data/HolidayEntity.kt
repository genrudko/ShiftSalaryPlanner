package com.vigilante.shiftsalaryplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object HolidayKinds {
    const val HOLIDAY = "HOLIDAY"
    const val TRANSFERRED_DAY_OFF = "TRANSFERRED_DAY_OFF"
    const val SHORT_DAY = "SHORT_DAY"
}

@Entity(tableName = "holiday_days")
data class HolidayEntity(
    @PrimaryKey
    val id: String,
    val date: String,
    val title: String,
    val scopeCode: String = "RU-FED",
    val kind: String = HolidayKinds.HOLIDAY,
    val isNonWorking: Boolean = true
)
