package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject

/**
 * M15.2 weather location bound to one workplace.
 *
 * Persisted separately from the existing [WorkAssignmentsStore] workplace
 * serialization: coordinates are authoritative and the label/address are display
 * metadata. The store is keyed by workplace id so each workplace keeps its own
 * weather default and a Today temporary selection never rewrites it here.
 */
data class WorkplaceWeatherLocation(
    val workplaceId: String,
    val coordinates: WeatherCoordinates,
    val label: String,
    val address: String? = null,
)

/** Schema-versioned JSON encoding of a [WeatherCoordinates] value. */
internal fun encodeLocationCoordinates(coordinates: WeatherCoordinates): JSONObject =
    JSONObject()
        .put("latitude", coordinates.latitude)
        .put("longitude", coordinates.longitude)

/** Forgiving decode of a [WeatherCoordinates] value; null when malformed. */
internal fun decodeLocationCoordinates(obj: JSONObject?): WeatherCoordinates? {
    if (obj == null) return null
    return try {
        val latitude = obj.optDouble("latitude", Double.NaN)
        val longitude = obj.optDouble("longitude", Double.NaN)
        WeatherCoordinates.of(latitude, longitude)
    } catch (e: JSONException) {
        null
    }
}

/** Normalizes an optional address: trimmed, null when blank. */
internal fun normalizeOptionalAddress(address: String?): String? =
    address?.trim()?.takeIf { it.isNotEmpty() }

internal fun JSONObject.optLocationString(name: String): String? = try {
    if (!has(name) || isNull(name)) null else getString(name).takeIf { it.isNotBlank() }
} catch (e: JSONException) {
    null
}

/**
 * Deterministic schema-versioned JSON encoding of the workplace-keyed weather
 * location map. Entries are sorted by workplace id so the encoding is stable.
 */
fun encodeWorkplaceWeatherLocations(locations: Collection<WorkplaceWeatherLocation>): String {
    val root = JSONObject()
    root.put("schemaVersion", WorkplaceWeatherLocationStore.SCHEMA_VERSION)
    val entries = JSONObject()
    locations
        .sortedBy { it.workplaceId }
        .forEach { location ->
            val entry = JSONObject()
                .put("workplaceId", location.workplaceId)
                .put("coordinates", encodeLocationCoordinates(location.coordinates))
                .put("label", location.label)
            normalizeOptionalAddress(location.address)?.let { entry.put("address", it) }
            entries.put(location.workplaceId, entry)
        }
    root.put("locations", entries)
    return root.toString()
}
/**
 * Forgiving decode of the workplace-keyed weather location map.
 *
 * Unknown schema versions, malformed JSON, invalid coordinates, blank labels
 * and id/key mismatches drop only the affected entry (or everything) and never
 * throw — a corrupt location document can never clear a Workplace default that
 * is otherwise valid.
 */
fun decodeWorkplaceWeatherLocations(raw: String): Map<String, WorkplaceWeatherLocation> {
    if (raw.isBlank()) return emptyMap()
    return try {
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion", 0) != WorkplaceWeatherLocationStore.SCHEMA_VERSION) return emptyMap()
        val entries = root.optJSONObject("locations") ?: return emptyMap()
        buildMap {
            val keys = entries.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = entries.optJSONObject(key) ?: continue
                val workplaceId = entry.optString("workplaceId").trim()
                if (workplaceId.isBlank() || workplaceId != key) continue
                val coordinates = decodeLocationCoordinates(entry.optJSONObject("coordinates")) ?: continue
                val label = entry.optString("label").trim()
                if (label.isBlank()) continue
                put(
                    workplaceId,
                    WorkplaceWeatherLocation(
                        workplaceId = workplaceId,
                        coordinates = coordinates,
                        label = label,
                        address = normalizeOptionalAddress(entry.optLocationString("address")),
                    ),
                )
            }
        }
    } catch (e: JSONException) {
        emptyMap()
    }
}

/**
 * Normalizes a workplace location for persistence; returns null (and therefore
 * the store ignores it) for a blank label or invalid coordinates.
 */
fun normalizeWorkplaceWeatherLocation(location: WorkplaceWeatherLocation): WorkplaceWeatherLocation? {
    val label = location.label.trim()
    if (label.isBlank()) return null
    if (!WeatherCoordinates.isValid(location.coordinates.latitude, location.coordinates.longitude)) return null
    return location.copy(label = label, address = normalizeOptionalAddress(location.address))
}

/**
 * Profile-scoped persistent store for workplace weather locations.
 *
 * Thin SharedPreferences wrapper over the deterministic pure codec in the
 * `workplace_weather_locations` prefs file; the codec is JVM-tested while this
 * Android shell keeps the existing prefs pattern.
 */
class WorkplaceWeatherLocationStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _locationsFlow = MutableStateFlow(load())

    val locationsFlow: Flow<Map<String, WorkplaceWeatherLocation>> = _locationsFlow.asStateFlow()

    /** Latest decoded locations without subscribing to the flow. */
    fun snapshot(): Map<String, WorkplaceWeatherLocation> = _locationsFlow.value

    /** Returns the location stored for [workplaceId], or null. */
    fun locationFor(workplaceId: String): WorkplaceWeatherLocation? = _locationsFlow.value[workplaceId]

    /** Upserts one workplace weather location keyed by [location.workplaceId]. */
    fun saveLocation(location: WorkplaceWeatherLocation) {
        val normalized = normalizeWorkplaceWeatherLocation(location) ?: return
        persist(_locationsFlow.value + (normalized.workplaceId to normalized))
    }

    /** Removes the entry for [workplaceId]; returns true when one was removed. */
    fun deleteLocation(workplaceId: String): Boolean {
        val current = _locationsFlow.value
        if (workplaceId !in current) return false
        persist(current - workplaceId)
        return true
    }

    private fun persist(updated: Map<String, WorkplaceWeatherLocation>) {
        prefs.edit { putString(KEY_JSON, encodeWorkplaceWeatherLocations(updated.values)) }
        _locationsFlow.value = updated
    }

    private fun load(): Map<String, WorkplaceWeatherLocation> {
        val raw = prefs.getString(KEY_JSON, null) ?: ""
        return decodeWorkplaceWeatherLocations(raw)
    }

    companion object {
        const val PREFS_NAME = "workplace_weather_locations"
        const val KEY_JSON = "locations_json"
        const val SCHEMA_VERSION = 1
    }
}
