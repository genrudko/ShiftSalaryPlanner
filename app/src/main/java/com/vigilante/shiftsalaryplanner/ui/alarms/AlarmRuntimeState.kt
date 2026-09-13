package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class AlarmReadinessUi(
    val ready: Boolean,
    val issues: List<String>
)

fun buildAlarmReadinessUi(
    canScheduleExactAlarms: Boolean,
    notificationPermissionGranted: Boolean,
    fullScreenIntentPermissionGranted: Boolean,
    exactAlarmRequired: Boolean,
    notificationPermissionRequired: Boolean,
    fullScreenIntentRequired: Boolean
): AlarmReadinessUi {
    val issues = buildList {
        if (exactAlarmRequired && !canScheduleExactAlarms) add("Точные будильники ограничены")
        if (notificationPermissionRequired && !notificationPermissionGranted) add("Уведомления не разрешены")
        if (fullScreenIntentRequired && !fullScreenIntentPermissionGranted) add("Полноэкранный режим не разрешён")
    }
    return AlarmReadinessUi(ready = issues.isEmpty(), issues = issues)
}

class AlarmRuntimeState {
    var permissionRefreshToken by mutableIntStateOf(0)
        private set
    var lastRescheduleResult by mutableStateOf<ShiftAlarmRescheduleResult?>(null)

    fun refreshPermissions() { permissionRefreshToken += 1 }
    fun recordReschedule(result: ShiftAlarmRescheduleResult?) { lastRescheduleResult = result }
}

@Composable
fun rememberAlarmRuntimeState(activeProfileId: String): AlarmRuntimeState =
    remember(activeProfileId) { AlarmRuntimeState() }
