package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global external-calendar layer settings for the M15 Today timeline.
 *
 * Defined by docs/superpowers/plans/2026-09-16-m15-1-today-foundation.md Task 5 and the
 * M15 design section 4: the layer is an opt-in read-only source, defaults to **disabled**,
 * and only the [enabledCalendarIds] calendars contribute occurrences to the normalized
 * timeline. The layer can be disabled globally while keeping the selected calendar IDs,
 * so re-enabling restores the same filtering set.
 */
data class TodayExternalCalendarSettings(
    val layerEnabled: Boolean = false,
    val enabledCalendarIds: Set<Long> = emptySet(),
)

/** Default external-calendar layer settings: globally disabled, no calendars selected. */
fun defaultTodayExternalCalendarSettings(): TodayExternalCalendarSettings =
    TodayExternalCalendarSettings()

/**
 * Deterministic serialization of enabled calendar IDs for SharedPreferences.
 *
 * Positive calendar IDs only; the set is sorted and de-duplicated so the stored value
 * is stable and order-independent across saves.
 */
fun serializeEnabledCalendarIds(ids: Collection<Long>): String =
    ids.filter { it > 0L }.distinct().sorted().joinToString(",")

/**
 * Parses a serialized enabled-calendar-ID list, ignoring empty or non-numeric tokens so
 * malformed or legacy values degrade to a safe (empty) filtering set.
 */
fun parseEnabledCalendarIds(raw: String): Set<Long> =
    raw.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it > 0L }.toSet()

/**
 * Focused persistence for the M15 external-calendar layer settings.
 *
 * Uses its own profile-scoped prefs file and deliberately does not replace
 * [TodayLayoutSettingsStore] responsibilities: layout bubbles stay in
 * `today_layout_settings`, this store persists only the global layer flag and the
 * enabled calendar ID filter. No event payloads are ever persisted here.
 */
class TodayExternalCalendarSettingsStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(load())

    val settingsFlow: Flow<TodayExternalCalendarSettings> = _settingsFlow.asStateFlow()

    fun save(settings: TodayExternalCalendarSettings) {
        val sanitized = settings.copy(enabledCalendarIds = parseEnabledCalendarIds(
            serializeEnabledCalendarIds(settings.enabledCalendarIds)
        ))
        prefs.edit {
            putBoolean(KEY_LAYER_ENABLED, sanitized.layerEnabled)
            putString(KEY_ENABLED_CALENDAR_IDS, serializeEnabledCalendarIds(sanitized.enabledCalendarIds))
        }
        _settingsFlow.value = sanitized
    }

    fun setLayerEnabled(enabled: Boolean) {
        save(_settingsFlow.value.copy(layerEnabled = enabled))
    }

    fun setEnabledCalendarIds(ids: Collection<Long>) {
        save(_settingsFlow.value.copy(enabledCalendarIds = ids.toSet()))
    }

    private fun load(): TodayExternalCalendarSettings {
        val layerEnabled = prefs.getBoolean(KEY_LAYER_ENABLED, false)
        val rawIds = prefs.getString(KEY_ENABLED_CALENDAR_IDS, "") ?: ""
        return TodayExternalCalendarSettings(
            layerEnabled = layerEnabled,
            enabledCalendarIds = parseEnabledCalendarIds(rawIds),
        )
    }

    companion object {
        const val PREFS_NAME = "today_external_calendar_settings"
        private const val KEY_LAYER_ENABLED = "layer_enabled"
        private const val KEY_ENABLED_CALENDAR_IDS = "enabled_calendar_ids"
    }
}
