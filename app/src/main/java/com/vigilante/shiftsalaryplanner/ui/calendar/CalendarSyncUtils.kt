package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import androidx.core.content.edit

const val PREFS_CALENDAR_SYNC = "calendar_sync_meta"
const val CALENDAR_AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
const val SICK_LIMITS_AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
const val PREFS_SICK_LIMITS_CACHE = "sick_limits_cache"

suspend fun checkAndSyncFederalCalendarIfChanged(
    holidaySyncRepository: HolidaySyncRepository,
    prefs: android.content.SharedPreferences,
    year: Int,
    hasLocalYear: Boolean,
    forceNetworkCheck: Boolean
): CalendarSyncCheckResult {
    val successKey = calendarSyncSuccessKey(year)
    val fingerprintKey = calendarSyncFingerprintKey(year)
    val lastCheckKey = calendarSyncLastCheckKey(year)
    val now = System.currentTimeMillis()
    val lastSuccessAt = prefs.getLong(successKey, 0L)
    val lastCheckAt = prefs.getLong(lastCheckKey, 0L)
    val savedFingerprint = prefs.getString(fingerprintKey, null)

    if (hasLocalYear && !forceNetworkCheck && lastCheckAt > 0L && now - lastCheckAt < CALENDAR_AUTO_CHECK_INTERVAL_MS) {
        return CalendarSyncCheckResult(
            updated = false,
            changedOnServer = false,
            dayCount = 0,
            message = if (lastSuccessAt > 0L) {
                "Используется локальный календарь ${year}. Последняя проверка: ${formatCalendarSyncMoment(lastCheckAt)}"
            } else {
                "Используется локальный календарь $year"
            }
        )
    }

    val latestFingerprint = fetchFederalCalendarFingerprint(year)

    if (hasLocalYear && savedFingerprint != null && latestFingerprint == savedFingerprint) {
        prefs.edit {
            putLong(lastCheckKey, now)
        }

        return CalendarSyncCheckResult(
            updated = false,
            changedOnServer = false,
            dayCount = 0,
            message = "Изменений для календаря $year не найдено. Проверено: ${formatCalendarSyncMoment(now)}"
        )
    }

    val syncedCount = holidaySyncRepository.syncFederalYear(year)
    prefs.edit {
        putLong(successKey, now)
            .putLong(lastCheckKey, now)
            .putString(fingerprintKey, latestFingerprint)
    }

    return CalendarSyncCheckResult(
        updated = true,
        changedOnServer = hasLocalYear,
        dayCount = syncedCount,
        message = when {
            !hasLocalYear -> "Календарь $year загружен и сохранён локально. Дней: $syncedCount"
            savedFingerprint == null -> "Календарь $year проверен и обновлён. Дней: $syncedCount"
            else -> "Найдены изменения на сервере. Календарь $year обновлён. Дней: $syncedCount"
        }
    )
}
private fun calendarSyncSuccessKey(year: Int): String = "federal_year_${year}_success_at"

private fun calendarSyncFingerprintKey(year: Int): String = "federal_year_${year}_fingerprint"

private fun calendarSyncLastCheckKey(year: Int): String = "federal_year_${year}_last_check_at"

fun sha256(text: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private suspend fun fetchFederalCalendarFingerprint(year: Int): String = withContext(Dispatchers.IO) {
    sha256(loadFederalCalendarRawText(year).trim())
}
private fun loadFederalCalendarRawText(year: Int): String {
    val urlString = "https://calendar.kuzyak.in/api/calendar/${year}/holidays"
    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "ShiftSalaryPlanner/1.0")
    }

    return try {
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorText = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            throw IllegalStateException("HTTP ${code}${if (errorText.isNotBlank()) ": $errorText" else ""}")
        }

        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
