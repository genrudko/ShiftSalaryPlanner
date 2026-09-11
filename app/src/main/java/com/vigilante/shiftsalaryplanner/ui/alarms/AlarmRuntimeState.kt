package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
