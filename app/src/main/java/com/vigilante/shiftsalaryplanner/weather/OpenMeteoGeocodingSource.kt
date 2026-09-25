package com.vigilante.shiftsalaryplanner.weather

import java.net.URLEncoder
import org.json.JSONException
import org.json.JSONObject

/** Open-Meteo Geocoding API search endpoint (M15.2 Task 5). */
const val OPEN_METEO_GEOCODING_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search"

/** Default bounded result count for the geocoding search. */
const val DEFAULT_OPEN_METEO_GEOCODING_LIMIT = 10

/**
 * A single normalized geocoding hit. The display [label] is the place [name]
 * and [admin]/[country] are optional display metadata; [coordinates] are
 * always authoritative (validated [WeatherCoordinates]).
 */
data class WeatherPlaceSearchResult(
    val name: String,
    val admin: String? = null,
    val country: String? = null,
    val coordinates: WeatherCoordinates,
)

/**
 * Builds the Open-Meteo geocoding request URL for [query].
 *
 * The URL is HTTPS-only (the shared `HttpWeatherTransport` enforces this
 * again), URL-encodes the trimmed query, bounds the result [limit] and forces
 * deterministic JSON output.
 */
fun openMeteoGeocodingUrl(query: String, limit: Int = DEFAULT_OPEN_METEO_GEOCODING_LIMIT): String {
    val count = limit.coerceIn(1, 100)
    val parameters = buildList {
        add("name=${URLEncoder.encode(query.trim(), Charsets.UTF_8.name())}")
        add("count=$count")
        add("language=en")
        add("format=json")
    }
    return "$OPEN_METEO_GEOCODING_ENDPOINT?${parameters.joinToString("&")}"
}

/**
 * Parses an Open-Meteo geocoding response into normalized
 * [WeatherPlaceSearchResult]s.
 *
 * Forgiving by contract: malformed entries, missing names, and non-finite or
 * out-of-range coordinates are skipped; a missing `results` key or a malformed
 * payload degrades to an empty list instead of throwing. `admin` prefers the
 * most specific admin-level field that is present.
 */
fun parseOpenMeteoGeocodingResults(raw: String): List<WeatherPlaceSearchResult> = try {
    val root = JSONObject(raw)
    val results = root.optJSONArray("results") ?: return emptyList()
    buildList {
        for (index in 0 until results.length()) {
            val entry = results.optJSONObject(index) ?: continue
            val name = entry.normalizedString("name")?.takeIf { it.isNotEmpty() } ?: continue
            val latitude = entry.normalizedDouble("latitude") ?: continue
            val longitude = entry.normalizedDouble("longitude") ?: continue
            val coordinates = WeatherCoordinates.of(latitude, longitude) ?: continue
            add(
                WeatherPlaceSearchResult(
                    name = name,
                    admin = entry.firstNormalizedString("admin1", "admin2", "admin3", "admin4"),
                    country = entry.normalizedString("country"),
                    coordinates = coordinates,
                )
            )
        }
    }
} catch (e: JSONException) {
    emptyList()
}

/**
 * Platform-independent geocoding search contract used by the picker. The
 * caller maps transport/parse errors (the transport throws
 * `HttpWeatherException` on failure) to a search-error state without touching
 * the already selected coordinates.
 */
interface OpenMeteoGeocodingSource {
    suspend fun search(
        query: String,
        limit: Int = DEFAULT_OPEN_METEO_GEOCODING_LIMIT,
    ): List<WeatherPlaceSearchResult>
}

/**
 * HTTP geocoding adapter over the shared bounded `HttpWeatherTransport`:
 * HTTPS only, bounded timeouts already enforced by the transport, and every
 * payload normalized through [parseOpenMeteoGeocodingResults].
 */
class HttpOpenMeteoGeocodingSource(
    private val transport: HttpWeatherTransport = HttpWeatherTransport(),
) : OpenMeteoGeocodingSource {

    override suspend fun search(
        query: String,
        limit: Int,
    ): List<WeatherPlaceSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return parseOpenMeteoGeocodingResults(transport.get(openMeteoGeocodingUrl(trimmed, limit)))
    }
}

private fun JSONObject.normalizedDouble(name: String): Double? = try {
    when (val value = opt(name)) {
        is Number -> value.toDouble().takeIf { it.isFinite() }
        else -> null
    }
} catch (e: JSONException) {
    null
}

private fun JSONObject.normalizedString(name: String): String? = try {
    if (!has(name) || isNull(name)) null else getString(name).trim()
} catch (e: JSONException) {
    null
}

private fun JSONObject.firstNormalizedString(vararg names: String): String? =
    names.asSequence()
        .mapNotNull { normalizedString(it) }
        .firstOrNull()
        ?.takeIf { it.isNotEmpty() }
