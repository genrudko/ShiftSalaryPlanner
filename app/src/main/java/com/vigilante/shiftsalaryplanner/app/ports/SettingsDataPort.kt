package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettingsStore
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettingsStore
import kotlinx.coroutines.flow.Flow

interface SettingsDataPort {
    val workflowSettings: Flow<AppWorkflowSettings>
    val todayLayoutSettings: Flow<TodayLayoutSettings>
    val patterns: Flow<List<PatternTemplate>>

    fun saveWorkflowSettings(settings: AppWorkflowSettings)
    fun saveTodayLayoutSettings(settings: TodayLayoutSettings)
    fun upsertPattern(pattern: PatternTemplate)
    fun deletePattern(id: String)
}

class DefaultSettingsDataPort(
    private val workflowStore: AppWorkflowSettingsStore,
    private val todayLayoutStore: TodayLayoutSettingsStore,
    private val patternStore: PatternTemplatesStore
) : SettingsDataPort {
    override val workflowSettings: Flow<AppWorkflowSettings> = workflowStore.settingsFlow
    override val todayLayoutSettings: Flow<TodayLayoutSettings> = todayLayoutStore.settingsFlow
    override val patterns: Flow<List<PatternTemplate>> = patternStore.patternsFlow

    override fun saveWorkflowSettings(settings: AppWorkflowSettings) = workflowStore.save(settings)
    override fun saveTodayLayoutSettings(settings: TodayLayoutSettings) = todayLayoutStore.save(settings)
    override fun upsertPattern(pattern: PatternTemplate) = patternStore.addOrUpdate(pattern)
    override fun deletePattern(id: String) = patternStore.deleteById(id)
}
