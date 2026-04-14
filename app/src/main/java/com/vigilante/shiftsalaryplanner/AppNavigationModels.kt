package com.vigilante.shiftsalaryplanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(val label: String, val icon: ImageVector) {
    CALENDAR("Календарь", Icons.Outlined.CalendarMonth),
    PAYROLL("Расчёт", Icons.Outlined.Calculate),
    PAYMENTS("Выплаты", Icons.Outlined.Payments),
    ALARMS("Будильники", Icons.Outlined.Alarm),
    SHIFTS("Смены", Icons.AutoMirrored.Outlined.EventNote),
    SETTINGS("Настройки", Icons.Outlined.Settings)
}
