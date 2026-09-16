package com.vigilante.shiftsalaryplanner.weather

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** Open-Meteo Forecast API endpoint. */
const val OPEN_METEO_FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast"

/**
 * Canonical current variables explicitly requested from Open-Meteo.
 *
 * M15.2 Task 3 requires current temperature, apparent temperature, humidity,
 * precipitation, pressure, wind speed/direction/gusts, visibility and weather
 * code to be requested explicitly, paired with canonical unit parameters.
 */
const val OPEN_METEO_CURRENT_VARIABLES =
    "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,surface_pressure," +
        "wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,weather_code"

/** Canonical hourly variables explicitly requested from Open-Meteo. */
const val OPEN_METEO_HOURLY_VARIABLES =
    "temperature_2m,apparent_temperature,relative_humidity_2m," +
        "precipitation_probability,precipitation,surface_pressure," +
        "wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,weather_code"

/** Canonical daily variables explicitly requested from Open-Meteo. */
const val OPEN_METEO_DAILY_VARIABLES =
    "weather_code,temperature_2m_max,temperature_2m_min," +
        "relative_humidity_2m_max,relative_humidity_2m_min," +
        "precipitation_sum,precipitation_probability_max,surface_pressure_max," +
        "wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant," +
        "sunrise,sunset,uv_index_max"

/**
 * Builds the Open-Meteo Forecast request URL for [request].
 *
 * The URL explicitly pins canonical units (Celsius, metres/second, millimetres),
 * declares the full current/hourly/daily variable sets and fixes the timezone to
 * UTC so epoch conversion is unambiguous. Horizons are forwarded through
 * `forecast_hours` / `forecast_days` when requested.
 */
fun openMeteoForecastUrl(coordinates: WeatherCoordinates, request: WeatherFetchRequest): String {
    val parameters = buildList {
        add("latitude=${coordinates.latitude}")
        add("longitude=${coordinates.longitude}")
        add("current=$OPEN_METEO_CURRENT_VARIABLES")
        add("hourly=$OPEN_METEO_HOURLY_VARIABLES")
        add("daily=$OPEN_METEO_DAILY_VARIABLES")
        add("temperature_unit=celsius")
        add("wind_speed_unit=ms")
        add("precipitation_unit=mm")
        add("timezone=UTC")
        if (request.hourlyHours > 0) add("forecast_hours=${request.hourlyHours}")
        if (request.dailyDays > 0) add("forecast_days=${request.dailyDays}")
    }
    return "$OPEN_METEO_FORECAST_ENDPOINT?${parameters.joinToString("&")}"
}

/**
 * Open-Meteo Forecast adapter.
 *
 * Normalizes every response into [NormalizedWeather] before returning it; no
 * provider JSON or API types ever leave this adapter and [capabilities] advertise
 * every canonical layer/metric (Open-Meteo supports visibility, UV and
 * precipitation probability too).
 */
class OpenMeteoWeatherProvider(
    private val transport: HttpWeatherTransport = HttpWeatherTransport(),
) : WeatherProvider {

    override val id: WeatherProviderId = WeatherProviderId.OPEN_METEO

    override val capabilities: Set<WeatherCapability> = WeatherCapability.values().toSet()

    override suspend fun fetch(request: WeatherFetchRequest): WeatherProviderResult = try {
        val body = transport.get(
            url = openMeteoForecastUrl(request.coordinates, request),
            headers = mapOf("User-Agent" to WEATHER_HTTP_USER_AGENT),
        )
        val weather = parseOpenMeteoForecast(
            json = body,
            coordinates = request.coordinates,
            fetchedAtMillis = System.currentTimeMillis(),
        )
        WeatherProviderResult.Success(weather)
    } catch (e: HttpWeatherException) {
        when (e.kind) {
            HttpWeatherFailureKind.TRANSPORT -> WeatherProviderResult.Failure(WeatherFailureReason.TRANSPORT)
            HttpWeatherFailureKind.HTTP_STATUS -> WeatherProviderResult.Failure(WeatherFailureReason.SERVER)
        }
    } catch (e: Exception) {
        WeatherProviderResult.Failure(WeatherFailureReason.PARSE)
    }
}

/**
 * Offline-deterministic parser for a fixed Open-Meteo Forecast JSON payload.
 *
 * [fetchedAtMillis] is the acquisition timestamp supplied by the caller (the
 * provider uses the wall clock at fetch time; tests pass a fixed value). The
 * current layer is nullable in the normalized model, so a response without a
 * `current` block still parses; hourly/daily points that cannot be mapped are
 * skipped while a present but unusable `current` block fails the parse (which
 * the provider maps to [WeatherFailureReason.PARSE]).
 */
fun parseOpenMeteoForecast(
    json: String,
    coordinates: WeatherCoordinates,
    fetchedAtMillis: Long,
): NormalizedWeather {
    val root = JSONObject(json)

    val current = root.optJSONObject("current")?.let { currentJson ->
        val observedAtMillis = currentJson.optStringOrNull("time")
            ?.let { weatherIsoTimeToEpochMillis(it) }
            ?: throw JSONException("Open-Meteo current.time missing or invalid")
        val temperatureCelsius = currentJson.optDoubleOrNull("temperature_2m")
            ?: throw JSONException("Open-Meteo current.temperature_2m missing or invalid")
        WeatherCurrent(
            observedAtMillis = observedAtMillis,
            temperatureCelsius = temperatureCelsius,
            feelsLikeCelsius = currentJson.optDoubleOrNull("apparent_temperature"),
            conditionCode = currentJson.optIntOrNull("weather_code")?.let { openMeteoWeatherCodeToCondition(it) },
            relativeHumidityPercent = currentJson.optDoubleOrNull("relative_humidity_2m"),
            pressureHpa = currentJson.optDoubleOrNull("surface_pressure"),
            windSpeedMs = currentJson.optDoubleOrNull("wind_speed_10m"),
            windGustMs = currentJson.optDoubleOrNull("wind_gusts_10m"),
            windDirectionDegrees = currentJson.optDoubleOrNull("wind_direction_10m"),
            precipitationMm = currentJson.optDoubleOrNull("precipitation"),
            visibilityMetres = currentJson.optDoubleOrNull("visibility"),
            uvIndex = currentJson.optDoubleOrNull("uv_index"),
        )
    }

    val hourly = buildList {
        root.optJSONObject("hourly")?.let { hourlyJson ->
            val times = hourlyJson.optJSONArray("time")
            if (times != null) {
                for (index in 0 until times.length()) {
                    val timeMillis = times.stringOrNull(index)
                        ?.let { runCatching { weatherIsoTimeToEpochMillis(it) }.getOrNull() }
                        ?: continue
                    val temperature = hourlyJson.optJSONArray("temperature_2m")?.numberOrNull(index)
                        ?: continue
                    add(
                        WeatherHourlyPoint(
                            forecastTimeMillis = timeMillis,
                            temperatureCelsius = temperature,
                            feelsLikeCelsius = hourlyJson.optJSONArray("apparent_temperature")?.numberOrNull(index),
                            conditionCode = hourlyJson.optJSONArray("weather_code")
                                ?.numberOrNull(index)?.toInt()
                                ?.let { openMeteoWeatherCodeToCondition(it) },
                            relativeHumidityPercent = hourlyJson.optJSONArray("relative_humidity_2m")?.numberOrNull(index),
                            pressureHpa = hourlyJson.optJSONArray("surface_pressure")?.numberOrNull(index),
                            windSpeedMs = hourlyJson.optJSONArray("wind_speed_10m")?.numberOrNull(index),
                            windGustMs = hourlyJson.optJSONArray("wind_gusts_10m")?.numberOrNull(index),
                            windDirectionDegrees = hourlyJson.optJSONArray("wind_direction_10m")?.numberOrNull(index),
                            precipitationProbabilityPercent =
                                hourlyJson.optJSONArray("precipitation_probability")?.numberOrNull(index),
                            precipitationMm = hourlyJson.optJSONArray("precipitation")?.numberOrNull(index),
                            visibilityMetres = hourlyJson.optJSONArray("visibility")?.numberOrNull(index),
                        )
                    )
                }
            }
        }
    }

    val daily = buildList {
        root.optJSONObject("daily")?.let { dailyJson ->
            val times = dailyJson.optJSONArray("time")
            if (times != null) {
                for (index in 0 until times.length()) {
                    val dayMillis = times.stringOrNull(index)
                        ?.let { runCatching { weatherIsoTimeToEpochMillis(it) }.getOrNull() }
                        ?: continue
                    val minTemperature = dailyJson.optJSONArray("temperature_2m_min")?.numberOrNull(index)
                        ?: continue
                    val maxTemperature = dailyJson.optJSONArray("temperature_2m_max")?.numberOrNull(index)
                        ?: continue
                    val sunriseRaw = dailyJson.optJSONArray("sunrise")?.stringOrNull(index)
                    val sunsetRaw = dailyJson.optJSONArray("sunset")?.stringOrNull(index)
                    add(
                        WeatherDailyPoint(
                            forecastDayMillis = dayMillis,
                            minTemperatureCelsius = minTemperature,
                            maxTemperatureCelsius = maxTemperature,
                            conditionCode = dailyJson.optJSONArray("weather_code")
                                ?.numberOrNull(index)?.toInt()
                                ?.let { openMeteoWeatherCodeToCondition(it) },
                            precipitationProbabilityPercent =
                                dailyJson.optJSONArray("precipitation_probability_max")?.numberOrNull(index),
                            precipitationMm = dailyJson.optJSONArray("precipitation_sum")?.numberOrNull(index),
                            pressureHpa = dailyJson.optJSONArray("surface_pressure_max")?.numberOrNull(index),
                            windSpeedMs = dailyJson.optJSONArray("wind_speed_10m_max")?.numberOrNull(index),
                            windGustMs = dailyJson.optJSONArray("wind_gusts_10m_max")?.numberOrNull(index),
                            windDirectionDegrees = dailyJson.optJSONArray("wind_direction_10m_dominant")?.numberOrNull(index),
                            sunriseMillis = sunriseRaw?.let { runCatching { weatherIsoTimeToEpochMillis(it) }.getOrNull() },
                            sunsetMillis = sunsetRaw?.let { runCatching { weatherIsoTimeToEpochMillis(it) }.getOrNull() },
                            uvIndexMax = dailyJson.optJSONArray("uv_index_max")?.numberOrNull(index),
                        )
                    )
                }
            }
        }
    }

    return NormalizedWeather(
        coordinates = coordinates,
        fetchedAtMillis = fetchedAtMillis,
        current = current,
        hourly = hourly,
        daily = daily,
    )
}

/** Maps an Open-Meteo WMO weather code to a stable canonical condition slug. */
fun openMeteoWeatherCodeToCondition(code: Int): String = when (code) {
    0 -> "clear"
    1 -> "mainly_clear"
    2 -> "partly_cloudy"
    3 -> "overcast"
    45, 48 -> "fog"
    51, 53, 55 -> "drizzle"
    56, 57 -> "freezing_drizzle"
    61, 63, 65 -> "rain"
    66, 67 -> "freezing_rain"
    71, 73, 75, 77 -> "snow"
    80, 81, 82 -> "rain_showers"
    85, 86 -> "snow_showers"
    95 -> "thunderstorm"
    96, 99 -> "thunderstorm_hail"
    else -> "unknown_$code"
}

/**
 * Parses provider ISO-8601 instants into epoch millis.
 *
 * Accepts offsets/`Z` suffixes ("2026-09-16T12:00:00Z"), zone-less date-times
 * (Open-Meteo "2026-09-16T13:00", interpreted as UTC because the request pins
 * `timezone=UTC`) and bare dates ("2026-09-16", interpreted as UTC midnight).
 */
internal fun weatherIsoTimeToEpochMillis(raw: String): Long {
    val trimmed = raw.trim()
    return try {
        OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
    } catch (offsetParse: DateTimeParseException) {
        try {
            LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (dateTimeParse: DateTimeParseException) {
            LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    }
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

private fun JSONObject.optIntOrNull(name: String): Int? = try {
    when (val value = opt(name)) {
        is Number -> value.toInt()
        else -> null
    }
} catch (e: JSONException) {
    null
}

private fun JSONArray.numberOrNull(index: Int): Double? {
    val value = opt(index) ?: return null
    return when (value) {
        is Number -> value.toDouble().takeIf { it.isFinite() }
        else -> null
    }
}

private fun JSONArray.stringOrNull(index: Int): String? {
    val value = opt(index) ?: return null
    return (value as? String)?.takeIf { it.isNotBlank() }
}
