package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.weather.NormalizedWeather
import com.vigilante.shiftsalaryplanner.weather.WeatherCache
import com.vigilante.shiftsalaryplanner.weather.WeatherCacheEntry
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherCurrent
import com.vigilante.shiftsalaryplanner.weather.WeatherDailyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherHourlyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses the "latitude,longitude" cache key back into authoritative coordinates.
 *
 * The key uses the same locale-independent format as `weatherCacheKey`; keys
 * that are empty, non-numeric, out-of-range or lacking a separator are rejected.
 */
fun weatherCoordinatesFromCacheKey(key: String): WeatherCoordinates? {
    val commaIndex = key.indexOf(',')
    if (commaIndex <= 0 || commaIndex >= key.length - 1) return null
    val latitude = key.substring(0, commaIndex).toDoubleOrNull() ?: return null
    val longitude = key.substring(commaIndex + 1).toDoubleOrNull() ?: return null
    return WeatherCoordinates.of(latitude, longitude)
}

/**
 * Schema-versioned JSON encoding of one [WeatherCacheEntry] under [key].
 *
 * The document carries the schema version, the authoritative coordinates (which
 * must match [key]), the producing provider, the acquisition timestamp and the
 * full normalized payload.
 */
fun encodeWeatherCacheEntry(key: String, entry: WeatherCacheEntry): String {
    val coordinates = weatherCoordinatesFromCacheKey(key)
        ?: throw IllegalArgumentException("Invalid weather cache key: $key")
    val root = JSONObject()
    root.put("schemaVersion", WeatherCacheStore.SCHEMA_VERSION)
    root.put("coordinates", encodeCoordinates(coordinates))
    root.put("provider", entry.provider.name)
    root.put("fetchedAtMillis", entry.fetchedAtMillis)
    root.put("weather", JSONObject(encodeNormalizedWeather(entry.weather)))
    return root.toString()
}

/**
 * Forgiving decode of a cached weather entry.
 *
 * Returns null (and therefore the store ignores the entry) for malformed JSON,
 * unknown schema versions, unknown providers and coordinate/key mismatches.
 * Forward-compatible unknown fields inside a known-schema document are ignored.
 */
fun decodeWeatherCacheEntry(key: String, raw: String): WeatherCacheEntry? {
    return try {
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion", 0) != WeatherCacheStore.SCHEMA_VERSION) return null
        val keyCoordinates = weatherCoordinatesFromCacheKey(key) ?: return null
        val storedCoordinates = decodeCoordinates(root.optJSONObject("coordinates")) ?: return null
        if (storedCoordinates != keyCoordinates) return null
        val provider = WeatherProviderId.values().firstOrNull { it.name == root.optString("provider") } ?: return null
        val fetchedAtMillis = root.optLong("fetchedAtMillis", 0L)
        val weather = decodeNormalizedWeather(root.optJSONObject("weather")) ?: return null
        WeatherCacheEntry(weather = weather, provider = provider, fetchedAtMillis = fetchedAtMillis)
    } catch (e: JSONException) {
        null
    }
}

/** Schema-versioned JSON encoding of the normalized weather payload. */
fun encodeNormalizedWeather(weather: NormalizedWeather): String {
    val root = JSONObject()
    root.put("coordinates", encodeCoordinates(weather.coordinates))
    root.put("fetchedAtMillis", weather.fetchedAtMillis)
    weather.current?.let { root.put("current", encodeCurrent(it)) }
    val hourly = JSONArray()
    weather.hourly.forEach { hourly.put(encodeHourlyPoint(it)) }
    root.put("hourly", hourly)
    val daily = JSONArray()
    weather.daily.forEach { daily.put(encodeDailyPoint(it)) }
    root.put("daily", daily)
    return root.toString()
}

/**
 * Decodes the normalized payload. Malformed points are skipped; a payload whose
 * coordinates or required temperature fields are missing is rejected as null.
 */
fun decodeNormalizedWeather(root: JSONObject?): NormalizedWeather? {
    if (root == null) return null
    val coordinates = decodeCoordinates(root.optJSONObject("coordinates")) ?: return null
    val fetchedAtMillis = root.optLong("fetchedAtMillis", 0L)
    val current = root.optJSONObject("current")?.let { decodeCurrent(it) }
    val hourly = decodePointList(root.optJSONArray("hourly")) { decodeHourlyPoint(it) }
    val daily = decodePointList(root.optJSONArray("daily")) { decodeDailyPoint(it) }
    return NormalizedWeather(
        coordinates = coordinates,
        fetchedAtMillis = fetchedAtMillis,
        current = current,
        hourly = hourly,
        daily = daily,
    )
}

private fun encodeCoordinates(coordinates: WeatherCoordinates): JSONObject =
    JSONObject()
        .put("latitude", coordinates.latitude)
        .put("longitude", coordinates.longitude)

private fun decodeCoordinates(obj: JSONObject?): WeatherCoordinates? {
    if (obj == null) return null
    val latitude = obj.optDoubleOrNull("latitude") ?: return null
    val longitude = obj.optDoubleOrNull("longitude") ?: return null
    return WeatherCoordinates.of(latitude, longitude)
}

private fun encodeCurrent(current: WeatherCurrent): JSONObject = JSONObject().apply {
    put("observedAtMillis", current.observedAtMillis)
    put("temperatureCelsius", current.temperatureCelsius)
    putOpt("feelsLikeCelsius", current.feelsLikeCelsius)
    putOpt("conditionCode", current.conditionCode)
    putOpt("relativeHumidityPercent", current.relativeHumidityPercent)
    putOpt("pressureHpa", current.pressureHpa)
    putOpt("windSpeedMs", current.windSpeedMs)
    putOpt("windGustMs", current.windGustMs)
    putOpt("windDirectionDegrees", current.windDirectionDegrees)
    putOpt("precipitationMm", current.precipitationMm)
    putOpt("visibilityMetres", current.visibilityMetres)
    putOpt("uvIndex", current.uvIndex)
}

private fun decodeCurrent(obj: JSONObject): WeatherCurrent? {
    val observedAtMillis = obj.optLong("observedAtMillis", 0L)
    val temperatureCelsius = obj.optDoubleOrNull("temperatureCelsius") ?: return null
    return WeatherCurrent(
        observedAtMillis = observedAtMillis,
        temperatureCelsius = temperatureCelsius,
        feelsLikeCelsius = obj.optDoubleOrNull("feelsLikeCelsius"),
        conditionCode = obj.optStringOrNull("conditionCode"),
        relativeHumidityPercent = obj.optDoubleOrNull("relativeHumidityPercent"),
        pressureHpa = obj.optDoubleOrNull("pressureHpa"),
        windSpeedMs = obj.optDoubleOrNull("windSpeedMs"),
        windGustMs = obj.optDoubleOrNull("windGustMs"),
        windDirectionDegrees = obj.optDoubleOrNull("windDirectionDegrees"),
        precipitationMm = obj.optDoubleOrNull("precipitationMm"),
        visibilityMetres = obj.optDoubleOrNull("visibilityMetres"),
        uvIndex = obj.optDoubleOrNull("uvIndex"),
    )
}

private fun encodeHourlyPoint(point: WeatherHourlyPoint): JSONObject = JSONObject().apply {
    put("forecastTimeMillis", point.forecastTimeMillis)
    put("temperatureCelsius", point.temperatureCelsius)
    putOpt("feelsLikeCelsius", point.feelsLikeCelsius)
    putOpt("conditionCode", point.conditionCode)
    putOpt("relativeHumidityPercent", point.relativeHumidityPercent)
    putOpt("pressureHpa", point.pressureHpa)
    putOpt("windSpeedMs", point.windSpeedMs)
    putOpt("windGustMs", point.windGustMs)
    putOpt("windDirectionDegrees", point.windDirectionDegrees)
    putOpt("precipitationProbabilityPercent", point.precipitationProbabilityPercent)
    putOpt("precipitationMm", point.precipitationMm)
    putOpt("visibilityMetres", point.visibilityMetres)
}

private fun decodeHourlyPoint(obj: JSONObject): WeatherHourlyPoint? {
    val forecastTimeMillis = obj.optLong("forecastTimeMillis", 0L)
    val temperatureCelsius = obj.optDoubleOrNull("temperatureCelsius") ?: return null
    return WeatherHourlyPoint(
        forecastTimeMillis = forecastTimeMillis,
        temperatureCelsius = temperatureCelsius,
        feelsLikeCelsius = obj.optDoubleOrNull("feelsLikeCelsius"),
        conditionCode = obj.optStringOrNull("conditionCode"),
        relativeHumidityPercent = obj.optDoubleOrNull("relativeHumidityPercent"),
        pressureHpa = obj.optDoubleOrNull("pressureHpa"),
        windSpeedMs = obj.optDoubleOrNull("windSpeedMs"),
        windGustMs = obj.optDoubleOrNull("windGustMs"),
        windDirectionDegrees = obj.optDoubleOrNull("windDirectionDegrees"),
        precipitationProbabilityPercent = obj.optDoubleOrNull("precipitationProbabilityPercent"),
        precipitationMm = obj.optDoubleOrNull("precipitationMm"),
        visibilityMetres = obj.optDoubleOrNull("visibilityMetres"),
    )
}

private fun encodeDailyPoint(point: WeatherDailyPoint): JSONObject = JSONObject().apply {
    put("forecastDayMillis", point.forecastDayMillis)
    put("minTemperatureCelsius", point.minTemperatureCelsius)
    put("maxTemperatureCelsius", point.maxTemperatureCelsius)
    putOpt("conditionCode", point.conditionCode)
    putOpt("relativeHumidityPercent", point.relativeHumidityPercent)
    putOpt("pressureHpa", point.pressureHpa)
    putOpt("windSpeedMs", point.windSpeedMs)
    putOpt("windGustMs", point.windGustMs)
    putOpt("windDirectionDegrees", point.windDirectionDegrees)
    putOpt("precipitationProbabilityPercent", point.precipitationProbabilityPercent)
    putOpt("precipitationMm", point.precipitationMm)
    putOpt("sunriseMillis", point.sunriseMillis)
    putOpt("sunsetMillis", point.sunsetMillis)
    putOpt("uvIndexMax", point.uvIndexMax)
}

private fun decodeDailyPoint(obj: JSONObject): WeatherDailyPoint? {
    val forecastDayMillis = obj.optLong("forecastDayMillis", 0L)
    val minTemperatureCelsius = obj.optDoubleOrNull("minTemperatureCelsius") ?: return null
    val maxTemperatureCelsius = obj.optDoubleOrNull("maxTemperatureCelsius") ?: return null
    return WeatherDailyPoint(
        forecastDayMillis = forecastDayMillis,
        minTemperatureCelsius = minTemperatureCelsius,
        maxTemperatureCelsius = maxTemperatureCelsius,
        conditionCode = obj.optStringOrNull("conditionCode"),
        relativeHumidityPercent = obj.optDoubleOrNull("relativeHumidityPercent"),
        pressureHpa = obj.optDoubleOrNull("pressureHpa"),
        windSpeedMs = obj.optDoubleOrNull("windSpeedMs"),
        windGustMs = obj.optDoubleOrNull("windGustMs"),
        windDirectionDegrees = obj.optDoubleOrNull("windDirectionDegrees"),
        precipitationProbabilityPercent = obj.optDoubleOrNull("precipitationProbabilityPercent"),
        precipitationMm = obj.optDoubleOrNull("precipitationMm"),
        sunriseMillis = obj.longOrNull("sunriseMillis"),
        sunsetMillis = obj.longOrNull("sunsetMillis"),
        uvIndexMax = obj.optDoubleOrNull("uvIndexMax"),
    )
}

private fun <T> decodePointList(array: JSONArray?, decode: (JSONObject) -> T?): List<T> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            decode(array.optJSONObject(index))?.let { add(it) }
        }
    }
}

private fun JSONObject.putOpt(name: String, value: Double?) {
    if (value != null) put(name, value)
}

private fun JSONObject.putOpt(name: String, value: Long?) {
    if (value != null) put(name, value)
}

private fun JSONObject.putOpt(name: String, value: String?) {
    if (value != null) put(name, value)
}

private fun JSONObject.optDoubleOrNull(name: String): Double? = try {
    if (!has(name) || isNull(name)) null else optDouble(name).takeIf { it.isFinite() }
} catch (e: JSONException) {
    null
}

private fun JSONObject.optStringOrNull(name: String): String? = try {
    if (!has(name) || isNull(name)) null else getString(name).takeIf { it.isNotBlank() }
} catch (e: JSONException) {
    null
}

private fun JSONObject.longOrNull(name: String): Long? = try {
    if (!has(name) || isNull(name)) null else when (val value = opt(name)) {
        is Number -> value.toLong()
        else -> null
    }
} catch (e: JSONException) {
    null
}

/**
 * Profile-scoped persistent [WeatherCache] for the M15.2 weather repository.
 *
 * Stores one schema-versioned JSON document per coordinate key in the
 * `weather_cache` prefs file. Malformed or unknown documents are ignored by the
 * codec (decode returns null) so a corrupt cache can never clear live data or
 * crash a load.
 */
class WeatherCacheStore(context: Context) : WeatherCache {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)

    override fun read(key: String): WeatherCacheEntry? {
        val raw = prefs.getString(key, null) ?: return null
        return decodeWeatherCacheEntry(key, raw)
    }

    override fun write(key: String, entry: WeatherCacheEntry) {
        prefs.edit { putString(key, encodeWeatherCacheEntry(key, entry)) }
    }

    companion object {
        const val PREFS_NAME = "weather_cache"
        const val SCHEMA_VERSION = 1
    }
}
