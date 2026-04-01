package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import java.time.LocalDate
import java.util.Locale
import androidx.core.content.edit

fun formatCalendarSyncMoment(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    return java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
}

fun readManualHolidayRecords(prefs: android.content.SharedPreferences): List<ManualHolidayRecord> {
    return prefs.getStringSet("items", emptySet())
        ?.mapNotNull { serialized ->
            parseManualHolidayRecord(serialized)
        }
        ?.sortedBy { it.date }
        ?: emptyList()
}

fun writeManualHolidayRecords(
    prefs: android.content.SharedPreferences,
    records: List<ManualHolidayRecord>
) {
    prefs.edit {
        putStringSet(
            "items",
            records.map { serializeManualHolidayRecord(it) }.toSet()
        )
    }
}

fun serializeManualHolidayRecord(record: ManualHolidayRecord): String {
    val safeTitle = record.title.replace(MANUAL_HOLIDAY_SEPARATOR, " ").replace("\n", " ").trim()
    val nonWorkingFlag = if (record.isNonWorking) "1" else "0"
    return listOf(record.date, safeTitle, record.kind, nonWorkingFlag)
        .joinToString(MANUAL_HOLIDAY_SEPARATOR)
}

fun parseManualHolidayRecord(serialized: String): ManualHolidayRecord? {
    val parts = serialized.split(MANUAL_HOLIDAY_SEPARATOR)
    if (parts.size < 4) return null

    return runCatching {
        LocalDate.parse(parts[0])
        ManualHolidayRecord(
            date = parts[0],
            title = parts[1],
            kind = parts[2],
            isNonWorking = parts[3] == "1"
        )
    }.getOrNull()
}

fun ManualHolidayRecord.toHolidayEntity(): HolidayEntity {
    return HolidayEntity(
        id = "MANUAL|$date|$kind",
        date = date,
        title = title,
        scopeCode = MANUAL_HOLIDAY_SCOPE,
        kind = kind,
        isNonWorking = isNonWorking
    )
}
