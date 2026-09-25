package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.weather.OPEN_METEO_GEOCODING_ENDPOINT
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherPlaceSearchResult
import com.vigilante.shiftsalaryplanner.weather.openMeteoGeocodingUrl
import com.vigilante.shiftsalaryplanner.weather.parseOpenMeteoGeocodingResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

/**
 * Offline M15.2 Task 5 geocoding tests.
 *
 * Covers only deterministic contracts that never touch the network:
 * - the Open-Meteo geocoding request builder pins the HTTPS search endpoint,
 *   encodes the query and forces JSON output;
 * - the payload parser normalizes fixed JSON fixtures into
 *   [WeatherPlaceSearchResult] with `name`, optional `admin`/`country`
 *   metadata and validated authoritative [WeatherCoordinates];
 * - malformed/non-finite/out-of-range entries are dropped and malformed
 *   payloads degrade to an empty list instead of throwing;
 * - the HTTP adapter is a thin wrapper over the bounded `HttpWeatherTransport`
 *   and never opens raw connections itself.
 */
class M15WeatherGeocodingTest {

    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    // --- Search URL builder ---

    @Test
    fun `open meteo geocoding url pins https encoded query count and json format`() {
        val url = openMeteoGeocodingUrl("Санкт-Петербург")
        assertTrue("geocoding must use HTTPS only", url.startsWith("https://"))
        assertTrue(
            "geocoding must target the Open-Meteo search endpoint",
            url.startsWith("$OPEN_METEO_GEOCODING_ENDPOINT?"),
        )
        assertTrue("query parameter must be present", url.contains("name="))
        val decodedQuery = URLDecoder.decode(url.substringAfter("name=").substringBefore('&'), "UTF-8")
        assertEquals("query must be URL-encoded and recoverable", "Санкт-Петербург", decodedQuery)
        assertTrue("count must be explicit and bounded", url.contains("count=10") || url.contains("count=5"))
        assertTrue("JSON output must be forced", url.contains("format=json"))
    }

    @Test
    fun `open meteo geocoding url trims and bounds limit`() {
        val url = openMeteoGeocodingUrl("  Kazan  ", limit = 5)
        val decodedQuery = URLDecoder.decode(url.substringAfter("name=").substringBefore('&'), "UTF-8")
        assertEquals("query must be trimmed", "Kazan", decodedQuery)
        assertTrue("limit must be forwarded", url.contains("count=5"))
    }
    // --- Payload parser ---

    @Test
    fun `geocoding parser normalizes results with name admin country and coordinates`() {
        val results = parseOpenMeteoGeocodingResults(FIXTURE_GEOCODING)
        assertEquals("fixture has two valid places", 2, results.size)

        val moscow = results[0]
        assertEquals("Moscow", moscow.name)
        assertEquals("Moscow", moscow.admin)
        assertEquals("Russia", moscow.country)
        assertEquals(55.75583, moscow.coordinates.latitude, 1e-9)
        assertEquals(37.61778, moscow.coordinates.longitude, 1e-9)

        val saintPetersburg = results[1]
        assertEquals("Saint Petersburg", saintPetersburg.name)
        assertEquals("Saint Petersburg", saintPetersburg.admin)
        assertEquals("Russia", saintPetersburg.country)
        assertEquals(59.93863, saintPetersburg.coordinates.latitude, 1e-9)
        assertEquals(30.31413, saintPetersburg.coordinates.longitude, 1e-9)
    }

    @Test
    fun `geocoding parser tolerates missing optional admin and country`() {
        val results = parseOpenMeteoGeocodingResults(
            """{"results":[{"name":"Pole of Inaccessibility","latitude":-82.06,"longitude":54.97}]}"""
        )
        assertEquals(1, results.size)
        assertEquals("Pole of Inaccessibility", results[0].name)
        assertNull("admin is optional", results[0].admin)
        assertNull("country is optional", results[0].country)
        assertEquals(-82.06, results[0].coordinates.latitude, 1e-9)
    }

    @Test
    fun `geocoding parser drops malformed entries and degrades empty payloads`() {
        val results = parseOpenMeteoGeocodingResults(
            """
            {"results":[
                {"name":"Valid","latitude":1.0,"longitude":2.0},
                {"name":"TooFarNorth","latitude":999.0,"longitude":2.0},
                {"name":"TooFarWest","latitude":1.0,"longitude":-999.0},
                {"name":"NotFinite","latitude":1e999,"longitude":2.0},
                {"latitude":1.0,"longitude":2.0},
                {"name":"NoCoordinates"},
                "not-an-object"
            ]}
            """
        )
        assertEquals("only the valid entry survives", 1, results.size)
        assertEquals("Valid", results[0].name)

        assertTrue("empty object has no results", parseOpenMeteoGeocodingResults("{}").isEmpty())
        assertTrue("no results key means empty", parseOpenMeteoGeocodingResults("""{"foo":[]}""").isEmpty())
        assertTrue("malformed payload must not throw", parseOpenMeteoGeocodingResults("not json {").isEmpty())
    }

    @Test
    fun `geocoding parser never emits non finite or out of range coordinates`() {
        val results = parseOpenMeteoGeocodingResults(FIXTURE_GEOCODING)
        assertTrue(results.isNotEmpty())
        results.forEach { result ->
            assertTrue(
                "coordinates must be finite and in range: ${result.coordinates}",
                WeatherCoordinates.isValid(result.coordinates.latitude, result.coordinates.longitude),
            )
        }
    }

    // --- Adapter structure (offline) ---

    @Test
    fun `geocoding source is a bounded https transport adapter`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/OpenMeteoGeocodingSource.kt")
        assertTrue("OpenMeteoGeocodingSource.kt must exist", file.exists())
        val text = file.readText()
        assertTrue("must declare the geocoding source interface", text.contains("interface OpenMeteoGeocodingSource"))
        assertTrue("must expose a suspend search contract", text.contains("suspend fun search"))
        assertTrue("must reuse the bounded weather transport", text.contains("HttpWeatherTransport"))
        assertTrue("must parse into WeatherPlaceSearchResult", text.contains("WeatherPlaceSearchResult"))
        assertFalse("adapter must not open raw connections itself", text.contains("URL("))
        assertFalse("no plain http allowed anywhere in the geocoding source", text.contains("http://"))
    }

    private companion object {
        const val FIXTURE_GEOCODING = """
        {
          "generationtime_ms": 1.5,
          "results": [
            {
              "id": 524894,
              "name": "Moscow",
              "latitude": 55.75583,
              "longitude": 37.61778,
              "elevation": 156.0,
              "feature_code": "PPLC",
              "country_code": "RU",
              "admin1_id": 524901,
              "admin2_id": 524894,
              "timezone": "Europe/Moscow",
              "population": 11979529,
              "country_id": 2017370,
              "country": "Russia",
              "admin1": "Moscow"
            },
            {
              "id": 499663,
              "name": "Saint Petersburg",
              "latitude": 59.93863,
              "longitude": 30.31413,
              "elevation": 11.0,
              "feature_code": "PPLC",
              "country_code": "RU",
              "admin1": "Saint Petersburg",
              "admin2": "Leningrad Oblast",
              "country": "Russia"
            }
          ]
        }
        """
    }

}
