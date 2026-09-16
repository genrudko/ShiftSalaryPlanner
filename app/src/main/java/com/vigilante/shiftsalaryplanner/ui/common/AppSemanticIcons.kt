package com.vigilante.shiftsalaryplanner

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppSemanticIconKey {
    CALENDAR,
    TODAY,
    FINANCE,
    MORE,
    WORKPLACES,
    SHIFT_TEMPLATES,
    PRODUCTION_CALENDAR,
    MANUAL_HOLIDAYS,
    ALARMS,
    NOTES,
    APPEARANCE,
    WIDGETS,
    QUICK_ACTIONS,
    WEAR,
    PROFILES,
    QUICK_START,
    BACKUP,
    CLOUD_BACKUP,
    IMPORT_EXPORT,
    REPORTS,
    HEALTH_CHECK,
    EVENT_LOG,
    CURRENT_PARAMETERS,
    ALARM_SETTINGS,
    SYSTEM_STATUS,
    TEST_ALARM,
    RESCHEDULE
}

fun appSemanticIcon(key: AppSemanticIconKey): ImageVector = when (key) {
    AppSemanticIconKey.CALENDAR -> Icons.Rounded.CalendarMonth
    AppSemanticIconKey.TODAY -> Icons.Rounded.Today
    AppSemanticIconKey.FINANCE -> Icons.Rounded.Paid
    AppSemanticIconKey.MORE -> Icons.Rounded.MoreHoriz
    AppSemanticIconKey.WORKPLACES -> Icons.Rounded.WorkHistory
    AppSemanticIconKey.SHIFT_TEMPLATES -> Icons.Rounded.Badge
    AppSemanticIconKey.PRODUCTION_CALENDAR -> Icons.Rounded.EventAvailable
    AppSemanticIconKey.MANUAL_HOLIDAYS -> Icons.Rounded.BeachAccess
    AppSemanticIconKey.ALARMS -> Icons.Rounded.Alarm
    AppSemanticIconKey.NOTES -> Icons.AutoMirrored.Rounded.EventNote
    AppSemanticIconKey.APPEARANCE -> Icons.Rounded.Palette
    AppSemanticIconKey.WIDGETS -> Icons.Rounded.Widgets
    AppSemanticIconKey.QUICK_ACTIONS -> Icons.Rounded.Bolt
    AppSemanticIconKey.WEAR -> Icons.Rounded.Watch
    AppSemanticIconKey.PROFILES -> Icons.Rounded.AccountCircle
    AppSemanticIconKey.QUICK_START -> Icons.Rounded.Lightbulb
    AppSemanticIconKey.BACKUP -> Icons.Rounded.Backup
    AppSemanticIconKey.CLOUD_BACKUP -> Icons.Rounded.Cloud
    AppSemanticIconKey.IMPORT_EXPORT -> Icons.Rounded.SwapVert
    AppSemanticIconKey.REPORTS -> Icons.Rounded.Assessment
    AppSemanticIconKey.HEALTH_CHECK -> Icons.AutoMirrored.Rounded.FactCheck
    AppSemanticIconKey.EVENT_LOG -> Icons.Rounded.History
    AppSemanticIconKey.CURRENT_PARAMETERS -> Icons.Rounded.Tune
    AppSemanticIconKey.ALARM_SETTINGS -> Icons.Rounded.Settings
    AppSemanticIconKey.SYSTEM_STATUS -> Icons.Rounded.VerifiedUser
    AppSemanticIconKey.TEST_ALARM -> Icons.Rounded.NotificationsActive
    AppSemanticIconKey.RESCHEDULE -> Icons.Rounded.Autorenew
}
