package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * M15.2 saved weather location with a stable id and deterministic order.
 *
 * Saved locations are profile-scoped. [id] is stable across edit/reorder and
 * [order] drives the deterministic list order (tie-broken by id) so a
 * reorder/delete never changes another entry's identity.
 */
data class SavedWeatherLocation(
    val id: String,
    val name: String,
    val coordinates: WeatherCoordinates,
    val address: String? = null,
    val order: Int,
)

/** Deterministic sort by (order, id); stable ids make ties unambiguous. */
fun sortedSavedWeatherLocations(locations: List<SavedWeatherLocation>): List<SavedWeatherLocation> =
    locations.sortedWith(compareBy({ it.order }, { it.id }))

/**
 * Stable deterministic deletion: removes only [id]; remaining entries keep
 * their ids and their relative order values.
 */
fun deleteSavedWeatherLocation(locations: List<SavedWeatherLocation>, id: String): List<SavedWeatherLocation> =
    locations.filterNot { it.id == id }

/**
 * Deterministic stable reorder: listed known ids move to the front in the
 * requested order (duplicates and unknown ids are ignored); unlisted ids keep
 * their previous relative order after them; orders are then reassigned 0..n-1.
 */
fun reorderSavedWeatherLocations(
    locations: List<SavedWeatherLocation>,
    orderedIds: List<String>,
): List<SavedWeatherLocation> {
    val byId = locations.associateBy { it.id }
    val seen = LinkedHashSet<String>()
    val reordered = mutableListOf<SavedWeatherLocation>()
    for (id in orderedIds) {
        val location = byId[id] ?: continue
        if (seen.add(id)) reordered += location
    }
    reordered += locations.filter { it.id !in seen }
    return reordered.mapIndexed { index, location -> location.copy(order = index) }
}

/** Deterministic next stable id: largest numeric `saved-N` suffix + 1, else `saved-1`. */
fun nextSavedLocationId(existing: Collection<SavedWeatherLocation>): String {
    val maxSuffix = existing.asSequence()
        .mapNotNull { it.id.removePrefix(SAVED_ID_PREFIX).toIntOrNull() }
        .maxOrNull() ?: 0
    return SAVED_ID_PREFIX + (maxSuffix + 1)
}

private const val SAVED_ID_PREFIX = "saved-"
/**
 * Deterministic schema-versioned JSON encoding of the saved location list,
 * always persisted in (order, id) sorted form.
 */
fun encodeSavedWeatherLocations(locations: List<SavedWeatherLocation>): String {
    val root = JSONObject()
    root.put("schemaVersion", SavedWeatherLocationStore.SCHEMA_VERSION)
    val entries = JSONArray()
    sortedSavedWeatherLocations(locations).forEach { location ->
        val entry = JSONObject()
            .put("id", location.id)
            .put("name", location.name)
            .put("coordinates", encodeLocationCoordinates(location.coordinates))
            .put("order", location.order)
        normalizeOptionalAddress(location.address)?.let { entry.put("address", it) }
        entries.put(entry)
    }
    root.put("locations", entries)
    return root.toString()
}

/**
 * Forgiving decode of the saved location list.
 *
 * Unknown schema versions, malformed JSON, invalid coordinates, blank ids and
 * blank names drop only the affected entry (or everything) and never throw —
 * a corrupt saved-location entry is ignored instead of destabilizing the list.
 */
fun decodeSavedWeatherLocations(raw: String): List<SavedWeatherLocation> {
    if (raw.isBlank()) return emptyList()
    return try {
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion", 0) != SavedWeatherLocationStore.SCHEMA_VERSION) return emptyList()
        val entries = root.optJSONArray("locations") ?: return emptyList()
        val decoded = buildList {
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                val id = entry.optString("id").trim()
                val name = entry.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue
                val coordinates = decodeLocationCoordinates(entry.optJSONObject("coordinates")) ?: continue
                add(
                    SavedWeatherLocation(
                        id = id,
                        name = name,
                        coordinates = coordinates,
                        address = normalizeOptionalAddress(entry.optLocationString("address")),
                        order = entry.optInt("order", 0),
                    ),
                )
            }
        }
        sortedSavedWeatherLocations(decoded)
    } catch (e: JSONException) {
        emptyList()
    }
}

/**
 * Normalizes a saved location for persistence; returns null (and therefore the
 * store ignores it) for a blank id/name or invalid coordinates.
 */
fun normalizeSavedWeatherLocation(location: SavedWeatherLocation): SavedWeatherLocation? {
    val id = location.id.trim()
    val name = location.name.trim()
    if (id.isBlank() || name.isBlank()) return null
    if (!WeatherCoordinates.isValid(location.coordinates.latitude, location.coordinates.longitude)) return null
    return location.copy(id = id, name = name, address = normalizeOptionalAddress(location.address))
}

/**
 * Profile-scoped persistent store for saved weather locations.
 *
 * Thin SharedPreferences wrapper over the deterministic pure codec in the
 * `saved_weather_locations` prefs file; the codec and reorder/delete helpers
 * are JVM-tested while this Android shell keeps the existing prefs pattern.
 */
class SavedWeatherLocationStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _locationsFlow = MutableStateFlow(load())

    val savedLocationsFlow: Flow<List<SavedWeatherLocation>> = _locationsFlow.asStateFlow()

    /** Latest decoded locations (sorted by order, then id) without subscribing. */
    fun snapshot(): List<SavedWeatherLocation> = _locationsFlow.value

    /** Returns the saved location with the stable [id], or null. */
    fun locationById(id: String): SavedWeatherLocation? = _locationsFlow.value.firstOrNull { it.id == id }

    /** Upserts by stable id; the list stays sorted by (order, id). */
    fun saveLocation(location: SavedWeatherLocation) {
        val normalized = normalizeSavedWeatherLocation(location) ?: return
        persist(sortedSavedWeatherLocations(_locationsFlow.value.filterNot { it.id == normalized.id } + normalized))
    }

    /** Removes one saved location by its stable id; returns true when removed. */
    fun delete(id: String): Boolean {
        val updated = deleteSavedWeatherLocation(_locationsFlow.value, id)
        if (updated.size == _locationsFlow.value.size) return false
        persist(updated)
        return true
    }

    /** Deterministic stable reorder over existing ids. */
    fun reorder(orderedIds: List<String>) {
        persist(reorderSavedWeatherLocations(_locationsFlow.value, orderedIds))
    }

    /** Deterministic next stable id for a new saved location. */
    fun newId(): String = nextSavedLocationId(_locationsFlow.value)

    private fun persist(updated: List<SavedWeatherLocation>) {
        prefs.edit { putString(KEY_JSON, encodeSavedWeatherLocations(updated)) }
        _locationsFlow.value = updated
    }

    private fun load(): List<SavedWeatherLocation> {
        val raw = prefs.getString(KEY_JSON, null) ?: ""
        return decodeSavedWeatherLocations(raw)
    }

    companion object {
        const val PREFS_NAME = "saved_weather_locations"
        const val KEY_JSON = "locations_json"
        const val SCHEMA_VERSION = 1
    }
}
