package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import com.vigilante.shiftsalaryplanner.ShiftAlarmConfig
import com.vigilante.shiftsalaryplanner.ShiftAlarmSettings
import com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.defaultShiftAlarmTitle
import com.vigilante.shiftsalaryplanner.defaultShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.resolveAlarmClockFromShiftStart
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

    suspend fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig) {
        val current = loadFromPrefs()
        val updated = current.templateConfigs.toMutableList()
        val index = updated.indexOfFirst { it.shiftCode == config.shiftCode }
        if (index >= 0) {
            updated[index] = config
        } else {
            updated.add(config)
        }
        save(current.copy(templateConfigs = updated.sortedBy { it.shiftCode }))
    }

    suspend fun removeTemplateConfig(shiftCode: String) {
        val current = loadFromPrefs()
        save(current.copy(templateConfigs = current.templateConfigs.filterNot { it.shiftCode == shiftCode }))
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
                    defaultShiftTemplateAlarmConfig(template).copy(
                        shiftCode = template.code,
                        enabled = true,
                        startHour = if (isNight) nightHour else dayHour,
                        startMinute = if (isNight) nightMinute else dayMinute,
                        alarms = legacyGroup.map { legacy ->
                            val (triggerHour, triggerMinute) = resolveAlarmClockFromShiftStart(
                                startHour = if (isNight) nightHour else dayHour,
                                startMinute = if (isNight) nightMinute else dayMinute,
                                minutesBefore = legacy.triggerMinutesBefore.coerceIn(0, 24 * 60)
                            )
                            ShiftAlarmConfig(
                                id = UUID.randomUUID().toString(),
                                title = legacy.title.ifBlank {
                                    defaultShiftAlarmTitle(
                                        shiftAlarmTemplateLabel(template),
                                        triggerHour,
                                        triggerMinute
                                    )
                                },
                                triggerHour = triggerHour,
                                triggerMinute = triggerMinute,
                                volumePercent = 100,
                                soundUri = null,
                                soundLabel = "",
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
                    val startHour = item.optInt("startHour", 8).coerceIn(0, 23)
                    val startMinute = item.optInt("startMinute", 0).coerceIn(0, 59)
                    add(
                        ShiftTemplateAlarmConfig(
                            shiftCode = item.optString("shiftCode"),
                            enabled = item.optBoolean("enabled", false),
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = item.optInt("endHour", (startHour + 12) % 24).coerceIn(0, 23),
                            endMinute = item.optInt("endMinute", startMinute).coerceIn(0, 59),
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
                val legacyTriggerMinutesBefore = item.optInt("triggerMinutesBefore", Int.MIN_VALUE)
                val triggerHour = when {
                    item.has("triggerHour") -> item.optInt("triggerHour", 7).coerceIn(0, 23)
                    legacyTriggerMinutesBefore != Int.MIN_VALUE -> resolveAlarmClockFromShiftStart(8, 0, legacyTriggerMinutesBefore.coerceIn(0, 24 * 60)).first
                    else -> 7
                }
                val triggerMinute = when {
                    item.has("triggerMinute") -> item.optInt("triggerMinute", 0).coerceIn(0, 59)
                    legacyTriggerMinutesBefore != Int.MIN_VALUE -> resolveAlarmClockFromShiftStart(8, 0, legacyTriggerMinutesBefore.coerceIn(0, 24 * 60)).second
                    else -> 0
                }
                add(
                    ShiftAlarmConfig(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = item.optString("title"),
                        triggerHour = triggerHour,
                        triggerMinute = triggerMinute,
                        volumePercent = item.optInt("volumePercent", 100).coerceIn(0, 100),
                        soundUri = item.optString("soundUri").ifBlank { null },
                        soundLabel = item.optString("soundLabel"),
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
                    put("endHour", config.endHour.coerceIn(0, 23))
                    put("endMinute", config.endMinute.coerceIn(0, 59))
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
                    put("triggerHour", alarm.triggerHour.coerceIn(0, 23))
                    put("triggerMinute", alarm.triggerMinute.coerceIn(0, 59))
                    put("volumePercent", alarm.volumePercent.coerceIn(0, 100))
                    if (!alarm.soundUri.isNullOrBlank()) put("soundUri", alarm.soundUri)
                    if (alarm.soundLabel.isNotBlank()) put("soundLabel", alarm.soundLabel)
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
