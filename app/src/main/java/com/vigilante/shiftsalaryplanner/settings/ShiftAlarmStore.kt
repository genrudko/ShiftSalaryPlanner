package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.ShiftAlarmConfig
import com.vigilante.shiftsalaryplanner.ShiftAlarmVibrationType
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingActionStyle
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingAnimationStyle
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingButtonsLayout
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingClockAlignment
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingAnimationMode
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingUiSettings
import com.vigilante.shiftsalaryplanner.ShiftAlarmRingVisualStyle
import com.vigilante.shiftsalaryplanner.ShiftAlarmBehaviorSettings
import com.vigilante.shiftsalaryplanner.ShiftAlarmSettings
import com.vigilante.shiftsalaryplanner.ShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.defaultShiftTemplateAlarmConfig
import com.vigilante.shiftsalaryplanner.resolveAlarmClockFromShiftStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ShiftAlarmStore(context: Context) {

    private val prefs = context.profileSharedPreferences("shift_alarm_settings")

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    val settingsFlow: Flow<ShiftAlarmSettings> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): ShiftAlarmSettings {
        val parsedTemplateConfigs = parseTemplateConfigs(prefs.getString(KEY_TEMPLATE_CONFIGS_JSON, null))
        val animationMode = runCatching {
            ShiftAlarmRingAnimationMode.valueOf(
                prefs.getString(
                    KEY_RING_ANIMATION_MODE,
                    ShiftAlarmRingAnimationMode.SOFT.name
                ) ?: ShiftAlarmRingAnimationMode.SOFT.name
            )
        }.getOrElse { ShiftAlarmRingAnimationMode.SOFT }
        val visualStyle = runCatching {
            ShiftAlarmRingVisualStyle.valueOf(
                prefs.getString(
                    KEY_RING_VISUAL_STYLE,
                    ShiftAlarmRingVisualStyle.MODERN.name
                ) ?: ShiftAlarmRingVisualStyle.MODERN.name
            )
        }.getOrElse { ShiftAlarmRingVisualStyle.MODERN }
        val animationStyle = runCatching {
            ShiftAlarmRingAnimationStyle.valueOf(
                prefs.getString(
                    KEY_RING_ANIMATION_STYLE,
                    ShiftAlarmRingAnimationStyle.AURORA.name
                ) ?: ShiftAlarmRingAnimationStyle.AURORA.name
            )
        }.getOrElse { ShiftAlarmRingAnimationStyle.AURORA }
        val actionStyle = runCatching {
            ShiftAlarmRingActionStyle.valueOf(
                prefs.getString(
                    KEY_RING_ACTION_STYLE,
                    ShiftAlarmRingActionStyle.BUTTONS.name
                ) ?: ShiftAlarmRingActionStyle.BUTTONS.name
            )
        }.getOrElse { ShiftAlarmRingActionStyle.BUTTONS }
        val buttonsLayout = runCatching {
            ShiftAlarmRingButtonsLayout.valueOf(
                prefs.getString(
                    KEY_RING_BUTTONS_LAYOUT,
                    ShiftAlarmRingButtonsLayout.HORIZONTAL.name
                ) ?: ShiftAlarmRingButtonsLayout.HORIZONTAL.name
            )
        }.getOrElse { ShiftAlarmRingButtonsLayout.HORIZONTAL }
        val clockAlignment = runCatching {
            ShiftAlarmRingClockAlignment.valueOf(
                prefs.getString(
                    KEY_RING_CLOCK_ALIGNMENT,
                    ShiftAlarmRingClockAlignment.TOP.name
                ) ?: ShiftAlarmRingClockAlignment.TOP.name
            )
        }.getOrElse { ShiftAlarmRingClockAlignment.TOP }
        return ShiftAlarmSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            autoReschedule = prefs.getBoolean(KEY_AUTO_RESCHEDULE, true),
            scheduleHorizonDays = prefs.getInt(KEY_SCHEDULE_HORIZON_DAYS, 90).coerceIn(7, 365),
            templateConfigs = parsedTemplateConfigs,
            ringUi = ShiftAlarmRingUiSettings(
                showCurrentClock = prefs.getBoolean(KEY_RING_SHOW_CURRENT_CLOCK, true),
                showDate = prefs.getBoolean(KEY_RING_SHOW_DATE, true),
                pulseAccent = prefs.getBoolean(KEY_RING_PULSE_ACCENT, true),
                animatedGradient = prefs.getBoolean(KEY_RING_ANIMATED_GRADIENT, true),
                animationMode = animationMode,
                animationStyle = animationStyle,
                visualStyle = visualStyle,
                actionStyle = actionStyle,
                buttonsLayout = buttonsLayout,
                clockAlignment = clockAlignment,
                clockScale = prefs.getFloat(KEY_RING_CLOCK_SCALE, 1.0f).coerceIn(0.8f, 1.4f),
                textScale = prefs.getFloat(KEY_RING_TEXT_SCALE, 1.0f).coerceIn(0.85f, 1.35f),
                useMonospaceClock = prefs.getBoolean(KEY_RING_USE_MONOSPACE_CLOCK, false),
                showMetaInfo = prefs.getBoolean(KEY_RING_SHOW_META_INFO, true),
                showSoundLabel = prefs.getBoolean(KEY_RING_SHOW_SOUND_LABEL, true),
                showVolumeInfo = prefs.getBoolean(KEY_RING_SHOW_VOLUME_INFO, true),
                showTimezoneInfo = prefs.getBoolean(KEY_RING_SHOW_TIMEZONE_INFO, false)
            ),
            behavior = ShiftAlarmBehaviorSettings(
                vibrationEnabled = prefs.getBoolean(KEY_BEHAVIOR_VIBRATION_ENABLED, true),
                vibrationType = runCatching {
                    ShiftAlarmVibrationType.valueOf(
                        prefs.getString(KEY_BEHAVIOR_VIBRATION_TYPE, ShiftAlarmVibrationType.SYSTEM.name)
                            ?: ShiftAlarmVibrationType.SYSTEM.name
                    )
                }.getOrElse { ShiftAlarmVibrationType.SYSTEM },
                vibrationDurationSeconds = prefs.getInt(KEY_BEHAVIOR_VIBRATION_DURATION_SECONDS, 25).coerceIn(0, 300),
                customVibrationPattern = prefs.getString(KEY_BEHAVIOR_CUSTOM_VIBRATION_PATTERN, "")?.trim().orEmpty(),
                snoozeIntervalMinutes = prefs.getInt(KEY_BEHAVIOR_SNOOZE_INTERVAL_MINUTES, 10).coerceIn(1, 120),
                snoozeCountLimit = prefs.getInt(KEY_BEHAVIOR_SNOOZE_COUNT_LIMIT, 3).coerceIn(0, 10),
                ringDurationSeconds = prefs.getInt(KEY_BEHAVIOR_RING_DURATION_SECONDS, 180).coerceIn(10, 3_600),
                rampUpDurationSeconds = prefs.getInt(KEY_BEHAVIOR_RAMP_UP_DURATION_SECONDS, 0).coerceIn(0, 180),
                defaultSoundUri = prefs.getString(KEY_BEHAVIOR_DEFAULT_SOUND_URI, null)?.ifBlank { null },
                defaultSoundLabel = prefs.getString(KEY_BEHAVIOR_DEFAULT_SOUND_LABEL, "")?.trim().orEmpty()
            )
        )
    }

    fun save(settings: ShiftAlarmSettings) {
        prefs.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
                .putBoolean(KEY_AUTO_RESCHEDULE, settings.autoReschedule)
                .putInt(KEY_SCHEDULE_HORIZON_DAYS, settings.scheduleHorizonDays.coerceIn(7, 365))
                .putBoolean(KEY_RING_SHOW_CURRENT_CLOCK, settings.ringUi.showCurrentClock)
                .putBoolean(KEY_RING_SHOW_DATE, settings.ringUi.showDate)
                .putBoolean(KEY_RING_PULSE_ACCENT, settings.ringUi.pulseAccent)
                .putBoolean(KEY_RING_ANIMATED_GRADIENT, settings.ringUi.animatedGradient)
                .putString(KEY_RING_ANIMATION_MODE, settings.ringUi.animationMode.name)
                .putString(KEY_RING_ANIMATION_STYLE, settings.ringUi.animationStyle.name)
                .putString(KEY_RING_VISUAL_STYLE, settings.ringUi.visualStyle.name)
                .putString(KEY_RING_ACTION_STYLE, settings.ringUi.actionStyle.name)
                .putString(KEY_RING_BUTTONS_LAYOUT, settings.ringUi.buttonsLayout.name)
                .putString(KEY_RING_CLOCK_ALIGNMENT, settings.ringUi.clockAlignment.name)
                .putFloat(KEY_RING_CLOCK_SCALE, settings.ringUi.clockScale.coerceIn(0.8f, 1.4f))
                .putFloat(KEY_RING_TEXT_SCALE, settings.ringUi.textScale.coerceIn(0.85f, 1.35f))
                .putBoolean(KEY_RING_USE_MONOSPACE_CLOCK, settings.ringUi.useMonospaceClock)
                .putBoolean(KEY_RING_SHOW_META_INFO, settings.ringUi.showMetaInfo)
                .putBoolean(KEY_RING_SHOW_SOUND_LABEL, settings.ringUi.showSoundLabel)
                .putBoolean(KEY_RING_SHOW_VOLUME_INFO, settings.ringUi.showVolumeInfo)
                .putBoolean(KEY_RING_SHOW_TIMEZONE_INFO, settings.ringUi.showTimezoneInfo)
                .putBoolean(KEY_BEHAVIOR_VIBRATION_ENABLED, settings.behavior.vibrationEnabled)
                .putString(KEY_BEHAVIOR_VIBRATION_TYPE, settings.behavior.vibrationType.name)
                .putInt(KEY_BEHAVIOR_VIBRATION_DURATION_SECONDS, settings.behavior.vibrationDurationSeconds.coerceIn(0, 300))
                .putString(KEY_BEHAVIOR_CUSTOM_VIBRATION_PATTERN, settings.behavior.customVibrationPattern.trim())
                .putInt(KEY_BEHAVIOR_SNOOZE_INTERVAL_MINUTES, settings.behavior.snoozeIntervalMinutes.coerceIn(1, 120))
                .putInt(KEY_BEHAVIOR_SNOOZE_COUNT_LIMIT, settings.behavior.snoozeCountLimit.coerceIn(0, 10))
                .putInt(KEY_BEHAVIOR_RING_DURATION_SECONDS, settings.behavior.ringDurationSeconds.coerceIn(10, 3_600))
                .putInt(KEY_BEHAVIOR_RAMP_UP_DURATION_SECONDS, settings.behavior.rampUpDurationSeconds.coerceIn(0, 180))
                .putString(KEY_BEHAVIOR_DEFAULT_SOUND_URI, settings.behavior.defaultSoundUri?.takeIf { it.isNotBlank() })
                .putString(KEY_BEHAVIOR_DEFAULT_SOUND_LABEL, settings.behavior.defaultSoundLabel.trim())
                .putString(
                    KEY_TEMPLATE_CONFIGS_JSON,
                    serializeTemplateConfigs(settings.templateConfigs)
                )
        }

        _settingsFlow.value = loadFromPrefs()
    }

    fun upsertTemplateConfig(config: ShiftTemplateAlarmConfig) {
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

    fun removeTemplateConfig(shiftCode: String) {
        val current = loadFromPrefs()
        save(current.copy(templateConfigs = current.templateConfigs.filterNot { it.shiftCode == shiftCode }))
    }

    fun synchronizeTemplates(templates: List<ShiftTemplateEntity>) {
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
                                title = legacy.title.trim(),
                                manualTitle = legacy.title.isNotBlank(),
                                triggerHour = triggerHour,
                                triggerMinute = triggerMinute,
                                volumePercent = 100,
                                soundUri = null,
                                soundLabel = "",
                                enabled = legacy.enabled,
                                vibrationEnabled = true,
                                vibrationType = ShiftAlarmVibrationType.SYSTEM,
                                vibrationDurationSeconds = 25,
                                customVibrationPattern = "",
                                snoozeIntervalMinutes = 10,
                                snoozeCountLimit = 3,
                                ringDurationSeconds = 180,
                                rampUpDurationSeconds = 0
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
                        manualTitle = if (item.has("manualTitle")) item.optBoolean("manualTitle", true) else true,
                        triggerHour = triggerHour,
                        triggerMinute = triggerMinute,
                        volumePercent = item.optInt("volumePercent", 100).coerceIn(0, 100),
                        soundUri = item.optString("soundUri").ifBlank { null },
                        soundLabel = item.optString("soundLabel"),
                        enabled = item.optBoolean("enabled", true),
                        vibrationEnabled = item.optBoolean("vibrationEnabled", true),
                        vibrationType = runCatching {
                            ShiftAlarmVibrationType.valueOf(
                                item.optString("vibrationType", ShiftAlarmVibrationType.SYSTEM.name)
                            )
                        }.getOrElse { ShiftAlarmVibrationType.SYSTEM },
                        vibrationDurationSeconds = item.optInt("vibrationDurationSeconds", 25).coerceIn(0, 300),
                        customVibrationPattern = item.optString("customVibrationPattern").trim(),
                        snoozeIntervalMinutes = item.optInt("snoozeIntervalMinutes", 10).coerceIn(1, 120),
                        snoozeCountLimit = item.optInt("snoozeCountLimit", 3).coerceIn(0, 10),
                        ringDurationSeconds = item.optInt("ringDurationSeconds", 180).coerceIn(10, 3_600),
                        rampUpDurationSeconds = item.optInt("rampUpDurationSeconds", 0).coerceIn(0, 180)
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
                    put("manualTitle", alarm.manualTitle)
                    put("triggerHour", alarm.triggerHour.coerceIn(0, 23))
                    put("triggerMinute", alarm.triggerMinute.coerceIn(0, 59))
                    put("volumePercent", alarm.volumePercent.coerceIn(0, 100))
                    if (!alarm.soundUri.isNullOrBlank()) put("soundUri", alarm.soundUri)
                    if (alarm.soundLabel.isNotBlank()) put("soundLabel", alarm.soundLabel)
                    put("enabled", alarm.enabled)
                    put("vibrationEnabled", alarm.vibrationEnabled)
                    put("vibrationType", alarm.vibrationType.name)
                    put("vibrationDurationSeconds", alarm.vibrationDurationSeconds.coerceIn(0, 300))
                    if (alarm.customVibrationPattern.isNotBlank()) {
                        put("customVibrationPattern", alarm.customVibrationPattern.trim())
                    }
                    put("snoozeIntervalMinutes", alarm.snoozeIntervalMinutes.coerceIn(1, 120))
                    put("snoozeCountLimit", alarm.snoozeCountLimit.coerceIn(0, 10))
                    put("ringDurationSeconds", alarm.ringDurationSeconds.coerceIn(10, 3_600))
                    put("rampUpDurationSeconds", alarm.rampUpDurationSeconds.coerceIn(0, 180))
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
        private const val KEY_RING_SHOW_CURRENT_CLOCK = "ring_show_current_clock"
        private const val KEY_RING_SHOW_DATE = "ring_show_date"
        private const val KEY_RING_PULSE_ACCENT = "ring_pulse_accent"
        private const val KEY_RING_ANIMATED_GRADIENT = "ring_animated_gradient"
        private const val KEY_RING_ANIMATION_MODE = "ring_animation_mode"
        private const val KEY_RING_ANIMATION_STYLE = "ring_animation_style"
        private const val KEY_RING_VISUAL_STYLE = "ring_visual_style"
        private const val KEY_RING_ACTION_STYLE = "ring_action_style"
        private const val KEY_RING_BUTTONS_LAYOUT = "ring_buttons_layout"
        private const val KEY_RING_CLOCK_ALIGNMENT = "ring_clock_alignment"
        private const val KEY_RING_CLOCK_SCALE = "ring_clock_scale"
        private const val KEY_RING_TEXT_SCALE = "ring_text_scale"
        private const val KEY_RING_USE_MONOSPACE_CLOCK = "ring_use_monospace_clock"
        private const val KEY_RING_SHOW_META_INFO = "ring_show_meta_info"
        private const val KEY_RING_SHOW_SOUND_LABEL = "ring_show_sound_label"
        private const val KEY_RING_SHOW_VOLUME_INFO = "ring_show_volume_info"
        private const val KEY_RING_SHOW_TIMEZONE_INFO = "ring_show_timezone_info"
        private const val KEY_BEHAVIOR_VIBRATION_ENABLED = "behavior_vibration_enabled"
        private const val KEY_BEHAVIOR_VIBRATION_TYPE = "behavior_vibration_type"
        private const val KEY_BEHAVIOR_VIBRATION_DURATION_SECONDS = "behavior_vibration_duration_seconds"
        private const val KEY_BEHAVIOR_CUSTOM_VIBRATION_PATTERN = "behavior_custom_vibration_pattern"
        private const val KEY_BEHAVIOR_SNOOZE_INTERVAL_MINUTES = "behavior_snooze_interval_minutes"
        private const val KEY_BEHAVIOR_SNOOZE_COUNT_LIMIT = "behavior_snooze_count_limit"
        private const val KEY_BEHAVIOR_RING_DURATION_SECONDS = "behavior_ring_duration_seconds"
        private const val KEY_BEHAVIOR_RAMP_UP_DURATION_SECONDS = "behavior_ramp_up_duration_seconds"
        private const val KEY_BEHAVIOR_DEFAULT_SOUND_URI = "behavior_default_sound_uri"
        private const val KEY_BEHAVIOR_DEFAULT_SOUND_LABEL = "behavior_default_sound_label"

        private const val KEY_LEGACY_ALARMS_JSON = "alarms_json"
        private const val KEY_LEGACY_DAY_SHIFT_START_HOUR = "day_shift_start_hour"
        private const val KEY_LEGACY_DAY_SHIFT_START_MINUTE = "day_shift_start_minute"
        private const val KEY_LEGACY_NIGHT_SHIFT_START_HOUR = "night_shift_start_hour"
        private const val KEY_LEGACY_NIGHT_SHIFT_START_MINUTE = "night_shift_start_minute"
        private const val LEGACY_TARGET_DAY = "DAY"
        private const val LEGACY_TARGET_NIGHT = "NIGHT"
    }
}
