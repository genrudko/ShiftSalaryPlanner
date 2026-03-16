package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import com.vigilante.shiftsalaryplanner.ShiftAlarmConfig
import com.vigilante.shiftsalaryplanner.ShiftAlarmSettings
import com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.defaultShiftAlarmTitle
import com.vigilante.shiftsalaryplanner.defaultShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.shiftAlarmTemplateLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ShiftAlarmStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("shift_alarm_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    val settingsFlow: Flow<ShiftAlarmSettings> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): ShiftAlarmSettings {
        val parsedTemplateConfigs = parseTemplateConfigs(prefs.getString(KEY_TEMPLATE_CONFIGS_JSON, null))
        return ShiftAlarmSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            autoReschedule = prefs.getBoolean(KEY_AUTO_RESCHEDULE, true),
            scheduleHorizonDays = prefs.getInt(KEY_SCHEDULE_HORIZON_DAYS, 90).coerceIn(7, 365),
            templateConfigs = parsedTemplateConfigs
        )
    }

    suspend fun save(settings: ShiftAlarmSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putBoolean(KEY_AUTO_RESCHEDULE, settings.autoReschedule)
            .putInt(KEY_SCHEDULE_HORIZON_DAYS, settings.scheduleHorizonDays.coerceIn(7, 365))
            .putString(KEY_TEMPLATE_CONFIGS_JSON, serializeTemplateConfigs(settings.templateConfigs))
            .apply()

        _settingsFlow.value = loadFromPrefs()
    }

    suspend fun synchronizeTemplates(templates: List<ShiftTemplateEntity>) {
        val current = loadFromPrefs()
        val synchronizedSettings = when {
            current.templateConfigs.isEmpty() && hasLegacyAlarmData() -> migrateLegacySettings(current, templates)
            else -> mergeCurrentSettingsWithTemplates(current, templates)
        }
        save(synchronizedSettings)
    }

    private fun mergeCurrentSettingsWithTemplates(
        current: ShiftAlarmSettings,
        templates: List<ShiftTemplateEntity>
    ): ShiftAlarmSettings {
        val existingByCode = current.templateConfigs.associateBy { it.shiftCode }
        val mergedConfigs = templates
            .sortedBy { it.sortOrder }
            .map { template ->
                existingByCode[template.code] ?: defaultShiftTemplateAlarmConfig(template)
            }
        return current.copy(templateConfigs = mergedConfigs)
    }

    private fun hasLegacyAlarmData(): Boolean {
        return prefs.contains(KEY_LEGACY_ALARMS_JSON) ||
                prefs.contains(KEY_LEGACY_DAY_SHIFT_START_HOUR) ||
                prefs.contains(KEY_LEGACY_NIGHT_SHIFT_START_HOUR)
    }

    private fun migrateLegacySettings(
        current: ShiftAlarmSettings,
        templates: List<ShiftTemplateEntity>
    ): ShiftAlarmSettings {
        val legacyAlarms = parseLegacyAlarms(prefs.getString(KEY_LEGACY_ALARMS_JSON, null))
        val dayAlarms = legacyAlarms.filter { it.targetTypeName == LEGACY_TARGET_DAY }
        val nightAlarms = legacyAlarms.filter { it.targetTypeName == LEGACY_TARGET_NIGHT }
        val dayHour = prefs.getInt(KEY_LEGACY_DAY_SHIFT_START_HOUR, 8).coerceIn(0, 23)
        val dayMinute = prefs.getInt(KEY_LEGACY_DAY_SHIFT_START_MINUTE, 0).coerceIn(0, 59)
        val nightHour = prefs.getInt(KEY_LEGACY_NIGHT_SHIFT_START_HOUR, 20).coerceIn(0, 23)
        val nightMinute = prefs.getInt(KEY_LEGACY_NIGHT_SHIFT_START_MINUTE, 0).coerceIn(0, 59)

        val migratedConfigs = templates
            .sortedBy { it.sortOrder }
            .map { template ->
                val isNight = template.nightHours > 0.0
                val legacyGroup = if (isNight) nightAlarms else dayAlarms
                if (legacyGroup.isEmpty()) {
                    defaultShiftTemplateAlarmConfig(template)
                } else {
                    ShiftTemplateAlarmConfig(
                        shiftCode = template.code,
                        enabled = true,
                        startHour = if (isNight) nightHour else dayHour,
                        startMinute = if (isNight) nightMinute else dayMinute,
                        alarms = legacyGroup.map { legacy ->
                            ShiftAlarmConfig(
                                id = UUID.randomUUID().toString(),
                                title = legacy.title.ifBlank {
                                    defaultShiftAlarmTitle(
                                        shiftAlarmTemplateLabel(template),
                                        legacy.triggerMinutesBefore
                                    )
                                },
                                triggerMinutesBefore = legacy.triggerMinutesBefore.coerceIn(0, 24 * 60),
                                enabled = legacy.enabled
                            )
                        }
                    )
                }
            }

        return current.copy(templateConfigs = migratedConfigs)
    }

    private fun parseTemplateConfigs(raw: String?): List<ShiftTemplateAlarmConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        ShiftTemplateAlarmConfig(
                            shiftCode = item.optString("shiftCode"),
                            enabled = item.optBoolean("enabled", false),
                            startHour = item.optInt("startHour", 8).coerceIn(0, 23),
                            startMinute = item.optInt("startMinute", 0).coerceIn(0, 59),
                            alarms = parseAlarmItems(item.optJSONArray("alarms"))
                        )
                    )
                }
            }.filter { it.shiftCode.isNotBlank() }
        }.getOrElse { emptyList() }
    }

    private fun parseAlarmItems(array: JSONArray?): List<ShiftAlarmConfig> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ShiftAlarmConfig(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = item.optString("title"),
                        triggerMinutesBefore = item.optInt("triggerMinutesBefore", 60).coerceIn(0, 24 * 60),
                        enabled = item.optBoolean("enabled", true)
                    )
                )
            }
        }
    }

    private fun serializeTemplateConfigs(configs: List<ShiftTemplateAlarmConfig>): String {
        val array = JSONArray()
        configs.forEach { config ->
            array.put(
                JSONObject().apply {
                    put("shiftCode", config.shiftCode)
                    put("enabled", config.enabled)
                    put("startHour", config.startHour.coerceIn(0, 23))
                    put("startMinute", config.startMinute.coerceIn(0, 59))
                    put("alarms", serializeAlarmItems(config.alarms))
                }
            )
        }
        return array.toString()
    }

    private fun serializeAlarmItems(alarms: List<ShiftAlarmConfig>): JSONArray {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("title", alarm.title)
                    put("triggerMinutesBefore", alarm.triggerMinutesBefore.coerceIn(0, 24 * 60))
                    put("enabled", alarm.enabled)
                }
            )
        }
        return array
    }

    private fun parseLegacyAlarms(raw: String?): List<LegacyShiftAlarmConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        LegacyShiftAlarmConfig(
                            title = item.optString("title"),
                            targetTypeName = item.optString("targetTypeName", LEGACY_TARGET_DAY),
                            triggerMinutesBefore = item.optInt("triggerMinutesBefore", 60).coerceIn(0, 24 * 60),
                            enabled = item.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private data class LegacyShiftAlarmConfig(
        val title: String,
        val targetTypeName: String,
        val triggerMinutesBefore: Int,
        val enabled: Boolean
    )

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_AUTO_RESCHEDULE = "auto_reschedule"
        private const val KEY_SCHEDULE_HORIZON_DAYS = "schedule_horizon_days"
        private const val KEY_TEMPLATE_CONFIGS_JSON = "template_configs_json"

        private const val KEY_LEGACY_ALARMS_JSON = "alarms_json"
        private const val KEY_LEGACY_DAY_SHIFT_START_HOUR = "day_shift_start_hour"
        private const val KEY_LEGACY_DAY_SHIFT_START_MINUTE = "day_shift_start_minute"
        private const val KEY_LEGACY_NIGHT_SHIFT_START_HOUR = "night_shift_start_hour"
        private const val KEY_LEGACY_NIGHT_SHIFT_START_MINUTE = "night_shift_start_minute"
        private const val LEGACY_TARGET_DAY = "DAY"
        private const val LEGACY_TARGET_NIGHT = "NIGHT"
    }
}
