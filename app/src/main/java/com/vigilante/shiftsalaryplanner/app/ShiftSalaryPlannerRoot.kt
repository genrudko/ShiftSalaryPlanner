package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vigilante.shiftsalaryplanner.settings.AppProfilesState
import com.vigilante.shiftsalaryplanner.settings.AppearanceSettingsStore
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme

@Composable
fun ShiftSalaryPlannerRoot(
    initialNavigationState: AppNavigationState = AppNavigationState()
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val appearanceSettingsStore = remember(appContext) { AppearanceSettingsStore(appContext) }
    val appearanceSettings by appearanceSettingsStore.settingsFlow.collectAsState(
        initial = AppearanceSettings()
    )

    val appDependencies = remember(appContext) { createAppDependencies(appContext) }
    val profilesState by appDependencies.profileData.state.collectAsState(
        initial = appDependencies.profileData.initialState
    )
    val activeProfileId = profilesState.activeProfileId
    val profileDependencies = remember(appContext, activeProfileId) {
        createProfileDependencies(appContext, activeProfileId)
    }

    ShiftSalaryPlannerTheme(
        appearanceSettings = appearanceSettings
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ShiftSalaryApp(
                initialNavigationState = initialNavigationState,
                appearanceSettings = appearanceSettings,
                onSaveAppearanceSettings = { updated -> appearanceSettingsStore.save(updated) },
                profilesState = profilesState,
                appDependencies = appDependencies,
                profileDependencies = profileDependencies
            )
        }
    }
}
