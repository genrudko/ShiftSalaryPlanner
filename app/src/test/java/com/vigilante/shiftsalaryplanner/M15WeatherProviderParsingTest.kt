package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.weather.MET_NORWAY_USER_AGENT
import com.vigilante.shiftsalaryplanner.weather.MetNorwayWeatherProvider
import com.vigilante.shiftsalaryplanner.weather.OPEN_METEO_CURRENT_VARIABLES
import com.vigilante.shiftsalaryplanner.weather.OPEN_METEO_DAILY_VARIABLES
import com.vigilante.shiftsalaryplanner.weather.OPEN_METEO_HOURLY_VARIABLES
import com.vigilante.shiftsalaryplanner.weather.OpenMeteoWeatherProvider
import com.vigilante.shiftsalaryplanner.weather.WeatherCapability
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherFetchRequest
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import com.vigilante.shiftsalaryplanner.weather.metNorwayCompactUrl
import com.vigilante.shiftsalaryplanner.weather.metNorwayRequestHeaders
import com.vigilante.shiftsalaryplanner.weather.openMeteoForecastUrl
import com.vigilante.shiftsalaryplanner.weather.parseMetNorwayCompact
import com.vigilante.shiftsalaryplanner.weather.parseOpenMeteoForecast
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Offline, fixed-fixture parser tests for the M15.2 Task 3 provider adapters.
 *
 * Covers only deterministic contracts that never touch the network:
 * - the Open-Meteo request builder explicitly asks for canonical Celsius/m/s/mm
 *   output and the full current/hourly/daily variable set (temperature, apparent
 *   temperature, humidity, precipitation, pressure, wind speed/direction/gusts,
 *   visibility and weather code);
 * - the Open-Meteo payload parser normalizes the fixed JSON fixture;
 * - the MET Norway request builder uses the Locationforecast 2.0 compact endpoint
 *   with coordinates rounded to <=4 decimals and an app-identifying User-Agent;
 * - the MET Norway compact parser maps available temp/humidity/pressure/wind/gust/
 *   precipitation/symbol fields while unsupported visibility stays null/optional;
 * - provider capability sets advertise exactly the layers/metrics the adapters can
 *   serve (MET Norway never advertises visibility/UV/precipitation-probability);
 * - the HTTPS transport contract (status validation, bounded timeouts, UTF-8) is
 *   enforced structurally.
 *
 * No live internet is used or required; fixtures live in
 * `app/src/test/resources/weather/`.
 */
class M15WeatherProviderParsingTest {

    private val coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!

    // --- Fixture / helper plumbing ---

    private fun request(
        hourlyHours: Int = 24,
        dailyDays: Int = 7,
    ): WeatherFetchRequest = WeatherFetchRequest(
        coordinates = coordinates,
        hourlyHours = hourlyHours,
        dailyDays = dailyDays,
        requiredMetrics = emptySet(),
        optionalMetrics = setOf(WeatherMetric.VISIBILITY, WeatherMetric.UV_INDEX),
    )

    private fun resourceText(path: String): String {
        val url = requireNotNull(javaClass.classLoader?.getResource(path)) { "Missing fixture: $path" }
        return url.readText(Charsets.UTF_8)
    }

    private fun utcMillis(isoDateTime: String): Long =
        OffsetDateTime.parse(isoDateTime).toInstant().toEpochMilli()

    private fun utcDateMillis(date: String): Long =
        LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }

    // --- Open-Meteo request builder ---

    @Test
    fun `open meteo request asks canonical celsius ms mm and all current variables`() {
        val url = openMeteoForecastUrl(coordinates, request(hourlyHours = 24, dailyDays = 7))

        assertTrue("request must be HTTPS", url.startsWith("https://"))
        assertTrue("request must carry latitude", url.contains("latitude=55.7558"))
        assertTrue("request must carry longitude", url.contains("longitude=37.6173"))
        assertTrue("request must ask canonical Celsius", url.contains("temperature_unit=celsius"))
        assertTrue("request must ask canonical m/s", url.contains("wind_speed_unit=ms"))
        assertTrue("request must ask canonical millimetres", url.contains("precipitation_unit=mm"))
        assertTrue("request must pin UTC so epoch parsing is unambiguous", url.contains("timezone=UTC"))
        assertTrue("request must ask forecast_hours for the hourly horizon", url.contains("forecast_hours=24"))
        assertTrue("request must ask forecast_days for the daily horizon", url.contains("forecast_days=7"))

        val variables = listOf(
            "temperature_2m",
            "apparent_temperature",
            "relative_humidity_2m",
            "precipitation",
            "surface_pressure",
            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",
            "visibility",
            "weather_code",
        )
        for (variable in variables) {
            assertTrue(
                "current variables must explicitly request $variable (got [$OPEN_METEO_CURRENT_VARIABLES])",
                OPEN_METEO_CURRENT_VARIABLES.split(',').contains(variable),
            )
        }
        assertTrue("URL must pass the exact current variable list", url.contains("current=$OPEN_METEO_CURRENT_VARIABLES"))

        val hourlyVariables = OPEN_METEO_HOURLY_VARIABLES.split(',')
        for (variable in variables + "precipitation_probability") {
            assertTrue("hourly variables must include $variable", hourlyVariables.contains(variable))
        }
        assertTrue("URL must pass the exact hourly variable list", url.contains("hourly=$OPEN_METEO_HOURLY_VARIABLES"))

        val dailyVariables = OPEN_METEO_DAILY_VARIABLES.split(',')
        for (variable in listOf(
            "weather_code",
            "temperature_2m_max",
            "temperature_2m_min",
            "precipitation_sum",
            "precipitation_probability_max",
            "surface_pressure_max",
            "wind_speed_10m_max",
            "wind_gusts_10m_max",
            "wind_direction_10m_dominant",
            "sunrise",
            "sunset",
            "uv_index_max",
        )) {
            assertTrue("daily variables must include $variable", dailyVariables.contains(variable))
        }
        assertTrue("URL must pass the exact daily variable list", url.contains("daily=$OPEN_METEO_DAILY_VARIABLES"))
    }

    @Test
    fun `open meteo request honors requested horizons`() {
        val url = openMeteoForecastUrl(coordinates, request(hourlyHours = 12, dailyDays = 3))
        assertTrue(url.contains("forecast_hours=12"))
        assertTrue(url.contains("forecast_days=3"))
    }

    // --- Open-Meteo parsing ---

    @Test
    fun `open meteo fixture parses into normalized weather`() {
        val fetchedAtMillis = utcMillis("2026-09-16T12:05:00Z")
        val weather = parseOpenMeteoForecast(
            json = resourceText("weather/open_meteo_forecast.json"),
            coordinates = coordinates,
            fetchedAtMillis = fetchedAtMillis,
        )

        assertEquals(coordinates, weather.coordinates)
        assertEquals(fetchedAtMillis, weather.fetchedAtMillis)

        val current = weather.current
        assertNotNull("fixture has a current block", current)
        assertEquals(utcMillis("2026-09-16T12:00:00Z"), current!!.observedAtMillis)
        assertEquals(18.5, current.temperatureCelsius, 1e-9)
        assertEquals(17.2, current.feelsLikeCelsius!!, 1e-9)
        assertEquals("partly_cloudy", current.conditionCode)
        assertEquals(62.0, current.relativeHumidityPercent!!, 1e-9)
        assertEquals(1014.2, current.pressureHpa!!, 1e-9)
        assertEquals(5.3, current.windSpeedMs!!, 1e-9)
        assertEquals(240.0, current.windDirectionDegrees!!, 1e-9)
        assertEquals(8.1, current.windGustMs!!, 1e-9)
        assertEquals(0.0, current.precipitationMm!!, 1e-9)
        assertEquals(20480.0, current.visibilityMetres!!, 1e-9)

        assertEquals("fixture carries 24 hourly points", 24, weather.hourly.size)
        val firstHourly = weather.hourly.first()
        assertEquals(utcMillis("2026-09-16T13:00:00Z"), firstHourly.forecastTimeMillis)
        assertEquals(19.1, firstHourly.temperatureCelsius, 1e-9)
        assertEquals(17.8, firstHourly.feelsLikeCelsius!!, 1e-9)
        assertEquals("partly_cloudy", firstHourly.conditionCode)
        assertEquals(58.0, firstHourly.relativeHumidityPercent!!, 1e-9)
        assertEquals(1014.0, firstHourly.pressureHpa!!, 1e-9)
        assertEquals(5.0, firstHourly.windSpeedMs!!, 1e-9)
        assertEquals(235.0, firstHourly.windDirectionDegrees!!, 1e-9)
        assertEquals(7.5, firstHourly.windGustMs!!, 1e-9)
        assertEquals(0.0, firstHourly.precipitationMm!!, 1e-9)
        assertEquals(0.0, firstHourly.precipitationProbabilityPercent!!, 1e-9)
        assertEquals(18000.0, firstHourly.visibilityMetres!!, 1e-9)

        val secondTwelve = weather.hourly[12]
        assertEquals(30.0, secondTwelve.precipitationProbabilityPercent!!, 1e-9)

        assertEquals("fixture carries 7 daily points", 7, weather.daily.size)
        val firstDaily = weather.daily.first()
        assertEquals(utcDateMillis("2026-09-16"), firstDaily.forecastDayMillis)
        assertEquals(24.0, firstDaily.maxTemperatureCelsius, 1e-9)
        assertEquals(12.0, firstDaily.minTemperatureCelsius, 1e-9)
        assertEquals("partly_cloudy", firstDaily.conditionCode)
        assertEquals(0.0, firstDaily.precipitationMm!!, 1e-9)
        assertEquals(0.0, firstDaily.precipitationProbabilityPercent!!, 1e-9)
        assertEquals(1015.0, firstDaily.pressureHpa!!, 1e-9)
        assertEquals(8.0, firstDaily.windSpeedMs!!, 1e-9)
        assertEquals(12.0, firstDaily.windGustMs!!, 1e-9)
        assertEquals(240.0, firstDaily.windDirectionDegrees!!, 1e-9)
        assertEquals(utcMillis("2026-09-16T03:47:00Z"), firstDaily.sunriseMillis)
        assertEquals(utcMillis("2026-09-16T16:52:00Z"), firstDaily.sunsetMillis)
        assertEquals(4.0, firstDaily.uvIndexMax!!, 1e-9)

        val thirdDaily = weather.daily[2]
        assertEquals("rain", thirdDaily.conditionCode)
        assertEquals(2.5, thirdDaily.precipitationMm!!, 1e-9)
    }

    @Test
    fun `open meteo malformed payload fails parsing`() {
        try {
            parseOpenMeteoForecast("not json at all", coordinates, fetchedAtMillis = 1L)
            fail("malformed payload must fail parsing (provider maps it to PARSE)")
        } catch (expected: JSONException) {
            // contract under test: parse errors surface as thrown, offline-determined
        }
    }

    @Test
    fun `open meteo provider advertises every layer and metric capability`() {
        val provider = OpenMeteoWeatherProvider()
        assertEquals(WeatherProviderId.OPEN_METEO, provider.id)
        assertEquals(WeatherCapability.values().toSet(), provider.capabilities)
        assertTrue(provider.capabilities.contains(WeatherCapability.of(WeatherLayerId.CURRENT)))
        assertTrue(provider.capabilities.contains(WeatherCapability.of(WeatherLayerId.HOURLY)))
        assertTrue(provider.capabilities.contains(WeatherCapability.of(WeatherLayerId.DAILY)))
    }

    // --- MET Norway request builder ---

    @Test
    fun `met norway uses compact endpoint with coordinates rounded to at most 4 decimals`() {
        val url = metNorwayCompactUrl(WeatherCoordinates.of(59.911491, 10.757933)!!)

        assertTrue(
            "MET Norway must use the 2.0 compact locationforecast endpoint",
            url.startsWith("https://api.met.no/weatherapi/locationforecast/2.0/compact?"),
        )
        assertTrue("rounded latitude must stay <= 4 decimals, got [$url]", url.contains("lat=59.9115"))
        assertTrue("rounded longitude must stay <= 4 decimals, got [$url]", url.contains("lon=10.7579"))
    }

    @Test
    fun `met norway sends an app identifying user agent never a generic one`() {
        assertTrue(
            "User-Agent must identify the app",
            MET_NORWAY_USER_AGENT.contains("ShiftSalaryPlanner"),
        )
        for (prohibited in listOf("okhttp", "Dalvik", "Java/", "curl/", "Apache")) {
            assertFalse("User-Agent must not look like a generic client: $prohibited", MET_NORWAY_USER_AGENT.contains(prohibited))
        }
        val headers = metNorwayRequestHeaders()
        assertEquals(MET_NORWAY_USER_AGENT, headers["User-Agent"])
        assertEquals("application/json", headers["Accept"])
    }

    // --- MET Norway parsing ---

    @Test
    fun `met norway fixture parses available fields and keeps unsupported visibility null`() {
        val fetchedAtMillis = utcMillis("2026-09-16T12:05:00Z")
        val weather = parseMetNorwayCompact(
            json = resourceText("weather/met_norway_compact.json"),
            coordinates = coordinates,
            fetchedAtMillis = fetchedAtMillis,
        )

        assertEquals(coordinates, weather.coordinates)
        assertEquals(fetchedAtMillis, weather.fetchedAtMillis)
        assertTrue("MET compact has no daily layer", weather.daily.isEmpty())

        val current = weather.current
        assertNotNull("fixture has an instant at index 0", current)
        assertEquals(utcMillis("2026-09-16T12:00:00Z"), current!!.observedAtMillis)
        assertEquals(17.8, current.temperatureCelsius, 1e-9)
        assertEquals(58.0, current.relativeHumidityPercent!!, 1e-9)
        assertEquals(1014.1, current.pressureHpa!!, 1e-9)
        assertEquals(5.1, current.windSpeedMs!!, 1e-9)
        assertEquals(233.0, current.windDirectionDegrees!!, 1e-9)
        assertEquals(7.9, current.windGustMs!!, 1e-9)
        assertEquals(
            "precipitation must fall back from next_1_hours (absent) to next_6_hours",
            0.2,
            current.precipitationMm!!,
            1e-9,
        )
        assertEquals("partlycloudy_day", current.conditionCode)
        assertNull("MET Norway exposes no apparent temperature", current.feelsLikeCelsius)
        assertNull("MET Norway compact exposes no visibility", current.visibilityMetres)
        assertNull("MET Norway compact exposes no uv index", current.uvIndex)

        assertEquals("timeseries[1..] become hourly points", 4, weather.hourly.size)
        val firstHourly = weather.hourly.first()
        assertEquals(utcMillis("2026-09-16T13:00:00Z"), firstHourly.forecastTimeMillis)
        assertEquals(17.9, firstHourly.temperatureCelsius, 1e-9)
        assertEquals(57.0, firstHourly.relativeHumidityPercent!!, 1e-9)
        assertEquals(1014.0, firstHourly.pressureHpa!!, 1e-9)
        assertEquals(5.2, firstHourly.windSpeedMs!!, 1e-9)
        assertEquals(234.0, firstHourly.windDirectionDegrees!!, 1e-9)
        assertEquals(8.0, firstHourly.windGustMs!!, 1e-9)
        assertEquals(0.0, firstHourly.precipitationMm!!, 1e-9)
        assertEquals("partlycloudy_day", firstHourly.conditionCode)
        assertNull(firstHourly.visibilityMetres)
        assertNull(firstHourly.precipitationProbabilityPercent)

        val rainHour = weather.hourly[2]
        assertEquals(utcMillis("2026-09-16T15:00:00Z"), rainHour.forecastTimeMillis)
        assertEquals(18.1, rainHour.temperatureCelsius, 1e-9)
        assertEquals(0.8, rainHour.precipitationMm!!, 1e-9)
        assertEquals("rain", rainHour.conditionCode)

        val showerHour = weather.hourly[3]
        assertEquals(0.4, showerHour.precipitationMm!!, 1e-9)
        assertEquals("rainshowers_day", showerHour.conditionCode)

        assertTrue("hourly points must stay ordered by time", weather.hourly.zipWithNext().all { (a, b) ->
            a.forecastTimeMillis < b.forecastTimeMillis
        })
    }

    @Test
    fun `met norway malformed payload fails parsing`() {
        try {
            parseMetNorwayCompact("{not valid json", coordinates, fetchedAtMillis = 1L)
            fail("malformed payload must fail parsing (provider maps it to PARSE)")
        } catch (expected: JSONException) {
            // contract under test
        }
    }

    @Test
    fun `met norway provider advertises exactly its supported capabilities`() {
        val provider = MetNorwayWeatherProvider()
        assertEquals(WeatherProviderId.MET_NORWAY, provider.id)
        val capabilities = provider.capabilities
        for (capability in listOf(
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
        )) {
            assertTrue("MET Norway must advertise $capability", capability in capabilities)
        }
        for (capability in listOf(
            WeatherCapability.DAILY,
            WeatherCapability.FEELS_LIKE,
            WeatherCapability.PRECIPITATION_PROBABILITY,
            WeatherCapability.VISIBILITY,
            WeatherCapability.UV_INDEX,
        )) {
            assertFalse("MET Norway must never advertise $capability", capability in capabilities)
        }
    }

    // --- HTTPS transport contract (structural, offline) ---

    @Test
    fun `http transport enforces https bounded timeouts status validation and utf8`() {
        val transport = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/weather/HttpWeatherTransport.kt")
        assertTrue("HttpWeatherTransport.kt must exist", transport.exists())
        val text = transport.readText()
        assertTrue("transport must open an HttpURLConnection", text.contains("HttpURLConnection"))
        assertTrue("transport must validate the HTTP status", text.contains("responseCode"))
        assertTrue("transport must enforce HTTPS", text.contains("startsWith(\"https://\")"))
        assertTrue("transport must expose a bounded connect timeout", text.contains("connectTimeout"))
        assertTrue("transport must expose a bounded read timeout", text.contains("readTimeout"))
        assertTrue("transport must read the body as UTF-8", text.contains("Charsets.UTF_8"))
    }
}
