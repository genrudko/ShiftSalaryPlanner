package com.vigilante.shiftsalaryplanner.wear.alarm

import org.json.JSONObject

data class WearAlarmPayload(
    val alarmKey: String = "shift_alarm",
    val title: String = "Скоро смена",
    val text: String = "Проверь календарь смен",
    val startedAt: Long = 0L,
    val volumePercent: Int = 100,
    val soundUri: String = "",
    val soundLabel: String = "",
    val wearSoundMode: String = "ALARM",
    val snoozeIntervalMinutes: Int = 10,
    val snoozeCountLimit: Int = 3,
    val snoozeCurrentCount: Int = 0,
    val ringDurationSeconds: Int = 180,
    val rampUpDurationSeconds: Int = 0,
    val vibrationEnabled: Boolean = true,
    val vibrationType: String = "SYSTEM",
    val vibrationDurationSeconds: Int = 25,
    val customVibrationPattern: String = ""
) {
    val canSnooze: Boolean
        get() = snoozeCountLimit > 0 && snoozeCurrentCount < snoozeCountLimit

    fun toJson(): JSONObject {
        return JSONObject()
            .put("alarmKey", alarmKey)
            .put("title", title)
            .put("text", text)
            .put("startedAt", startedAt)
            .put("volumePercent", volumePercent)
            .put("soundUri", soundUri)
            .put("soundLabel", soundLabel)
            .put("wearSoundMode", wearSoundMode)
            .put("snoozeIntervalMinutes", snoozeIntervalMinutes)
            .put("snoozeCountLimit", snoozeCountLimit)
            .put("snoozeCurrentCount", snoozeCurrentCount)
            .put("ringDurationSeconds", ringDurationSeconds)
            .put("rampUpDurationSeconds", rampUpDurationSeconds)
            .put("vibrationEnabled", vibrationEnabled)
            .put("vibrationType", vibrationType)
            .put("vibrationDurationSeconds", vibrationDurationSeconds)
            .put("customVibrationPattern", customVibrationPattern)
    }

    companion object {
        fun fromBytes(payload: ByteArray): WearAlarmPayload {
            return runCatching { fromJson(JSONObject(payload.toString(Charsets.UTF_8))) }
                .getOrDefault(WearAlarmPayload())
        }

        fun fromJson(raw: JSONObject): WearAlarmPayload {
            return WearAlarmPayload(
                alarmKey = raw.optString("alarmKey").ifBlank { "shift_alarm" },
                title = raw.optString("title").ifBlank { "Скоро смена" },
                text = raw.optString("text").ifBlank { "Проверь календарь смен" },
                startedAt = raw.optLong("startedAt", System.currentTimeMillis()),
                volumePercent = raw.optInt("volumePercent", 100).coerceIn(0, 100),
                soundUri = raw.optString("soundUri"),
                soundLabel = raw.optString("soundLabel"),
                wearSoundMode = raw.optString("wearSoundMode").ifBlank { "ALARM" },
                snoozeIntervalMinutes = raw.optInt("snoozeIntervalMinutes", 10).coerceIn(1, 120),
                snoozeCountLimit = raw.optInt("snoozeCountLimit", 3).coerceIn(0, 10),
                snoozeCurrentCount = raw.optInt("snoozeCurrentCount", 0).coerceAtLeast(0),
                ringDurationSeconds = raw.optInt("ringDurationSeconds", 180).coerceIn(10, 3_600),
                rampUpDurationSeconds = raw.optInt("rampUpDurationSeconds", 0).coerceIn(0, 180),
                vibrationEnabled = raw.optBoolean("vibrationEnabled", true),
                vibrationType = raw.optString("vibrationType").ifBlank { "SYSTEM" },
                vibrationDurationSeconds = raw.optInt("vibrationDurationSeconds", 25).coerceIn(0, 300),
                customVibrationPattern = raw.optString("customVibrationPattern").trim()
            )
        }
    }
}
