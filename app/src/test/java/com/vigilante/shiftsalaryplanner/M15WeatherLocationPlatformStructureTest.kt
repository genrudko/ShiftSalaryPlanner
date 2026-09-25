package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.ui.weather.WeatherLocationPickerResult
import com.vigilante.shiftsalaryplanner.ui.weather.initialStateForWeatherLocationPicker
import com.vigilante.shiftsalaryplanner.ui.weather.onCurrentLocationError
import com.vigilante.shiftsalaryplanner.ui.weather.onCurrentLocationFinished
import com.vigilante.shiftsalaryplanner.ui.weather.onWeatherLocationResultPicked
import com.vigilante.shiftsalaryplanner.ui.weather.onWeatherLocationSearchError
import com.vigilante.shiftsalaryplanner.ui.weather.shouldMovePickerCamera
import com.vigilante.shiftsalaryplanner.weather.CurrentLocationResult
import com.vigilante.shiftsalaryplanner.weather.CurrentLocationSource
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherPlaceSearchResult
import com.vigilante.shiftsalaryplanner.weather.normalizeCurrentLocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline M15.2 Task 5 platform/structure tests.
 *
 * - pure current-location normalization: valid fixes become an
 *   [CurrentLocationResult.Available] with authoritative [WeatherCoordinates]
 *   while non-finite/out-of-range fixes become an explicit failure;
 * - structural checks that [CurrentLocationSource] (used by
 *   `AndroidCurrentLocationSource`) is built on `LocationManager` with
 *   permission checks, last-known + single-shot behavior, an explicit
 *   unavailable/error vocabulary, and never references a background-location
 *   permission;
 * - the manifest declares exactly `ACCESS_COARSE_LOCATION` and
 *   `ACCESS_FINE_LOCATION` and no background location permission;
 * - MapLibre Native Android is pinned to exactly `13.6.1` and the picker loads
 *   the OpenFreeMap Liberty style with attribution visible;
 * - the picker confirms an authoritative center/pin plus editable display
 *   label/address metadata, and network/current-location errors never mutate
 *   the existing selected coordinates;
 * - Task 5 does not start Task 6 (weather settings / saved-location
 *   management UI).
 */
class M15WeatherLocationPlatformStructureTest {

    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun countOccurrences(haystack: String, needle: String): Int =
        haystack.windowed(needle.length).count { it == needle }

    // --- Pure current-location normalization ---

    @Test
    fun `current location normalization returns available with normalized coordinates`() {
        val result = normalizeCurrentLocationResult(
            latitude = 55.7558,
            longitude = 37.6173,
            accuracyMetres = 12.5f,
            provider = "gps",
            observedAtMillis = 123L,
        )
        assertTrue("valid fix must be Available", result is CurrentLocationResult.Available)
        val available = result as CurrentLocationResult.Available
        assertEquals(55.7558, available.coordinates.latitude, 1e-9)
        assertEquals(37.6173, available.coordinates.longitude, 1e-9)
        assertEquals(12.5f, available.accuracyMetres!!, 1e-6f)
        assertEquals("gps", available.provider)
        assertEquals(123L, available.observedAtMillis!!.toLong())
    }

    @Test
    fun `current location normalization rejects invalid and non finite coordinates`() {
        val invalid = listOf(
            Pair(91.0, 0.0),
            Pair(-91.0, 0.0),
            Pair(0.0, 181.0),
            Pair(0.0, -181.0),
            Pair(Double.NaN, 0.0),
            Pair(0.0, Double.POSITIVE_INFINITY),
        )
        invalid.forEach { (latitude, longitude) ->
            val result = normalizeCurrentLocationResult(latitude, longitude)
            assertTrue("[$latitude, $longitude] must fail explicitly", result is CurrentLocationResult.Failed)
        }
    }

    // --- AndroidCurrentLocationSource structure ---

    @Test
    fun `android current location source uses location manager with permissions and single shot`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/AndroidCurrentLocationSource.kt")
        assertTrue("AndroidCurrentLocationSource.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("must be built on Android LocationManager", text.contains("LocationManager"))
        assertTrue("must check fine-location permission", text.contains("ACCESS_FINE_LOCATION"))
        assertTrue("must check coarse-location permission", text.contains("ACCESS_COARSE_LOCATION"))
        assertTrue("must read a last-known location first", text.contains("getLastKnownLocation"))
        assertTrue("must request a single current fix", text.contains("requestSingleUpdate"))
        assertTrue("must implement CurrentLocationSource", text.contains("CurrentLocationSource"))
        assertTrue("must return normalized CurrentLocationResult", text.contains("CurrentLocationResult"))
        assertFalse(
            "absolutely no background-location permission in the source",
            text.contains("ACCESS_BACKGROUND_LOCATION"),
        )
    }

    @Test
    fun `current location source exposes an explicit unavailable and error vocabulary`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/AndroidCurrentLocationSource.kt")
        val text = file.readText()
        for (marker in listOf("PermissionDenied", "ProviderDisabled", "NoLocationFix", "Failed")) {
            assertTrue("result vocabulary must include $marker", text.contains(marker))
        }
    }
    // --- Manifest and dependency pins ---

    @Test
    fun `manifest declares only coarse and fine location permissions`() {
        val manifest = File(root(), "app/src/main/AndroidManifest.xml").readText()
        assertEquals(
            "ACCESS_COARSE_LOCATION must be declared exactly once",
            1,
            countOccurrences(manifest, "android.permission.ACCESS_COARSE_LOCATION"),
        )
        assertEquals(
            "ACCESS_FINE_LOCATION must be declared exactly once",
            1,
            countOccurrences(manifest, "android.permission.ACCESS_FINE_LOCATION"),
        )
        assertFalse("no background-location permission allowed", manifest.contains("ACCESS_BACKGROUND_LOCATION"))
    }

    @Test
    fun `maplibre native android is pinned to exactly 13 6 1 in the version catalog`() {
        val libs = File(root(), "gradle/libs.versions.toml").readText()
        assertTrue("version catalog must pin maplibre = 13.6.1", libs.contains("maplibre = \"13.6.1\""))
        assertTrue(
            "version catalog must map the org.maplibre.gl android-sdk artifact",
            libs.contains("org.maplibre.gl") && libs.contains("\"android-sdk\""),
        )
        val build = File(root(), "app/build.gradle.kts").readText()
        assertTrue("app must depend on the pinned MapLibre SDK", build.contains("maplibre.android.sdk"))
    }

    // --- Picker contract ---

    @Test
    fun `picker uses maplibre with openfreemap liberty style and visible attribution`() {
        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        assertTrue("picker screen must exist", picker.exists())
        val text = picker.readText()
        assertTrue("picker must use MapLibre MapView", text.contains("MapView"))
        assertTrue(
            "picker must load the OpenFreeMap Liberty style",
            text.contains("https://tiles.openfreemap.org/styles/liberty"),
        )
        assertTrue("picker must embed the native map into Compose via AndroidView", text.contains("AndroidView"))
        assertFalse("map attribution must remain visible (never disabled)", text.contains("setAttributionEnabled(false)"))
    }

    @Test
    fun `picker confirm result carries authoritative coordinates plus editable label and address metadata`() {
        val result = WeatherLocationPickerResult(
            coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!,
            label = "Москва",
            address = "Москва, Россия",
        )
        assertTrue(WeatherCoordinates.isValid(result.coordinates.latitude, result.coordinates.longitude))
        assertEquals("Москва", result.label)
        assertEquals("Москва, Россия", result.address)
    }

    @Test
    fun `network and current location errors never mutate existing selected coordinates`() {
        val seed = WeatherCoordinates.of(55.7558, 37.6173)!!
        val state = initialStateForWeatherLocationPicker(
            initialCoordinates = seed,
            initialLabel = "Москва",
            initialAddress = "Москва, Россия",
        )
        val afterSearchError = onWeatherLocationSearchError(state, "connection failed")
        assertEquals("search failure must keep coordinates", seed, afterSearchError.coordinates)
        assertEquals("search failure must keep label", "Москва", afterSearchError.label)
        assertEquals("search failure must keep address", "Москва, Россия", afterSearchError.address)
        assertTrue("search error must be surfaced", afterSearchError.searchError != null)

        val afterGpsError = onCurrentLocationError(afterSearchError, "no fix")
        assertEquals("gps failure must keep coordinates", seed, afterGpsError.coordinates)
        assertEquals("gps failure must keep label", "Москва", afterGpsError.label)
        assertEquals("gps failure must keep address", "Москва, Россия", afterGpsError.address)
    }

    @Test
    fun `search result and current gps seed the map and confirm uses the last selected center`() {
        var state = initialStateForWeatherLocationPicker(initialCoordinates = null)
        val kazan = WeatherPlaceSearchResult(
            name = "Казань",
            admin = "Татарстан",
            country = "Россия",
            coordinates = WeatherCoordinates.of(55.7963, 49.1088)!!,
        )
        state = onWeatherLocationResultPicked(state, kazan)
        assertEquals("picking a result must move the authoritative center", 55.7963, state.coordinates!!.latitude, 1e-9)
        assertEquals("picking a result must seed the display label", "Казань", state.label)
        assertTrue("picking a result must compose address metadata", state.address != null)

        val gps = onCurrentLocationFinished(
            state = state,
            coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!,
            providerLabel = "Current device",
        )
        assertEquals("gps seed must move the authoritative center", 55.7558, gps.coordinates!!.latitude, 1e-9)
        val confirm = WeatherLocationPickerResult(
            coordinates = gps.coordinates!!,
            label = gps.label,
            address = gps.address,
        )
        assertEquals("confirm result must use the last selected center", gps.coordinates, confirm.coordinates)
    }

    @Test
    fun `picker screen exposes search and current gps seeding with confirm callback`() {
        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        val text = picker.readText()
        assertTrue("must expose the picker composable", text.contains("fun WeatherLocationPickerScreen"))
        assertTrue("must accept a geocoding source for search", text.contains("geocodingSource"))
        assertTrue("must accept a current-location source for GPS seeding", text.contains("currentLocationSource"))
        assertTrue("must accept an onConfirm callback", text.contains("onConfirm"))
        assertTrue("must accept an onDismiss callback", text.contains("onDismiss"))
        assertTrue("must expose editable label metadata", text.contains("label"))
        assertTrue("must expose editable address metadata", text.contains("address"))
    }

    @Test
    fun `search result without address clears stale metadata`() {
        val state = initialStateForWeatherLocationPicker(
            initialCoordinates = WeatherCoordinates.of(55.7558, 37.6173)!!,
            initialLabel = "Old place",
            initialAddress = "Old address",
        )
        val result = WeatherPlaceSearchResult(
            name = "Unnamed district",
            admin = null,
            country = null,
            coordinates = WeatherCoordinates.of(56.0, 38.0)!!,
        )

        val next = onWeatherLocationResultPicked(state, result)

        assertEquals("Unnamed district", next.label)
        assertTrue("new result without address must clear unrelated old address", next.address == null)
    }

    @Test
    fun `current location replaces stale label and address metadata`() {
        val state = initialStateForWeatherLocationPicker(
            initialCoordinates = WeatherCoordinates.of(55.7963, 49.1088)!!,
            initialLabel = "Казань",
            initialAddress = "Татарстан, Россия",
        )

        val next = onCurrentLocationFinished(
            state = state,
            coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!,
            providerLabel = "Current device",
        )

        assertEquals("Current device", next.label)
        assertTrue("current-device seed must clear stale address metadata", next.address == null)
    }

    @Test
    fun `picker requests runtime location permission`() {
        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        val text = picker.readText()

        assertTrue("picker must use Activity Result permission launcher", text.contains("rememberLauncherForActivityResult"))
        assertTrue("picker must request multiple location permissions", text.contains("ActivityResultContracts.RequestMultiplePermissions"))
        assertTrue("picker must request coarse location", text.contains("Manifest.permission.ACCESS_COARSE_LOCATION"))
        assertTrue("picker must request fine location", text.contains("Manifest.permission.ACCESS_FINE_LOCATION"))
    }

    @Test
    fun `maplibre is initialized before map view construction`() {
        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        val text = picker.readText()
        val init = text.indexOf("MapLibre.getInstance(context)")
        val construct = text.indexOf("MapView(context)")

        assertTrue("MapLibre.getInstance(context) must be present", init >= 0)
        assertTrue("MapView(context) must be present", construct >= 0)
        assertTrue("MapLibre must be initialized before MapView construction", init < construct)
    }

    @Test
    fun `search result lazy keys are not based on place name alone`() {
        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        val text = picker.readText()

        assertFalse(
            "different places may have the same name; name alone is not a safe LazyColumn key",
            text.contains("key = { it.name }"),
        )
    }

    @Test
    fun `single shot registration does not abort when only one provider registration fails`() {
        val source = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/AndroidCurrentLocationSource.kt")
        val text = source.readText()
        val section = text.substringAfter("private suspend fun requestSingleShot")
            .substringBefore("private fun terminateSingleShot")

        assertTrue(
            "single-shot registration must track whether any provider registered successfully",
            section.contains("successfulRegistrations"),
        )
        assertFalse(
            "one provider registration failure must not terminate the listener while another provider can still succeed",
            section.contains(".onFailure {"),
        )
    }

    @Test
    fun `camera sync preserves zoom when camera already has selected center`() {
        val selected = WeatherCoordinates.of(55.7558, 37.6173)!!
        val moved = WeatherCoordinates.of(55.7568, 37.6173)!!

        assertFalse(
            "camera-idle feedback with the same center must not trigger a programmatic recenter",
            shouldMovePickerCamera(selected, selected),
        )
        assertTrue(
            "an externally selected search or GPS coordinate must still move the camera",
            shouldMovePickerCamera(selected, moved),
        )

        val picker = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt")
        val text = picker.readText()
        assertTrue(
            "map effect must guard programmatic moveCamera with the center-difference check",
            text.contains("if (shouldMovePickerCamera(currentCenter, center))"),
        )
    }

    // --- Task boundary guard ---

    @Test
    fun `task 5 does not start task 6 weather settings or location management UI`() {
        val root = root()
        val uiWeatherDir = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather")
        val workplaces = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt")
        val mainActivity = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt")

        assertFalse(
            "Task 6 weather settings screen must not exist yet",
            File(uiWeatherDir, "WeatherSettingsScreen.kt").exists(),
        )
        if (uiWeatherDir.exists()) {
            uiWeatherDir.listFiles()?.forEach { file ->
                val text = file.readText()
                assertFalse(
                    "no Task 6 weather-settings entry inside the picker area (${file.name})",
                    text.contains("WeatherSettingsScreen"),
                )
            }
        }
        if (workplaces.exists()) {
            assertFalse(
                "Task 6 must not wire weather-location setup into Workplaces yet",
                workplaces.readText().contains("WeatherLocationPickerScreen"),
            )
        }
        if (mainActivity.exists()) {
            assertFalse(
                "Task 6 must not wire weather settings into MainActivity yet",
                mainActivity.readText().contains("WeatherSettingsScreen"),
            )
        }
    }
}
