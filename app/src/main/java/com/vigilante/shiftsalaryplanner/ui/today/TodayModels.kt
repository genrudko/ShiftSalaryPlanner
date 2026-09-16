package com.vigilante.shiftsalaryplanner.ui.today

import java.time.LocalDate

data class TodayShiftPreview(
    val title: String,
    val workplace: String,
    val code: String,
    val iconKey: String,
    val badgeColor: Int,
    val timeLabel: String,
    val hoursLabel: String
)

data class UpcomingPaymentItem(
    val title: String,
    val periodLabel: String,
    val date: LocalDate,
    val amount: Double? = null
)
