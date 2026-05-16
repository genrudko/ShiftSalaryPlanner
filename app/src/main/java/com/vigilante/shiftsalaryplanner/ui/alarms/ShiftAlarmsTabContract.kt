package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

data class ShiftAlarmsTabState(
    val settings: ShiftAlarmSettings,
    val shiftTemplates: List<ShiftTemplateEntity>,
    val lastRescheduleResult: ShiftAlarmRescheduleResult?,
    val upcomingAlarms: List<ShiftAlarmUpcomingInfo>,
    val canScheduleExactAlarms: Boolean,
    val notificationPermissionGranted: Boolean,
    val fullScreenIntentPermissionGranted: Boolean,
    val alarmSwipeDuplicateEnabled: Boolean = true,
    val alarmSwipeDeleteEnabled: Boolean = true
)

data class ShiftAlarmsTabActions(
    val onSave: (ShiftAlarmSettings) -> Unit,
    val onRequestNotificationPermission: () -> Unit,
    val onOpenExactAlarmSettings: () -> Unit,
    val onOpenFullScreenIntentSettings: () -> Unit,
    val onOpenSystemClock: () -> Unit,
    val onRescheduleNow: () -> Unit,
    val onCancelUpcomingAlarm: (ShiftAlarmUpcomingInfo) -> Unit,
    val onCancelUpcomingAlarms: (List<ShiftAlarmUpcomingInfo>) -> Unit
)
