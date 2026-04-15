package com.vigilante.shiftsalaryplanner

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val fallbackSickInsuranceBaseLimits = mapOf(
    2023 to 1_917_000.0,
    2024 to 2_225_000.0,
    2025 to 2_759_000.0,
    2026 to 2_979_000.0
)

fun formatWholeNumber(value: Double): String = value.toLong().toString()

suspend fun fetchSickInsuranceBaseLimitsFromInternet(
    vararg years: Int
): Map<Int, Double> = withContext(Dispatchers.IO) {
    val requestedYears = years.distinct().filter { it >= 2023 }
    if (requestedYears.isEmpty()) return@withContext emptyMap()

    val result = mutableMapOf<Int, Double>()
    val urls = listOf(
        "https://www.nalog.gov.ru/rn77/taxation/insprem/",
        "https://www.nalog.gov.ru/rn77/ip/prem_employ/"
    )

    urls.forEach { url ->
        if (requestedYears.all { result.containsKey(it) }) return@forEach
        runCatching { downloadUrlText(url) }.getOrNull()?.let { html ->
            val normalized = html
                .replace("&nbsp;", " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")

            requestedYears.forEach { year ->
                if (!result.containsKey(year)) {
                    extractInsuranceBaseLimitFromText(normalized, year)?.let { result[year] = it }
                }
            }
        }
    }

    requestedYears.forEach { year ->
        if (!result.containsKey(year)) {
            fallbackSickInsuranceBaseLimits[year]?.let { result[year] = it }
        }
    }

    if (result.isEmpty()) {
        throw IllegalStateException("Не удалось определить лимиты")
    }
    result
}

private fun extractInsuranceBaseLimitFromText(
    text: String,
    year: Int
): Double? {
    val yearWindow = Regex("""\b$year\b.{0,260}""")
        .find(text)
        ?.value
        ?: return null

    return Regex("""\d[\d\s]{5,15}\d""")
        .findAll(yearWindow)
        .mapNotNull { it.value.filter(Char::isDigit).toLongOrNull() }
        .firstOrNull { value -> value in 1_000_000L..9_999_999L }
        ?.toDouble()
}

private fun downloadUrlText(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Mobile Safari/537.36"
        )
    }

    return try {
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

data class SickLimitsSyncResult(
    val updated: Boolean,
    val changedOnServer: Boolean,
    val limits: Map<Int, Double>,
    val message: String
)

private fun sickLimitsValueKey(year: Int): String = "sick_limit_${year}_value"

private fun sickLimitsFingerprintKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_fingerprint"
}

private fun sickLimitsLastCheckKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_last_check_at"
}

private fun sickLimitsSuccessKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_success_at"
}

fun readCachedSickInsuranceBaseLimits(
    prefs: SharedPreferences,
    vararg years: Int
): Map<Int, Double> {
    return buildMap {
        years.distinct().forEach { year ->
            val value = prefs.getFloat(sickLimitsValueKey(year), Float.NaN)
            if (!value.isNaN()) put(year, value.toDouble())
        }
    }
}

suspend fun checkAndFetchSickInsuranceBaseLimitsIfChanged(
    prefs: SharedPreferences,
    year1: Int,
    year2: Int,
    forceNetworkCheck: Boolean
): SickLimitsSyncResult {
    val cachedLimits = readCachedSickInsuranceBaseLimits(prefs, year1, year2)
    val hasCachedLimits = cachedLimits.containsKey(year1) && cachedLimits.containsKey(year2)
    val now = System.currentTimeMillis()

    val lastCheckKey = sickLimitsLastCheckKey(year1, year2)
    val successKey = sickLimitsSuccessKey(year1, year2)
    val fingerprintKey = sickLimitsFingerprintKey(year1, year2)
    val lastCheckAt = prefs.getLong(lastCheckKey, 0L)
    val lastSuccessAt = prefs.getLong(successKey, 0L)
    val savedFingerprint = prefs.getString(fingerprintKey, null)

    if (hasCachedLimits && !forceNetworkCheck && lastCheckAt > 0L && now - lastCheckAt < SICK_LIMITS_AUTO_CHECK_INTERVAL_MS) {
        return SickLimitsSyncResult(
            updated = false,
            changedOnServer = false,
            limits = cachedLimits,
            message = if (lastSuccessAt > 0L) {
                "Используются локальные лимиты. Последняя проверка: ${formatCalendarSyncMoment(lastCheckAt)}"
            } else {
                "Используются локально сохранённые лимиты"
            }
        )
    }

    val latestLimits = fetchSickInsuranceBaseLimitsFromInternet(year1, year2)
    val latestFingerprint = sha256(
        latestLimits.toSortedMap().entries.joinToString(separator = "|") { (year, value) ->
            "$year=${formatWholeNumber(value)}"
        }
    )

    if (hasCachedLimits && savedFingerprint != null && latestFingerprint == savedFingerprint) {
        prefs.edit { putLong(lastCheckKey, now) }
        return SickLimitsSyncResult(
            updated = false,
            changedOnServer = false,
            limits = cachedLimits,
            message = "Изменений по лимитам не найдено. Проверено: ${formatCalendarSyncMoment(now)}"
        )
    }

    prefs.edit {
        latestLimits.forEach { (year, value) ->
            putFloat(sickLimitsValueKey(year), value.toFloat())
        }
        putLong(lastCheckKey, now)
        putLong(successKey, now)
        putString(fingerprintKey, latestFingerprint)
    }

    return SickLimitsSyncResult(
        updated = true,
        changedOnServer = hasCachedLimits,
        limits = latestLimits,
        message = when {
            !hasCachedLimits -> "Лимиты ФНС загружены и сохранены локально"
            savedFingerprint == null -> "Лимиты ФНС проверены и обновлены"
            else -> "Найдены изменения по лимитам. Локальные данные обновлены"
        }
    )
}

