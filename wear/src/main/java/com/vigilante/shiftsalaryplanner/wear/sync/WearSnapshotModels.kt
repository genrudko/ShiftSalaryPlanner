package com.vigilante.shiftsalaryplanner.wear.sync

import org.json.JSONArray
import org.json.JSONObject

data class WearSnapshot(
    val generatedAt: Long = 0L,
    val today: String = "",
    val month: String = "",
    val todaySummary: String = "Жду данные с телефона",
    val tomorrowSummary: String = "",
    val calendar: List<WearCalendarDay> = emptyList(),
    val payroll: WearPayroll = WearPayroll(),
    val alarms: WearAlarms = WearAlarms(),
    val notes: List<WearNote> = emptyList(),
    val assistant: WearAssistant = WearAssistant()
) {
    companion object {
        fun fromJson(raw: String): WearSnapshot {
            val root = JSONObject(raw)
            return WearSnapshot(
                generatedAt = root.optLong("generatedAt", 0L),
                today = root.optString("today"),
                month = root.optString("month"),
                todaySummary = root.optString("todaySummary").ifBlank { "Сегодня без смен" },
                tomorrowSummary = root.optString("tomorrowSummary"),
                calendar = root.optJSONArray("calendar").toItems(::parseCalendarDay),
                payroll = parsePayroll(root.optJSONObject("payroll") ?: JSONObject()),
                alarms = parseAlarms(root.optJSONObject("alarms") ?: JSONObject()),
                notes = root.optJSONArray("notes").toItems(::parseNote),
                assistant = parseAssistant(root.optJSONObject("assistant") ?: JSONObject())
            )
        }

        private fun parseCalendarDay(item: JSONObject): WearCalendarDay {
            return WearCalendarDay(
                date = item.optString("date"),
                day = item.optInt("day"),
                dayOfWeek = item.optInt("dayOfWeek"),
                isToday = item.optBoolean("isToday"),
                notesCount = item.optInt("notesCount"),
                assignments = item.optJSONArray("assignments").toItems(::parseAssignment)
            )
        }

        private fun parseAssignment(item: JSONObject): WearAssignment {
            return WearAssignment(
                workplaceName = item.optString("workplaceName"),
                shiftCode = item.optString("shiftCode"),
                displayCode = item.optString("displayCode"),
                title = item.optString("title"),
                iconKey = item.optString("iconKey").ifBlank { "TEXT" },
                colorHex = item.optString("colorHex").ifBlank { "#7AE582" },
                paidHours = item.optDouble("paidHours", 0.0),
                start = item.optString("start"),
                end = item.optString("end")
            )
        }

        private fun parsePayroll(item: JSONObject): WearPayroll {
            return WearPayroll(
                periodLabel = item.optString("periodLabel"),
                workedHours = item.optDouble("workedHours", 0.0),
                nightHours = item.optDouble("nightHours", 0.0),
                grossTotal = item.optDouble("grossTotal", 0.0),
                netTotal = item.optDouble("netTotal", 0.0),
                advance = item.optDouble("advance", 0.0),
                salary = item.optDouble("salary", 0.0),
                ndfl = item.optDouble("ndfl", 0.0),
                advanceDate = item.optString("advanceDate"),
                salaryDate = item.optString("salaryDate")
            )
        }

        private fun parseAlarms(item: JSONObject): WearAlarms {
            return WearAlarms(
                enabled = item.optBoolean("enabled"),
                autoReschedule = item.optBoolean("autoReschedule", true),
                wearMirrorEnabled = item.optBoolean("wearMirrorEnabled"),
                wearSoundMode = item.optString("wearSoundMode").ifBlank { "ALARM" },
                upcoming = item.optJSONArray("upcoming").toItems(::parseUpcomingAlarm),
                templates = item.optJSONArray("templates").toItems(::parseTemplateAlarm)
            )
        }

        private fun parseUpcomingAlarm(item: JSONObject): WearUpcomingAlarm {
            return WearUpcomingAlarm(
                triggerAtMillis = item.optLong("triggerAtMillis"),
                title = item.optString("title"),
                text = item.optString("text"),
                shiftCode = item.optString("shiftCode")
            )
        }

        private fun parseTemplateAlarm(item: JSONObject): WearTemplateAlarm {
            return WearTemplateAlarm(
                shiftCode = item.optString("shiftCode"),
                displayCode = item.optString("displayCode"),
                title = item.optString("title"),
                colorHex = item.optString("colorHex").ifBlank { "#7AE582" },
                enabled = item.optBoolean("enabled"),
                start = item.optString("start"),
                alarmsEnabledCount = item.optInt("alarmsEnabledCount")
            )
        }

        private fun parseNote(item: JSONObject): WearNote {
            return WearNote(
                id = item.optString("id"),
                date = item.optString("date"),
                title = item.optString("title"),
                body = item.optString("body")
            )
        }

        private fun parseAssistant(item: JSONObject): WearAssistant {
            return WearAssistant(
                provider = item.optString("provider"),
                configured = item.optBoolean("configured"),
                lastReply = item.optString("lastReply"),
                lastError = item.optString("lastError")
            )
        }
    }
}

data class WearCalendarDay(
    val date: String,
    val day: Int,
    val dayOfWeek: Int,
    val isToday: Boolean,
    val notesCount: Int,
    val assignments: List<WearAssignment>
)

data class WearAssignment(
    val workplaceName: String,
    val shiftCode: String,
    val displayCode: String,
    val title: String,
    val iconKey: String,
    val colorHex: String,
    val paidHours: Double,
    val start: String,
    val end: String
)

data class WearPayroll(
    val periodLabel: String = "",
    val workedHours: Double = 0.0,
    val nightHours: Double = 0.0,
    val grossTotal: Double = 0.0,
    val netTotal: Double = 0.0,
    val advance: Double = 0.0,
    val salary: Double = 0.0,
    val ndfl: Double = 0.0,
    val advanceDate: String = "",
    val salaryDate: String = ""
)

data class WearAlarms(
    val enabled: Boolean = false,
    val autoReschedule: Boolean = true,
    val wearMirrorEnabled: Boolean = false,
    val wearSoundMode: String = "ALARM",
    val upcoming: List<WearUpcomingAlarm> = emptyList(),
    val templates: List<WearTemplateAlarm> = emptyList()
)

data class WearUpcomingAlarm(
    val triggerAtMillis: Long,
    val title: String,
    val text: String,
    val shiftCode: String
)

data class WearTemplateAlarm(
    val shiftCode: String,
    val displayCode: String,
    val title: String,
    val colorHex: String,
    val enabled: Boolean,
    val start: String,
    val alarmsEnabledCount: Int
)

data class WearNote(
    val id: String,
    val date: String,
    val title: String,
    val body: String
)

data class WearAssistant(
    val provider: String = "",
    val configured: Boolean = false,
    val lastReply: String = "",
    val lastError: String = ""
)

private fun <T> JSONArray?.toItems(parser: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(parser(item))
        }
    }
}
