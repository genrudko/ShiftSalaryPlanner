package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.decodeSavedWeatherLocations
import com.vigilante.shiftsalaryplanner.settings.decodeWorkplaceWeatherLocations
import com.vigilante.shiftsalaryplanner.settings.deleteSavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.encodeSavedWeatherLocations
import com.vigilante.shiftsalaryplanner.settings.encodeWorkplaceWeatherLocations
import com.vigilante.shiftsalaryplanner.settings.nextSavedLocationId
import com.vigilante.shiftsalaryplanner.settings.reorderSavedWeatherLocations
import com.vigilante.shiftsalaryplanner.settings.sortedSavedWeatherLocations
import com.vigilante.shiftsalaryplanner.weather.ResolvedWeatherLocation
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherLocationSelection
import com.vigilante.shiftsalaryplanner.weather.WeatherLocationSource
import com.vigilante.shiftsalaryplanner.weather.resolveWeatherLocation
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline M15.2 Task 4 tests for workplace/saved/temporary weather locations.
 *
 * The Android stores are thin profile-scoped SharedPreferences wrappers over
 * deterministic pure-Kotlin codecs; those codecs and the pure location resolver
 * are JVM-tested here without any Android runtime or network dependency:
 * - resolver priority: temporary override -> active Workplace saved
 *   coordinates -> current-device fallback only when explicitly allowed;
 * - temporary override never mutates the persisted Workplace default;
 * - workplace-keyed persistence round trip/update/delete with safe rejection
 *   of malformed coordinates;
 * - saved-location stable ids, deterministic round trip/reorder/delete and
 *   malformed-entry rejection;
 * - structural checks that the Android store classes and the required
 *   `SettingsDataPort` / `AppDependencies` wiring exist and that Task 5
 *   platform code is not started.
 */
class M15WeatherLocationTest {

    private val moscow = WeatherCoordinates.of(55.7558, 37.6173)!!
    private val saintPetersburg = WeatherCoordinates.of(59.95, 30.3167)!!
    private val dacha = WeatherCoordinates.of(55.0, 38.0)!!

    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }
    // --- Resolver ---

    @Test
    fun `temporary override wins over the active workplace default`() {
        val selection = WeatherLocationSelection(
            defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN),
            temporaryOverride = WeatherLocationSource.Workplace(WORKPLACE_SECOND),
        )
        val resolved = resolveWeatherLocation(
            selection = selection,
            workplaceLocations = mapOf(
                WORKPLACE_MAIN to WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1"),
                WORKPLACE_SECOND to WorkplaceWeatherLocation(WORKPLACE_SECOND, saintPetersburg, "Работа 2", "Склад"),
            ),
        )
        assertNotNull(resolved)
        assertEquals(WeatherLocationSource.Workplace(WORKPLACE_SECOND), resolved!!.source)
        assertEquals(saintPetersburg, resolved.coordinates)
        assertEquals("Работа 2", resolved.label)
        assertEquals("Склад", resolved.address)
    }

    @Test
    fun `temporary saved override wins over the active workplace default`() {
        val selection = WeatherLocationSelection(
            defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN),
            temporaryOverride = WeatherLocationSource.Saved(SAVED_HOME),
        )
        val resolved = resolveWeatherLocation(
            selection = selection,
            workplaceLocations = mapOf(WORKPLACE_MAIN to WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1")),
            savedLocations = mapOf(
                SAVED_HOME to SavedWeatherLocation(SAVED_HOME, "Home", saintPetersburg, address = "Москва", order = 0),
            ),
        )
        assertNotNull(resolved)
        assertEquals(WeatherLocationSource.Saved(SAVED_HOME), resolved!!.source)
        assertEquals(saintPetersburg, resolved.coordinates)
        assertEquals("Home", resolved.label)
        assertEquals("Москва", resolved.address)
    }

    @Test
    fun `active workplace default resolves from persisted workplace coordinates`() {
        val selection = WeatherLocationSelection(defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN))
        val resolved = resolveWeatherLocation(
            selection = selection,
            workplaceLocations = mapOf(WORKPLACE_MAIN to WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1")),
        )
        assertNotNull(resolved)
        assertEquals(WeatherLocationSource.Workplace(WORKPLACE_MAIN), resolved!!.source)
        assertEquals(moscow, resolved.coordinates)
        assertEquals("Работа 1", resolved.label)
        assertNull(resolved.address)
    }

    @Test
    fun `unresolvable temporary override falls back to the active workplace default`() {
        val selection = WeatherLocationSelection(
            defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN),
            temporaryOverride = WeatherLocationSource.Saved("saved-deleted"),
        )
        val resolved = resolveWeatherLocation(
            selection = selection,
            workplaceLocations = mapOf(WORKPLACE_MAIN to WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1")),
            savedLocations = emptyMap(),
        )
        assertNotNull(resolved)
        assertEquals(WeatherLocationSource.Workplace(WORKPLACE_MAIN), resolved!!.source)
        assertEquals(moscow, resolved.coordinates)
    }


    @Test
    fun `current device is only resolved when the caller explicitly allows it`() {
        val currentDevice = WeatherLocationSource.CurrentDevice
        val noWorkplaces = emptyMap<String, WorkplaceWeatherLocation>()
        // Not allowed, no coordinates => unresolved.
        assertNull(
            resolveWeatherLocation(
                WeatherLocationSelection(currentDevice),
                noWorkplaces,
                allowCurrentDeviceFallback = false,
            ),
        )
        // Not allowed, coordinates present => still unresolved.
        assertNull(
            resolveWeatherLocation(
                WeatherLocationSelection(currentDevice),
                noWorkplaces,
                currentDeviceCoordinates = moscow,
                allowCurrentDeviceFallback = false,
            ),
        )
        // Allowed, no coordinates => unresolved.
        assertNull(
            resolveWeatherLocation(
                WeatherLocationSelection(currentDevice),
                noWorkplaces,
                currentDeviceCoordinates = null,
                allowCurrentDeviceFallback = true,
            ),
        )
        // Allowed, coordinates present => resolved current device.
        val resolved = resolveWeatherLocation(
            WeatherLocationSelection(currentDevice),
            noWorkplaces,
            currentDeviceCoordinates = moscow,
            allowCurrentDeviceFallback = true,
        )
        assertNotNull(resolved)
        assertEquals(WeatherLocationSource.CurrentDevice, resolved!!.source)
        assertEquals(moscow, resolved.coordinates)
    }

    @Test
    fun `current device is a last resort fallback only when explicitly allowed`() {
        val default = WeatherLocationSource.Workplace(WORKPLACE_MAIN)
        // Active workplace has no saved coordinates: current device must never
        // substitute when the caller did not explicitly allow the fallback.
        assertNull(resolveWeatherLocation(WeatherLocationSelection(default), emptyMap()))
        val allowed = resolveWeatherLocation(
            WeatherLocationSelection(default),
            emptyMap(),
            currentDeviceCoordinates = moscow,
            allowCurrentDeviceFallback = true,
        )
        assertNotNull(allowed)
        assertEquals(WeatherLocationSource.CurrentDevice, allowed!!.source)
        assertEquals(moscow, allowed.coordinates)
    }

    @Test
    fun `temporary override never mutates the workplace default`() {
        val defaultLocation = WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1")
        val encodedDefault = encodeWorkplaceWeatherLocations(listOf(defaultLocation))
        val selection = WeatherLocationSelection(
            defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN),
            temporaryOverride = WeatherLocationSource.Workplace(WORKPLACE_SECOND),
        )
        resolveWeatherLocation(
            selection = selection,
            workplaceLocations = mapOf(
                WORKPLACE_MAIN to defaultLocation,
                WORKPLACE_SECOND to WorkplaceWeatherLocation(WORKPLACE_SECOND, saintPetersburg, "Работа 2"),
            ),
            currentDeviceCoordinates = saintPetersburg,
            allowCurrentDeviceFallback = true,
        )
        // The persisted workplace default is byte-for-byte unchanged after a
        // temporary Today selection.
        assertEquals(encodedDefault, encodeWorkplaceWeatherLocations(decodeWorkplaceWeatherLocations(encodedDefault).values))
        // The default source still resolves to the original workplace coordinates.
        val resolved = resolveWeatherLocation(
            WeatherLocationSelection(defaultSource = WeatherLocationSource.Workplace(WORKPLACE_MAIN)),
            workplaceLocations = decodeWorkplaceWeatherLocations(encodedDefault),
        )
        assertNotNull(resolved)
        assertEquals(moscow, resolved!!.coordinates)
    }

    // --- Workplace persistence codec ---

    @Test
    fun `workplace weather locations round trip keyed by workplace id`() {
        val locations = listOf(
            WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1"),
            WorkplaceWeatherLocation(WORKPLACE_SECOND, saintPetersburg, "Работа 2", "Склад"),
        )
        val decoded = decodeWorkplaceWeatherLocations(encodeWorkplaceWeatherLocations(locations))
        assertEquals(2, decoded.size)
        assertEquals(locations[0], decoded[WORKPLACE_MAIN])
        assertEquals(locations[1], decoded[WORKPLACE_SECOND])
    }

    @Test
    fun `workplace locations persist independently per workplace id`() {
        val main = WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1")
        val second = WorkplaceWeatherLocation(WORKPLACE_SECOND, saintPetersburg, "Работа 2")
        val firstDoc = encodeWorkplaceWeatherLocations(listOf(main, second))

        // Updating one workplace changes only that key; the other is untouched.
        val updatedSecond = second.copy(coordinates = saintPetersburg, label = "Работа 2 (спб)")
        val secondDoc = encodeWorkplaceWeatherLocations(listOf(main, updatedSecond))
        assertEquals(main, decodeWorkplaceWeatherLocations(firstDoc)[WORKPLACE_MAIN])
        assertEquals(main, decodeWorkplaceWeatherLocations(secondDoc)[WORKPLACE_MAIN])
        assertEquals(updatedSecond, decodeWorkplaceWeatherLocations(secondDoc)[WORKPLACE_SECOND])

        // Deleting one workplace leaves the other untouched.
        val afterDelete = decodeWorkplaceWeatherLocations(
            encodeWorkplaceWeatherLocations(
                decodeWorkplaceWeatherLocations(secondDoc).values.filterNot { it.workplaceId == WORKPLACE_SECOND }
            )
        )
        assertEquals(main, afterDelete[WORKPLACE_MAIN])
        assertNull(afterDelete[WORKPLACE_SECOND])
    }
    @Test
    fun `malformed workplace coordinates are rejected safely`() {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        val entries = JSONObject()
        entries.put(
            WORKPLACE_MAIN,
            JSONObject()
                .put("workplaceId", WORKPLACE_MAIN)
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("label", "Работа 1"),
        )
        entries.put(
            "work_bad_range",
            JSONObject()
                .put("workplaceId", "work_bad_range")
                .put("coordinates", JSONObject().put("latitude", 999.0).put("longitude", 37.6173))
                .put("label", "Bad range"),
        )
        entries.put(
            "work_bad_type",
            JSONObject()
                .put("workplaceId", "work_bad_type")
                .put("coordinates", JSONObject().put("latitude", "NaN").put("longitude", 37.6173))
                .put("label", "Bad type"),
        )
        entries.put(
            "work_bad_missing",
            JSONObject()
                .put("workplaceId", "work_bad_missing")
                .put("coordinates", JSONObject())
                .put("label", "Bad missing"),
        )
        root.put("locations", entries)
        val decoded = decodeWorkplaceWeatherLocations(root.toString())
        assertEquals(setOf(WORKPLACE_MAIN), decoded.keys)
        assertEquals(WorkplaceWeatherLocation(WORKPLACE_MAIN, moscow, "Работа 1"), decoded[WORKPLACE_MAIN])
    }

    @Test
    fun `malformed workplace documents decode to empty map`() {
        assertTrue(decodeWorkplaceWeatherLocations("").isEmpty())
        assertTrue(decodeWorkplaceWeatherLocations("not json{{").isEmpty())
        assertTrue(decodeWorkplaceWeatherLocations("""{"schemaVersion":2,"locations":{}}""").isEmpty())
        assertTrue(decodeWorkplaceWeatherLocations("""{"schemaVersion":1}""").isEmpty())
    }

    @Test
    fun `workplace codec rejects blank labels and id mismatches`() {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        val entries = JSONObject()
        entries.put(
            WORKPLACE_MAIN,
            JSONObject()
                .put("workplaceId", WORKPLACE_MAIN)
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("label", "   "),
        )
        entries.put(
            "work_mismatch",
            JSONObject()
                .put("workplaceId", "other_id")
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("label", "X"),
        )
        entries.put(
            "work_blank_id",
            JSONObject()
                .put("workplaceId", " ")
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("label", "Y"),
        )
        root.put("locations", entries)
        assertTrue(decodeWorkplaceWeatherLocations(root.toString()).isEmpty())
    }

    // --- Saved location persistence codec ---

    @Test
    fun `saved locations round trip with stable ids and order`() {
        val locations = listOf(
            SavedWeatherLocation("saved-1", "Home", moscow, order = 2),
            SavedWeatherLocation("saved-2", "Office", saintPetersburg, "Addr", order = 0),
            SavedWeatherLocation("saved-3", "Dacha", dacha, order = 1),
        )
        val decoded = decodeSavedWeatherLocations(encodeSavedWeatherLocations(locations))
        assertEquals(locations.size, decoded.size)
        // Deterministic sort by (order, id) with stable ids as tie-break.
        assertEquals(listOf("saved-2", "saved-3", "saved-1"), decoded.map { it.id })
        assertEquals(locations.sortedWith(compareBy({ it.order }, { it.id })), decoded)
        assertEquals(locations.sortedWith(compareBy({ it.order }, { it.id })), sortedSavedWeatherLocations(locations))
    }

    @Test
    fun `saved location ids stay stable across delete`() {
        val locations = listOf(
            SavedWeatherLocation("saved-1", "Home", moscow, order = 0),
            SavedWeatherLocation("saved-2", "Office", saintPetersburg, order = 1),
            SavedWeatherLocation("saved-3", "Dacha", dacha, order = 2),
        )
        val remaining = deleteSavedWeatherLocation(locations, "saved-2")
        assertEquals(listOf("saved-1", "saved-3"), remaining.map { it.id })
        assertEquals(listOf(0, 2), remaining.map { it.order })
        assertEquals("saved-1", remaining[0].id)
        assertEquals("saved-3", remaining[1].id)
    }

    @Test
    fun `saved location reorder is deterministic and preserves ids`() {
        val locations = listOf(
            SavedWeatherLocation("saved-1", "Home", moscow, order = 0),
            SavedWeatherLocation("saved-2", "Office", saintPetersburg, order = 1),
            SavedWeatherLocation("saved-3", "Dacha", dacha, order = 2),
        )
        val reordered = reorderSavedWeatherLocations(locations, listOf("saved-3", "saved-1"))
        assertEquals(listOf("saved-3", "saved-1", "saved-2"), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.order })
        // Deterministic: identical inputs produce identical outputs.
        assertEquals(reordered, reorderSavedWeatherLocations(locations, listOf("saved-3", "saved-1")))
    }

    @Test
    fun `saved location reorder is stable for duplicate and unknown ids`() {
        val locations = listOf(
            SavedWeatherLocation("saved-1", "Home", moscow, order = 0),
            SavedWeatherLocation("saved-2", "Office", saintPetersburg, order = 1),
        )
        // Duplicate ids are deduped, unknown ids are ignored, and unlisted ids
        // keep their previous relative order after the listed ones.
        val reordered = reorderSavedWeatherLocations(locations, listOf("saved-missing", "saved-1", "saved-2", "saved-2"))
        assertEquals(listOf("saved-1", "saved-2"), reordered.map { it.id })
        assertEquals(listOf(0, 1), reordered.map { it.order })
    }

    @Test
    fun `malformed saved location entries are skipped safely`() {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        val entries = JSONArray()
        entries.put(
            JSONObject()
                .put("id", "saved-1")
                .put("name", "Home")
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("order", 0),
        )
        entries.put(
            JSONObject()
                .put("id", "saved-bad")
                .put("name", "Bad")
                .put("coordinates", JSONObject().put("latitude", 91.0).put("longitude", 37.6173))
                .put("order", 1),
        )
        entries.put(
            JSONObject()
                .put("id", "saved-empty")
                .put("name", " ")
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("order", 2),
        )
        entries.put(
            JSONObject()
                .put("id", "   ")
                .put("name", "BlankId")
                .put("coordinates", JSONObject().put("latitude", 55.7558).put("longitude", 37.6173))
                .put("order", 3),
        )
        root.put("locations", entries)
        val decoded = decodeSavedWeatherLocations(root.toString())
        assertEquals(listOf("saved-1"), decoded.map { it.id })
    }

    @Test
    fun `malformed saved location documents decode safely`() {
        assertTrue(decodeSavedWeatherLocations("").isEmpty())
        assertTrue(decodeSavedWeatherLocations("garbage{{").isEmpty())
        assertTrue(decodeSavedWeatherLocations("""{"schemaVersion":9,"locations":[]}""").isEmpty())
        assertTrue(decodeSavedWeatherLocations("""{"schemaVersion":1}""").isEmpty())
    }

    @Test
    fun `saved location id generation is deterministic and monotonic`() {
        assertEquals("saved-1", nextSavedLocationId(emptyList()))
        val existing = listOf(
            SavedWeatherLocation("saved-1", "Home", moscow, order = 0),
            SavedWeatherLocation("saved-3", "Dacha", dacha, order = 1),
            SavedWeatherLocation("custom-7", "Custom", moscow, order = 2),
        )
        assertEquals("saved-4", nextSavedLocationId(existing))
    }

    // --- Structure / dependency wiring (offline) ---

    @Test
    fun `weather location models and resolver live in the weather package`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherLocation.kt")
        assertTrue("WeatherLocation.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("WeatherLocationSource must be sealed", text.contains("sealed class WeatherLocationSource"))
        assertTrue("Workplace source must exist", text.contains("data class Workplace"))
        assertTrue("CurrentDevice source must exist", text.contains("object CurrentDevice"))
        assertTrue("Saved source must exist", text.contains("data class Saved"))
        assertTrue("WeatherLocationSelection must exist", text.contains("data class WeatherLocationSelection"))
        assertTrue("temporaryOverride must be optional", text.contains("temporaryOverride"))
        assertTrue("resolver must exist", text.contains("fun resolveWeatherLocation"))
        assertFalse("Task 4 resolver must be pure: no Android imports", text.contains("import android."))
        assertFalse("no LocationManager in Task 4", text.contains("LocationManager"))
        assertFalse("no Geocoder in Task 4", text.contains("Geocoder"))
        assertFalse("no MapLibre in Task 4", text.contains("MapLibre"))
    }

    @Test
    fun `workplace weather location store is a profile scoped prefs wrapper`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WorkplaceWeatherLocationStore.kt")
        assertTrue("WorkplaceWeatherLocationStore.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("WorkplaceWeatherLocation model must exist", text.contains("data class WorkplaceWeatherLocation"))
        assertTrue("workplaceId must be the key", text.contains("val workplaceId: String"))
        assertTrue("coordinates must be authoritative", text.contains("val coordinates: WeatherCoordinates"))
        assertTrue("store class must exist", text.contains("class WorkplaceWeatherLocationStore"))
        assertTrue("store must persist via profile-scoped prefs", text.contains("profileSharedPreferences"))
        assertTrue("store must expose a locations flow", text.contains("locationsFlow"))
        assertTrue("store must expose saveLocation", text.contains("fun saveLocation"))
        assertTrue("store must expose deleteLocation", text.contains("fun deleteLocation"))
    }

    @Test
    fun `saved weather location store is a profile scoped prefs wrapper`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/SavedWeatherLocationStore.kt")
        assertTrue("SavedWeatherLocationStore.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("SavedWeatherLocation model must exist", text.contains("data class SavedWeatherLocation"))
        assertTrue("stable id must exist", text.contains("val id: String"))
        assertTrue("order must exist", text.contains("val order: Int"))
        assertTrue("store class must exist", text.contains("class SavedWeatherLocationStore"))
        assertTrue("store must persist via profile-scoped prefs", text.contains("profileSharedPreferences"))
        assertTrue("store must expose a savedLocationsFlow", text.contains("savedLocationsFlow"))
        assertTrue("store must expose saveLocation", text.contains("fun saveLocation"))
        assertTrue("store must expose delete", text.contains("fun delete"))
        assertTrue("store must expose reorder", text.contains("fun reorder"))
    }

    @Test
    fun `settings data port exposes locations and the app wires the location stores`() {
        val port = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt").readText()
        assertTrue(
            "port must expose workplaceWeatherLocations",
            port.contains("val workplaceWeatherLocations: Flow<Map<String, WorkplaceWeatherLocation>>"),
        )
        assertTrue(
            "port must expose savedWeatherLocations",
            port.contains("val savedWeatherLocations: Flow<List<SavedWeatherLocation>>"),
        )
        assertTrue("port must expose saveWorkplaceWeatherLocation", port.contains("fun saveWorkplaceWeatherLocation"))
        assertTrue("port must expose deleteWorkplaceWeatherLocation", port.contains("fun deleteWorkplaceWeatherLocation"))
        assertTrue("port must expose saveSavedWeatherLocation", port.contains("fun saveSavedWeatherLocation"))
        assertTrue("port must expose deleteSavedWeatherLocation", port.contains("fun deleteSavedWeatherLocation"))
        assertTrue("port must expose reorderSavedWeatherLocations", port.contains("fun reorderSavedWeatherLocations"))

        val dependencies = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()
        assertTrue("AppDependencies must construct the workplace location store", dependencies.contains("WorkplaceWeatherLocationStore("))
        assertTrue("AppDependencies must construct the saved location store", dependencies.contains("SavedWeatherLocationStore("))
    }

    @Test
    fun `task 4 does not start task 5 location platform or weather UI`() {
        val root = root()
        val weatherDir = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/weather")
        val uiWeatherDir = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather")
        val manifest = File(root, "app/src/main/AndroidManifest.xml")
        val libs = File(root, "gradle/libs.versions.toml")

        assertFalse("Task 5 current-location source must not exist yet", File(weatherDir, "AndroidCurrentLocationSource.kt").exists())
        assertFalse("Task 5 geocoding source must not exist yet", File(weatherDir, "OpenMeteoGeocodingSource.kt").exists())
        assertFalse("Task 5 picker must not exist yet", File(uiWeatherDir, "WeatherLocationPickerScreen.kt").exists())
        assertFalse("no weather UI may exist before Task 6/7", uiWeatherDir.exists())
        if (manifest.exists()) {
            val manifestText = manifest.readText()
            assertFalse("no coarse location permission in Task 4", manifestText.contains("ACCESS_COARSE_LOCATION"))
            assertFalse("no fine location permission in Task 4", manifestText.contains("ACCESS_FINE_LOCATION"))
        }
        if (libs.exists()) {
            assertFalse("no MapLibre dependency in Task 4", libs.readText().contains("maplibre", ignoreCase = true))
        }
    }

    @Test
    fun `task 4 does not touch WorkAssignmentsStore workplace serialization`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WorkAssignmentsStore.kt")
        assertTrue("WorkAssignmentsStore.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("existing Workplace(id,name) JSON stays intact", text.contains("put(\"id\", workplace.id)"))
        assertTrue("existing Workplace(id,name) JSON stays intact", text.contains("put(\"name\", workplace.name)"))
        assertFalse("workplace weather serialization must stay separated", text.contains("WorkplaceWeatherLocation"))
    }

    private companion object {
        const val WORKPLACE_MAIN = "work_main"
        const val WORKPLACE_SECOND = "work_second"
        const val SAVED_HOME = "saved-home"
    }
}

