package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.DEFAULT_FOREGROUND_FRESHNESS_MILLIS
import com.vigilante.shiftsalaryplanner.settings.WeatherSettings
import com.vigilante.shiftsalaryplanner.settings.decodeWeatherCacheEntry
import com.vigilante.shiftsalaryplanner.settings.decodeWeatherSettings
import com.vigilante.shiftsalaryplanner.settings.defaultWeatherSettings
import com.vigilante.shiftsalaryplanner.settings.encodeWeatherCacheEntry
import com.vigilante.shiftsalaryplanner.settings.encodeWeatherSettings
import com.vigilante.shiftsalaryplanner.settings.weatherCoordinatesFromCacheKey
import com.vigilante.shiftsalaryplanner.weather.DailyHorizon
import com.vigilante.shiftsalaryplanner.weather.HourlyHorizon
import com.vigilante.shiftsalaryplanner.weather.NormalizedWeather
import com.vigilante.shiftsalaryplanner.weather.PrecipitationUnit
import com.vigilante.shiftsalaryplanner.weather.PressureUnit
import com.vigilante.shiftsalaryplanner.weather.TemperatureUnit
import com.vigilante.shiftsalaryplanner.weather.VisibilityUnit
import com.vigilante.shiftsalaryplanner.weather.WeatherCacheEntry
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherCurrent
import com.vigilante.shiftsalaryplanner.weather.WeatherDailyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherHourlyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import com.vigilante.shiftsalaryplanner.weather.WindDirectionFormat
import com.vigilante.shiftsalaryplanner.weather.WindSpeedUnit
import com.vigilante.shiftsalaryplanner.weather.weatherCacheKey
import com.vigilante.shiftsalaryplanner.weather.workWindWeatherPresentationConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline persistence tests for the M15.2 Task 3 settings/cache stores.
 *
 * The Android `SharedPreferences` stores are thin wrappers over deterministic
 * pure-Kotlin codecs; those codecs are JVM-tested here without any Android
 * runtime, network or environment dependency:
 * - [WeatherSettings] default values, full encode/decode round trip, schema
 *   marker, malformed/unknown-value tolerance to defaults;
 * - [WeatherCacheEntry] full round trip through the schema-versioned JSON codec,
 *   forward-compatible unknown-field tolerance, and safe rejection of malformed
 *   JSON, unknown schema versions, unknown providers and coordinate mismatches;
 * - structural checks that the Android store classes and the required
 *   `SettingsDataPort` / `AppDependencies` wiring exist.
 *
 * Fixtures/cache payloads are constructed inline; no file is read from the
 * network.
 */
class M15WeatherPersistenceTest {

    private val coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!

    // --- Helpers ---

    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    private fun sampleWeather(): NormalizedWeather = NormalizedWeather(
        coordinates = coordinates,
        fetchedAtMillis = 1_700_000_000_000L,
        current = WeatherCurrent(
            observedAtMillis = 1_700_000_000_000L,
            temperatureCelsius = 21.0,
            feelsLikeCelsius = 20.1,
            conditionCode = "partly_cloudy",
            relativeHumidityPercent = 58.0,
            pressureHpa = 1012.0,
            windSpeedMs = 4.2,
            windGustMs = 6.7,
            windDirectionDegrees = 260.0,
            precipitationMm = 0.5,
            visibilityMetres = 12_000.0,
            uvIndex = 4.3,
        ),
        hourly = listOf(
            WeatherHourlyPoint(
                forecastTimeMillis = 1_700_003_600_000L,
                temperatureCelsius = 20.0,
                feelsLikeCelsius = 19.0,
                conditionCode = "rain",
                relativeHumidityPercent = 62.0,
                pressureHpa = 1011.5,
                windSpeedMs = 3.8,
                windGustMs = 6.1,
                windDirectionDegrees = 255.0,
                precipitationProbabilityPercent = 40.0,
                precipitationMm = 0.2,
                visibilityMetres = 8_000.0,
            ),
        ),
        daily = listOf(
            WeatherDailyPoint(
                forecastDayMillis = 1_700_064_000_000L,
                minTemperatureCelsius = 12.0,
                maxTemperatureCelsius = 22.0,
                conditionCode = "sunny",
                relativeHumidityPercent = 50.0,
                pressureHpa = 1010.0,
                windSpeedMs = 3.0,
                windGustMs = 5.0,
                windDirectionDegrees = 240.0,
                precipitationProbabilityPercent = 10.0,
                precipitationMm = 0.0,
                sunriseMillis = 1_700_066_820_000L,
                sunsetMillis = 1_700_070_720_000L,
                uvIndexMax = 5.0,
            ),
        ),
    )

    private fun sampleEntry(): WeatherCacheEntry = WeatherCacheEntry(
        weather = sampleWeather(),
        provider = WeatherProviderId.OPEN_METEO,
        fetchedAtMillis = 1_700_000_000_000L,
    )

    // --- WeatherSettings defaults ---

    @Test
    fun `weather settings default to open meteo primary with canonical SI units and basic preset`() {
        val settings = defaultWeatherSettings()
        assertEquals(WeatherProviderId.OPEN_METEO, settings.primaryProviderId)
        assertEquals(WeatherProviderId.MET_NORWAY, settings.fallbackProviderId)
        assertEquals(TemperatureUnit.CELSIUS, settings.temperatureUnit)
        assertEquals(WindSpeedUnit.METRES_PER_SECOND, settings.windSpeedUnit)
        assertEquals(PressureUnit.HECTOPASCAL, settings.pressureUnit)
        assertEquals(PrecipitationUnit.MILLIMETRES, settings.precipitationUnit)
        assertEquals(VisibilityUnit.METRES, settings.visibilityUnit)
        assertEquals(WindDirectionFormat.DEGREES_AND_COMPASS, settings.windDirectionFormat)
        assertEquals(WeatherPresetId.BASIC, settings.presentationConfig.presetId)
        assertEquals(DEFAULT_FOREGROUND_FRESHNESS_MILLIS, settings.foregroundFreshnessMillis)

        val defaulted = defaultWeatherSettings()
        assertEquals(settings, defaulted)
    }

    @Test
    fun `weather settings encode decode round trip preserves every persisted field`() {
        val settings = defaultWeatherSettings().copy(
            primaryProviderId = WeatherProviderId.MET_NORWAY,
            fallbackProviderId = WeatherProviderId.OPEN_METEO,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            windSpeedUnit = WindSpeedUnit.KILOMETRES_PER_HOUR,
            pressureUnit = PressureUnit.MILLIMETRES_MERCURY,
            precipitationUnit = PrecipitationUnit.INCHES,
            visibilityUnit = VisibilityUnit.KILOMETRES,
            windDirectionFormat = WindDirectionFormat.COMPASS,
            presentationConfig = workWindWeatherPresentationConfig().toCustom().copy(
                hourlyHorizon = HourlyHorizon.H48,
                dailyHorizon = DailyHorizon.D10,
            ),
            foregroundFreshnessMillis = 15 * 60 * 1000L,
        )

        val restored = decodeWeatherSettings(encodeWeatherSettings(settings))

        assertEquals(settings, restored)
    }

    @Test
    fun `weather settings encoding stamps the schema and persists provider units and preset config`() {
        val raw = encodeWeatherSettings(defaultWeatherSettings())
        val root = JSONObject(raw)
        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals("OPEN_METEO", root.getString("primaryProviderId"))
        assertEquals("MET_NORWAY", root.getString("fallbackProviderId"))
        assertEquals("CELSIUS", root.getString("temperatureUnit"))
        assertEquals("METRES_PER_SECOND", root.getString("windSpeedUnit"))
        assertEquals("HECTOPASCAL", root.getString("pressureUnit"))
        assertEquals("MILLIMETRES", root.getString("precipitationUnit"))
        assertEquals("METRES", root.getString("visibilityUnit"))
        assertEquals("DEGREES_AND_COMPASS", root.getString("windDirectionFormat"))
        assertEquals(DEFAULT_FOREGROUND_FRESHNESS_MILLIS, root.getLong("foregroundFreshnessMillis"))
        val presentation = root.getJSONObject("presentation")
        assertEquals("BASIC", presentation.getString("presetId"))
        assertEquals(24, presentation.getInt("hourlyHorizonHours"))
        assertEquals(7, presentation.getInt("dailyHorizonDays"))
        assertTrue("layers must be persisted", presentation.getJSONArray("layers").length() > 0)
        assertTrue("per-metric display must be persisted", presentation.getJSONArray("metrics").length() > 0)
    }

    // --- WeatherSettings malformed / unknown-value tolerance ---

    @Test
    fun `weather settings decode tolerates unknown provider and unit values with defaults`() {
        val restored = decodeWeatherSettings(
            JSONObject()
                .put("schemaVersion", 1)
                .put("primaryProviderId", "MARS")
                .put("fallbackProviderId", "VENUS")
                .put("temperatureUnit", "KELVIN")
                .put("presetId", "BLAZING")
                .toString(),
        )

        assertEquals(defaultWeatherSettings(), restored)
    }

    @Test
    fun `weather settings decode of malformed json degrades to defaults`() {
        assertEquals(defaultWeatherSettings(), decodeWeatherSettings("this is not json {"))
    }

    @Test
    fun `weather settings decode fills absent fields with defaults`() {
        val restored = decodeWeatherSettings(
            JSONObject()
                .put("schemaVersion", 1)
                .put("foregroundFreshnessMillis", 7_200_000L)
                .toString(),
        )

        val expected = defaultWeatherSettings().copy(foregroundFreshnessMillis = 7_200_000L)
        assertEquals(expected, restored)
    }

    // --- WeatherCache codec round trips ---

    @Test
    fun `weather cache full entry round trips through the schema versioned codec`() {
        val key = weatherCacheKey(coordinates)
        val entry = sampleEntry()

        val restored = decodeWeatherCacheEntry(key, encodeWeatherCacheEntry(key, entry))

        assertNotNull("full cache entry must round trip", restored)
        assertEquals(entry, restored)
    }

    @Test
    fun `weather cache entry without current or layers still round trips`() {
        val key = weatherCacheKey(coordinates)
        val entry = WeatherCacheEntry(
            weather = NormalizedWeather(coordinates = coordinates, fetchedAtMillis = 1_700_000_000_000L),
            provider = WeatherProviderId.MET_NORWAY,
            fetchedAtMillis = 1_700_000_000_000L,
        )

        val restored = decodeWeatherCacheEntry(key, encodeWeatherCacheEntry(key, entry))

        assertEquals(entry, restored)
        assertNull(restored!!.weather.current)
        assertTrue(restored.weather.hourly.isEmpty())
        assertTrue(restored.weather.daily.isEmpty())
    }

    @Test
    fun `weather cache encoded payload carries schema coordinates provider and acquisition time`() {
        val key = weatherCacheKey(coordinates)
        val root = JSONObject(encodeWeatherCacheEntry(key, sampleEntry()))

        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals("OPEN_METEO", root.getString("provider"))
        assertEquals(1_700_000_000_000L, root.getLong("fetchedAtMillis"))
        val encodedCoordinates = root.getJSONObject("coordinates")
        assertEquals(55.7558, encodedCoordinates.getDouble("latitude"), 1e-12)
        assertEquals(37.6173, encodedCoordinates.getDouble("longitude"), 1e-12)
        assertTrue("normalized payload must be embedded", root.has("weather"))
    }

    @Test
    fun `weather cache tuple from the coordinate key must equal the stored coordinates`() {
        val key = weatherCacheKey(coordinates)
        val restoredCoordinates = weatherCoordinatesFromCacheKey(key)

        assertNotNull("the cache key must be parseable back into coordinates", restoredCoordinates)
        assertEquals(coordinates, restoredCoordinates)
    }

    @Test
    fun `weather cache decode ignores unknown top level fields for forward compatibility`() {
        val key = weatherCacheKey(coordinates)
        val root = JSONObject(encodeWeatherCacheEntry(key, sampleEntry()))
        root.put("futureField", "anything")
        root.put("futureNested", JSONObject().put("x", 1))

        val restored = decodeWeatherCacheEntry(key, root.toString())

        assertNotNull("unknown fields must not invalidate a known-schema cache entry", restored)
        assertEquals(sampleEntry(), restored)
    }

    // --- WeatherCache safe rejection of malformed / unknown payloads ---

    @Test
    fun `weather cache decode rejects malformed json`() {
        val key = weatherCacheKey(coordinates)
        assertNull(decodeWeatherCacheEntry(key, "{broken json"))
    }

    @Test
    fun `weather cache decode rejects unknown schema versions instead of guessing`() {
        val key = weatherCacheKey(coordinates)
        val unknownSchema = JSONObject(encodeWeatherCacheEntry(key, sampleEntry()))
            .put("schemaVersion", 999)
            .toString()

        assertNull("future schema versions must be ignored safely", decodeWeatherCacheEntry(key, unknownSchema))
    }

    @Test
    fun `weather cache decode rejects unknown provider ids`() {
        val key = weatherCacheKey(coordinates)
        val unknownProvider = JSONObject(encodeWeatherCacheEntry(key, sampleEntry()))
            .put("provider", "PROVIDER_FROM_THE_FUTURE")
            .toString()

        assertNull(decodeWeatherCacheEntry(key, unknownProvider))
    }

    @Test
    fun `weather cache decode rejects entries whose stored coordinates mismatch the key`() {
        val otherCoordinates = WeatherCoordinates.of(59.9115, 10.7579)!!
        val keyForOther = weatherCacheKey(otherCoordinates)
        val mismatched = encodeWeatherCacheEntry(keyForOther, sampleEntry())

        val readUnderWrongKey = decodeWeatherCacheEntry(weatherCacheKey(coordinates), mismatched)
        assertNull("entry stored under other coordinates must not be readable under this key", readUnderWrongKey)

        val readUnderOwnKey = decodeWeatherCacheEntry(keyForOther, mismatched)
        assertNotNull("the same payload must remain readable under its own key", readUnderOwnKey)
        assertEquals(
            "the embedded coordinate-keyed payload must round trip without rewriting",
            coordinates,
            readUnderOwnKey!!.weather.coordinates,
        )
        assertEquals(WeatherProviderId.OPEN_METEO, readUnderOwnKey.provider)
        assertEquals(1_700_000_000_000L, readUnderOwnKey.fetchedAtMillis)
    }

    @Test
    fun `weather cache key parsing rejects malformed coordinate keys`() {
        assertNull(weatherCoordinatesFromCacheKey(""))
        assertNull(weatherCoordinatesFromCacheKey("abc,def"))
        assertNull(weatherCoordinatesFromCacheKey("55.7558"))
        assertNull(weatherCoordinatesFromCacheKey("999.0,37.6173"))
        assertEquals(WeatherCoordinates.of(10.0, -20.0), weatherCoordinatesFromCacheKey("10.0,-20.0"))
    }

    // --- Android store + wiring structure checks (offline) ---

    @Test
    fun `settings and cache stores are profile scoped SharedPreferences wrappers`() {
        val settingsStore = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WeatherSettingsStore.kt")
        assertTrue("WeatherSettingsStore.kt must exist", settingsStore.exists())
        val settingsText = settingsStore.readText()
        assertTrue("WeatherSettingsStore must be a class", settingsText.contains("class WeatherSettingsStore"))
        assertTrue("WeatherSettingsStore must persist via profile-scoped prefs", settingsText.contains("profileSharedPreferences"))
        assertTrue("WeatherSettingsStore must expose a settings flow", settingsText.contains("settingsFlow"))
        assertTrue("WeatherSettingsStore must have a save entry point", settingsText.contains("fun save"))

        val cacheStore = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WeatherCacheStore.kt")
        assertTrue("WeatherCacheStore.kt must exist", cacheStore.exists())
        val cacheText = cacheStore.readText()
        assertTrue("WeatherCacheStore must implement WeatherCache", cacheText.contains("class WeatherCacheStore") && cacheText.contains(": WeatherCache"))
        assertTrue("WeatherCacheStore must implement read", cacheText.contains("override fun read"))
        assertTrue("WeatherCacheStore must implement write", cacheText.contains("override fun write"))
        assertTrue("WeatherCacheStore must persist via profile-scoped prefs", cacheText.contains("profileSharedPreferences"))
    }

    @Test
    fun `settings data port exposes weather settings and the app wires a weather repository`() {
        val port = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt").readText()
        assertTrue("SettingsDataPort must expose weatherSettings", port.contains("val weatherSettings: Flow<WeatherSettings>"))
        assertTrue("SettingsDataPort must expose saveWeatherSettings", port.contains("fun saveWeatherSettings"))

        val dependencies = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt").readText()
        assertTrue("AppDependencies must expose a WeatherRepository", dependencies.contains("val weatherRepository: WeatherRepository"))
        assertTrue("AppDependencies must construct the Open-Meteo adapter", dependencies.contains("OpenMeteoWeatherProvider()"))
        assertTrue("AppDependencies must construct the MET Norway adapter", dependencies.contains("MetNorwayWeatherProvider()"))
        assertTrue("AppDependencies must construct the persistent cache store", dependencies.contains("WeatherCacheStore("))
        assertTrue("AppDependencies must construct the weather settings store", dependencies.contains("WeatherSettingsStore("))
    }

    @Test
    fun `task 6 does not start task 7 today weather UI`() {
        val root = root()
        val todayWeather = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/TodayWeatherModule.kt")
        val todayScreen = File(root, "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt")
        assertFalse("Task 7 TodayWeatherModule must not exist yet", todayWeather.exists())
        if (todayScreen.exists()) {
            val text = todayScreen.readText()
            assertFalse("Task 6 must not wire TodayWeatherModule", text.contains("TodayWeatherModule("))
            assertFalse("Today UI must stay provider-independent", text.contains("WeatherProviderId"))
        }
    }
}
