package com.vigilante.shiftsalaryplanner.payroll.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.vigilante.shiftsalaryplanner.payroll.models.AccrualSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Репозиторий для работы с настройками начислений
 */
class PayrollSettingsRepository(private val context: Context) {

    private val Context.dataStore: DataStore<List<AccrualSettings>> by dataStore(
        fileName = "accrual_settings.json",
        serializer = AccrualSettingsSerializer
    )

    val settingsFlow: Flow<List<AccrualSettings>> = context.dataStore.data

    suspend fun getSettings(): List<AccrualSettings> {
        return context.dataStore.data.first()
    }

    suspend fun saveSettings(settings: List<AccrualSettings>) {
        context.dataStore.updateData { settings }
    }

    suspend fun updateSetting(updated: AccrualSettings) {
        context.dataStore.updateData { current ->
            current.map { if (it.id == updated.id) updated else it }
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.updateData {
            DefaultAccrualConfig.getDefaultSettings()
        }
    }

    suspend fun addCustomSetting(setting: AccrualSettings) {
        context.dataStore.updateData { current -> current + setting }
    }

    suspend fun removeSetting(id: String) {
        context.dataStore.updateData { current -> current.filter { it.id != id } }
    }
}