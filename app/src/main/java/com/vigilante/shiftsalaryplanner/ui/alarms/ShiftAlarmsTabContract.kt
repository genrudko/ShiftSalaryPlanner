package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

data class ShiftAlarmsTabState(
    val settings: ShiftAlarmSettings,
    val shiftTemplates: List<ShiftTemplateEntity>,
    val lastRescheduleResult: ShiftAlarmRescheduleResult?,
    val canScheduleExactAlarms: Boolean,
    val notificationPermissionGranted: Boolean,
    val fullScreenIntentPermissionGranted: Boolean
)

data class ShiftAlarmsTabActions(
    val onSave: (ShiftAlarmSettings) -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onOpenExactAlarmSettings: () -> Unit,
    val onOpenFullScreenIntentSettings: () -> Unit,
    val onOpenSystemClock: () -> Unit,
    val onRescheduleNow: () -> Unit
)
