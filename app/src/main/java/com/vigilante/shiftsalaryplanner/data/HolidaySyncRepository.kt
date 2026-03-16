package com.vigilante.shiftsalaryplanner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale

class HolidaySyncRepository(
    private val holidayDao: HolidayDao
) {
    suspend fun syncFederalYear(year: Int): Int = withContext(Dispatchers.IO) {
        val url = "https://calendar.kuzyak.in/api/calendar/$year/holidays"
        val jsonText = loadText(url)
        val items = buildFederalHolidayItems(year, jsonText)

        if (items.isEmpty()) {
            throw IllegalStateException(
                "API вернул 0 дней. Ответ: " + jsonText.take(700)
            )
        }

        holidayDao.deleteByYear(
            scopeCode = "RU-FED",
            yearMask = "$year-%"
        )
        holidayDao.upsertAll(items)

        items.size
    }

    private fun buildFederalHolidayItems(
        year: Int,
        jsonText: String
    ): List<HolidayEntity> {
        val fixedItems = fixedFederalHolidays(year)
        val fixedDates = fixedItems.map { it.date }.toSet()

        val apiDays = parseApiSpecialDays(jsonText)

        val result = linkedMapOf<String, HolidayEntity>()

        fixedItems
            .sortedBy { it.date }
            .forEach { result[it.date] = it }

        apiDays
            .sortedWith(compareBy({ it.date }, { it.kind }))
            .forEach { apiDay ->
                val entity = finalizeApiDay(apiDay, fixedDates) ?: return@forEach
                result[entity.date] = entity
            }

        return result.values.sortedBy { it.date }
    }

    private fun finalizeApiDay(
        day: ParsedHolidayDay,
        fixedDates: Set<String>
    ): HolidayEntity? {
        if (!DATE_REGEX.matches(day.date)) return null

        if (day.date in fixedDates) {
            return null
        }

        val finalKind = when {
            day.kind == HolidayKinds.SHORT_DAY -> HolidayKinds.SHORT_DAY
            day.kind == HolidayKinds.TRANSFERRED_DAY_OFF -> HolidayKinds.TRANSFERRED_DAY_OFF
            day.isNonWorking -> HolidayKinds.TRANSFERRED_DAY_OFF
            else -> HolidayKinds.HOLIDAY
        }

        val finalTitle = when (finalKind) {
            HolidayKinds.SHORT_DAY -> day.title.ifBlank { "Сокращённый день" }
            HolidayKinds.TRANSFERRED_DAY_OFF -> "Перенесённый выходной"
            else -> day.title.ifBlank { "Праздничный день" }
        }

        val finalIsNonWorking = finalKind != HolidayKinds.SHORT_DAY

        return HolidayEntity(
            id = "RU-FED|${day.date}|$finalKind",
            date = day.date,
            title = finalTitle,
            scopeCode = "RU-FED",
            kind = finalKind,
            isNonWorking = finalIsNonWorking
        )
    }

    private fun fixedFederalHolidays(year: Int): List<HolidayEntity> {
        val items = mutableListOf<Pair<LocalDate, String>>()

        for (day in 1..6) {
            items += LocalDate.of(year, 1, day) to "Новогодние каникулы"
        }
        items += LocalDate.of(year, 1, 7) to "Рождество Христово"
        items += LocalDate.of(year, 1, 8) to "Новогодние каникулы"

        items += LocalDate.of(year, 2, 23) to "День защитника Отечества"
        items += LocalDate.of(year, 3, 8) to "Международный женский день"
        items += LocalDate.of(year, 5, 1) to "Праздник Весны и Труда"
        items += LocalDate.of(year, 5, 9) to "День Победы"
        items += LocalDate.of(year, 6, 12) to "День России"
        items += LocalDate.of(year, 11, 4) to "День народного единства"

        return items
            .distinctBy { it.first }
            .sortedBy { it.first }
            .map { (date, title) ->
                HolidayEntity(
                    id = "RU-FED|${date}|${HolidayKinds.HOLIDAY}",
                    date = date.toString(),
                    title = title,
                    scopeCode = "RU-FED",
                    kind = HolidayKinds.HOLIDAY,
                    isNonWorking = true
                )
            }
    }

    private fun loadText(urlString: String): String {
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

                throw IllegalStateException(
                    "HTTP $code${if (errorText.isNotBlank()) ": $errorText" else ""}"
                )
            }

            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseApiSpecialDays(jsonText: String): List<ParsedHolidayDay> {
        val trimmed = jsonText.trim()
        val result = mutableListOf<ParsedHolidayDay>()

        if (trimmed.startsWith("[")) {
            parseNamedHolidayArray(
                array = JSONArray(trimmed),
                result = result,
                kind = HolidayKinds.HOLIDAY,
                isNonWorking = true
            )

            return result
                .sortedWith(compareBy({ it.date }, { it.kind }))
                .distinctBy { "${it.date}|${it.kind}" }
        }

        val root = JSONObject(trimmed)

        parseNamedHolidayArray(
            array = root.optJSONArray("holidays"),
            result = result,
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )

        parseNamedHolidayArray(
            array = root.optJSONArray("shortDays"),
            result = result,
            kind = HolidayKinds.SHORT_DAY,
            isNonWorking = false
        )

        parseNamedHolidayArray(
            array = root.optJSONArray("preholidays"),
            result = result,
            kind = HolidayKinds.SHORT_DAY,
            isNonWorking = false
        )

        parseFlexibleDayArray(root.optJSONArray("days"), result)
        parseFlexibleDayArray(root.optJSONArray("data"), result)
        parseFlexibleDayArray(root.optJSONArray("items"), result)

        return result
            .sortedWith(compareBy({ it.date }, { it.kind }))
            .distinctBy { "${it.date}|${it.kind}" }
    }

    private fun parseNamedHolidayArray(
        array: JSONArray?,
        result: MutableList<ParsedHolidayDay>,
        kind: String,
        isNonWorking: Boolean
    ) {
        if (array == null) return

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            val rawDate = pickString(item, "date", "day", "fullDate", "isoDate")
            val date = normalizeApiDate(rawDate)
            if (!DATE_REGEX.matches(date)) continue

            val title = pickString(item, "name", "title", "description")

            result += ParsedHolidayDay(
                date = date,
                title = title,
                kind = kind,
                isNonWorking = isNonWorking
            )
        }
    }

    private fun parseFlexibleDayArray(
        array: JSONArray?,
        result: MutableList<ParsedHolidayDay>
    ) {
        if (array == null) return

        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is JSONObject -> {
                    val rawDate = pickString(item, "date", "day", "fullDate", "isoDate")
                    val date = normalizeApiDate(rawDate)
                    if (!DATE_REGEX.matches(date)) continue

                    val title = pickString(item, "title", "name", "description")
                    val type = pickString(item, "type", "kind", "dayType", "status")
                        .lowercase(Locale.ROOT)

                    val isShortDay =
                        type.contains("short") ||
                                type.contains("pre") ||
                                type.contains("сокр") ||
                                pickBoolean(
                                    item,
                                    "isShortDay",
                                    "isShortened",
                                    "shortened",
                                    "isPreholiday",
                                    "preholiday"
                                ) == true

                    val isTransferDay =
                        type.contains("transfer") ||
                                type.contains("observ") ||
                                type.contains("перен")

                    val isNonWorkingExplicit = pickBoolean(
                        item,
                        "isHoliday",
                        "holiday",
                        "isDayOff",
                        "dayoff",
                        "isNonWorking",
                        "nonWorking",
                        "isWeekend",
                        "weekend"
                    )

                    val isHolidayLike =
                        type.contains("holiday") ||
                                type.contains("dayoff") ||
                                type.contains("weekend") ||
                                type.contains("nonworking") ||
                                type.contains("празд") ||
                                type.contains("выход")

                    if (!isHolidayLike && !isShortDay && isNonWorkingExplicit != true && !isTransferDay) {
                        continue
                    }

                    val kind = when {
                        isShortDay -> HolidayKinds.SHORT_DAY
                        isTransferDay -> HolidayKinds.TRANSFERRED_DAY_OFF
                        else -> HolidayKinds.HOLIDAY
                    }

                    val isNonWorking = when {
                        kind == HolidayKinds.SHORT_DAY -> false
                        isNonWorkingExplicit != null -> isNonWorkingExplicit
                        else -> true
                    }

                    result += ParsedHolidayDay(
                        date = date,
                        title = title,
                        kind = kind,
                        isNonWorking = isNonWorking
                    )
                }

                is String -> {
                    val date = normalizeApiDate(item)
                    if (DATE_REGEX.matches(date)) {
                        result += ParsedHolidayDay(
                            date = date,
                            title = "",
                            kind = HolidayKinds.HOLIDAY,
                            isNonWorking = true
                        )
                    }
                }
            }
        }
    }

    private fun normalizeApiDate(raw: String): String {
        return raw
            .trim()
            .substringBefore("T")
            .substringBefore(" ")
    }

    private fun pickString(obj: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            if (!obj.has(key)) return@forEach

            val value = obj.opt(key)
            if (value != null && value != JSONObject.NULL) {
                val text = value.toString().trim()
                if (text.isNotBlank() && text != "null") {
                    return text
                }
            }
        }
        return ""
    }

    private fun pickBoolean(obj: JSONObject, vararg keys: String): Boolean? {
        keys.forEach { key ->
            if (!obj.has(key)) return@forEach

            val value = obj.opt(key) ?: return@forEach
            if (value == JSONObject.NULL) return@forEach

            when (value) {
                is Boolean -> return value
                is Number -> return value.toInt() != 0
                is String -> {
                    return when (value.trim().lowercase(Locale.ROOT)) {
                        "true", "1", "yes", "y" -> true
                        "false", "0", "no", "n" -> false
                        else -> null
                    }
                }
            }
        }
        return null
    }

    private data class ParsedHolidayDay(
        val date: String,
        val title: String,
        val kind: String,
        val isNonWorking: Boolean
    )

    private companion object {
        val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
