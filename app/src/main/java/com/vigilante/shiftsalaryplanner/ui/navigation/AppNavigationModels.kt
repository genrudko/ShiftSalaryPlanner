package com.vigilante.shiftsalaryplanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label
) {
    CALENDAR("Календарь", Icons.Rounded.CalendarMonth),
    FINANCE("Финансы", Icons.Rounded.Paid),
    ALARMS("Будильники", Icons.Rounded.Alarm),
    SHIFTS("Смены", Icons.AutoMirrored.Rounded.Assignment),
    SETTINGS("Настройки", Icons.Rounded.Settings)
}
