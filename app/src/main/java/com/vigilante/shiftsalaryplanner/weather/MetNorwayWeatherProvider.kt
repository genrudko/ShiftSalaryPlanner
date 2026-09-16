package com.vigilante.shiftsalaryplanner.weather

import org.json.JSONException
import org.json.JSONObject
import kotlin.math.round

/** MET Norway Locationforecast 2.0 compact endpoint. */
const val MET_NORWAY_LOCATIONFORECAST_ENDPOINT = "https://api.met.no/weatherapi/locationforecast/2.0/compact"

/**
 * App-identifying User-Agent for MET Norway calls.
 *
 * MET Norway requires a non-generic application User-Agent; this contract is
 * wired to the shared [WEATHER_HTTP_USER_AGENT] so all weather requests identify
 * the app and never present as `okhttp`, `Dalvik`, `Java/` or similar.
 */
const val MET_NORWAY_USER_AGENT: String = WEATHER_HTTP_USER_AGENT

/** Rounds [value] to at most 4 decimal places (MET Norway coordinate contract). */
fun metNorwayCoordinate(value: Double): Double = round(value * 10_000.0) / 10_000.0

/**
 * Builds the MET Norway Locationforecast 2.0 `compact` request URL.
 *
 * Coordinates are rounded to <=4 decimals per the M15.2 Task 3 contract.
 */
fun metNorwayCompactUrl(coordinates: WeatherCoordinates): String =
    "$MET_NORWAY_LOCATIONFORECAST_ENDPOINT?" +
        "lat=${metNorwayCoordinate(coordinates.latitude)}&lon=${metNorwayCoordinate(coordinates.longitude)}"

/** Request headers for MET Norway: the app-identifying User-Agent plus JSON Accept. */
fun metNorwayRequestHeaders(): Map<String, String> = mapOf(
    "User-Agent" to MET_NORWAY_USER_AGENT,
    "Accept" to "application/json",
)

/**
 * MET Norway Locationforecast 2.0 compact adapter (fallback provider).
 *
 * Advertises only the layers/metrics the compact format can actually serve:
 * CURRENT + HOURLY, temperature/condition/humidity/pressure/wind/gust/direction
 * and precipitation. Visibility, UV index and precipitation probability are not
 * advertised (and remain null/optional in normalized payloads).
 */
class MetNorwayWeatherProvider(
    private val transport: HttpWeatherTransport = HttpWeatherTransport(),
) : WeatherProvider {

    override val id: WeatherProviderId = WeatherProviderId.MET_NORWAY

    override val capabilities: Set<WeatherCapability> = setOf(
        WeatherCapability.CURRENT,
        WeatherCapability.HOURLY,
        WeatherCapability.TEMPERATURE,
        WeatherCapability.CONDITION,
        WeatherCapability.HUMIDITY,
        WeatherCapability.PRESSURE,
        WeatherCapability.WIND_SPEED,
        WeatherCapability.WIND_GUST,
        WeatherCapability.WIND_DIRECTION,
        WeatherCapability.PRECIPITATION,
    )

    override suspend fun fetch(request: WeatherFetchRequest): WeatherProviderResult = try {
        val body = transport.get(
            url = metNorwayCompactUrl(request.coordinates),
            headers = metNorwayRequestHeaders(),
        )
        val weather = parseMetNorwayCompact(
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
 * Offline-deterministic parser for a fixed MET Norway compact JSON payload.
 *
 * Maps the available `instant.details` fields (temperature, relative humidity,
 * sea-level pressure, wind speed, wind gust, wind direction) plus the
 * `next_*_hours` symbol/precipitation fields. Unsupported metrics — visibility,
 * UV index, precipitation probability, apparent temperature — stay null/optional
 * and never invalidate otherwise usable data. The first timeseries entry becomes
 * the normalized current observation; the remaining entries become hourly points
 * (entries without an instant temperature are skipped).
 */
fun parseMetNorwayCompact(
    json: String,
    coordinates: WeatherCoordinates,
    fetchedAtMillis: Long,
): NormalizedWeather {
    val properties = JSONObject(json).getJSONObject("properties")
    val timeseries = properties.getJSONArray("timeseries")
    if (timeseries.length() == 0) {
        throw JSONException("MET Norway compact payload has an empty timeseries")
    }

    val current = parseMetNorwayCurrent(timeseries.getJSONObject(0))

    val hourly = buildList {
        for (index in 1 until timeseries.length()) {
            parseMetNorwayHourlyPoint(timeseries.getJSONObject(index))?.let { add(it) }
        }
    }

    return NormalizedWeather(
        coordinates = coordinates,
        fetchedAtMillis = fetchedAtMillis,
        current = current,
        hourly = hourly,
        daily = emptyList(),
    )
}

private fun parseMetNorwayCurrent(point: JSONObject): WeatherCurrent {
    val timeMillis = weatherIsoTimeToEpochMillis(point.getString("time"))
    val details = instantDetails(point) ?: throw JSONException("MET Norway instant.details missing")
    val temperature = details.optDoubleOrNull("air_temperature")
        ?: throw JSONException("MET Norway air_temperature missing or invalid")
    return WeatherCurrent(
        observedAtMillis = timeMillis,
        temperatureCelsius = temperature,
        conditionCode = symbolCode(point),
        relativeHumidityPercent = details.optDoubleOrNull("relative_humidity"),
        pressureHpa = details.optDoubleOrNull("air_pressure_at_sea_level"),
        windSpeedMs = details.optDoubleOrNull("wind_speed"),
        windGustMs = details.optDoubleOrNull("wind_speed_of_gust"),
        windDirectionDegrees = details.optDoubleOrNull("wind_from_direction"),
        precipitationMm = precipitationAmount(point),
    )
}

private fun parseMetNorwayHourlyPoint(point: JSONObject): WeatherHourlyPoint? {
    val timeMillis = runCatching { weatherIsoTimeToEpochMillis(point.getString("time")) }.getOrNull()
        ?: return null
    val details = instantDetails(point) ?: return null
    val temperature = details.optDoubleOrNull("air_temperature") ?: return null
    return WeatherHourlyPoint(
        forecastTimeMillis = timeMillis,
        temperatureCelsius = temperature,
        conditionCode = symbolCode(point),
        relativeHumidityPercent = details.optDoubleOrNull("relative_humidity"),
        pressureHpa = details.optDoubleOrNull("air_pressure_at_sea_level"),
        windSpeedMs = details.optDoubleOrNull("wind_speed"),
        windGustMs = details.optDoubleOrNull("wind_speed_of_gust"),
        windDirectionDegrees = details.optDoubleOrNull("wind_from_direction"),
        precipitationMm = precipitationAmount(point),
    )
}

private fun instantDetails(point: JSONObject): JSONObject? =
    point.optJSONObject("data")?.optJSONObject("instant")?.optJSONObject("details")

/** First present symbol code across next_1/6/12 hours summaries. */
private fun symbolCode(point: JSONObject): String? {
    val data = point.optJSONObject("data") ?: return null
    return listOf("next_1_hours", "next_6_hours", "next_12_hours")
        .asSequence()
        .mapNotNull { data.optJSONObject(it)?.optJSONObject("summary")?.optStringOrNull("symbol_code") }
        .firstOrNull()
}

/** First present precipitation amount across next_1/6/12 hours details. */
private fun precipitationAmount(point: JSONObject): Double? {
    val data = point.optJSONObject("data") ?: return null
    return listOf("next_1_hours", "next_6_hours", "next_12_hours")
        .asSequence()
        .mapNotNull { data.optJSONObject(it)?.optJSONObject("details")?.optDoubleOrNull("precipitation_amount") }
        .firstOrNull()
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
