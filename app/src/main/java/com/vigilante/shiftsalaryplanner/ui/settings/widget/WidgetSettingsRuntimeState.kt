package com.vigilante.shiftsalaryplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class WidgetSettingsRuntimeState {
    var refreshToken by mutableIntStateOf(0)
        private set

    fun refresh() { refreshToken += 1 }
}

@Composable
fun rememberWidgetSettingsRuntimeState(activeProfileId: String): WidgetSettingsRuntimeState =
    remember(activeProfileId) { WidgetSettingsRuntimeState() }
