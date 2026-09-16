package com.vigilante.shiftsalaryplanner.app.ports

import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettingsStore
import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocationStore
import com.vigilante.shiftsalaryplanner.settings.TodayExternalCalendarSettings
import com.vigilante.shiftsalaryplanner.settings.TodayExternalCalendarSettingsStore
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettingsStore
import com.vigilante.shiftsalaryplanner.settings.WeatherSettings
import com.vigilante.shiftsalaryplanner.settings.WeatherSettingsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocationStore
import kotlinx.coroutines.flow.Flow

interface SettingsDataPort {
    val workflowSettings: Flow<AppWorkflowSettings>
    val todayLayoutSettings: Flow<TodayLayoutSettings>
    val todayExternalCalendarSettings: Flow<TodayExternalCalendarSettings>
    val weatherSettings: Flow<WeatherSettings>
    val workplaceWeatherLocations: Flow<Map<String, WorkplaceWeatherLocation>>
    val savedWeatherLocations: Flow<List<SavedWeatherLocation>>
    val patterns: Flow<List<PatternTemplate>>

    fun saveWorkflowSettings(settings: AppWorkflowSettings)
    fun saveTodayLayoutSettings(settings: TodayLayoutSettings)
    fun saveTodayExternalCalendarSettings(settings: TodayExternalCalendarSettings)
    fun saveWeatherSettings(settings: WeatherSettings)
    fun saveWorkplaceWeatherLocation(location: WorkplaceWeatherLocation)
    fun deleteWorkplaceWeatherLocation(workplaceId: String)
    fun saveSavedWeatherLocation(location: SavedWeatherLocation)
    fun deleteSavedWeatherLocation(id: String)
    fun reorderSavedWeatherLocations(orderedIds: List<String>)
    fun upsertPattern(pattern: PatternTemplate)
    fun deletePattern(id: String)
}

class DefaultSettingsDataPort(
    private val workflowStore: AppWorkflowSettingsStore,
    private val todayLayoutStore: TodayLayoutSettingsStore,
    private val todayExternalCalendarStore: TodayExternalCalendarSettingsStore,
    private val weatherSettingsStore: WeatherSettingsStore,
    private val workplaceWeatherLocationStore: WorkplaceWeatherLocationStore,
    private val savedWeatherLocationStore: SavedWeatherLocationStore,
    private val patternStore: PatternTemplatesStore
) : SettingsDataPort {
    override val workflowSettings: Flow<AppWorkflowSettings> = workflowStore.settingsFlow
    override val todayLayoutSettings: Flow<TodayLayoutSettings> = todayLayoutStore.settingsFlow
    override val todayExternalCalendarSettings: Flow<TodayExternalCalendarSettings> =
        todayExternalCalendarStore.settingsFlow
    override val weatherSettings: Flow<WeatherSettings> = weatherSettingsStore.settingsFlow
    override val workplaceWeatherLocations: Flow<Map<String, WorkplaceWeatherLocation>> =
        workplaceWeatherLocationStore.locationsFlow
    override val savedWeatherLocations: Flow<List<SavedWeatherLocation>> =
        savedWeatherLocationStore.savedLocationsFlow
    override val patterns: Flow<List<PatternTemplate>> = patternStore.patternsFlow

    override fun saveWorkflowSettings(settings: AppWorkflowSettings) = workflowStore.save(settings)
    override fun saveTodayLayoutSettings(settings: TodayLayoutSettings) = todayLayoutStore.save(settings)
    override fun saveTodayExternalCalendarSettings(settings: TodayExternalCalendarSettings) =
        todayExternalCalendarStore.save(settings)
    override fun saveWeatherSettings(settings: WeatherSettings) = weatherSettingsStore.save(settings)
    override fun saveWorkplaceWeatherLocation(location: WorkplaceWeatherLocation) =
        workplaceWeatherLocationStore.saveLocation(location)
    override fun deleteWorkplaceWeatherLocation(workplaceId: String) {
        workplaceWeatherLocationStore.deleteLocation(workplaceId)
    }
    override fun saveSavedWeatherLocation(location: SavedWeatherLocation) =
        savedWeatherLocationStore.saveLocation(location)
    override fun deleteSavedWeatherLocation(id: String) {
        savedWeatherLocationStore.delete(id)
    }
    override fun reorderSavedWeatherLocations(orderedIds: List<String>) =
        savedWeatherLocationStore.reorder(orderedIds)
    override fun upsertPattern(pattern: PatternTemplate) = patternStore.addOrUpdate(pattern)
    override fun deletePattern(id: String) = patternStore.deleteById(id)
}
