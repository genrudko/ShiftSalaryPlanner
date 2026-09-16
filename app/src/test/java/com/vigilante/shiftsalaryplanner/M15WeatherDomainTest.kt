package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.weather.DailyHorizon
import com.vigilante.shiftsalaryplanner.weather.HourlyHorizon
import com.vigilante.shiftsalaryplanner.weather.NormalizedWeather
import com.vigilante.shiftsalaryplanner.weather.PrecipitationUnit
import com.vigilante.shiftsalaryplanner.weather.PressureUnit
import com.vigilante.shiftsalaryplanner.weather.TemperatureUnit
import com.vigilante.shiftsalaryplanner.weather.VisibilityUnit
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherCurrent
import com.vigilante.shiftsalaryplanner.weather.WeatherDailyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherHourlyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherMetricDisplay
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import com.vigilante.shiftsalaryplanner.weather.WeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.WindDirectionFormat
import com.vigilante.shiftsalaryplanner.weather.WindSpeedUnit
import com.vigilante.shiftsalaryplanner.weather.basicWeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.compassDirection
import com.vigilante.shiftsalaryplanner.weather.convertPrecipitation
import com.vigilante.shiftsalaryplanner.weather.convertPressure
import com.vigilante.shiftsalaryplanner.weather.convertTemperature
import com.vigilante.shiftsalaryplanner.weather.convertVisibility
import com.vigilante.shiftsalaryplanner.weather.convertWindSpeed
import com.vigilante.shiftsalaryplanner.weather.formatPrecipitation
import com.vigilante.shiftsalaryplanner.weather.formatPressure
import com.vigilante.shiftsalaryplanner.weather.formatTemperature
import com.vigilante.shiftsalaryplanner.weather.formatVisibility
import com.vigilante.shiftsalaryplanner.weather.formatWindDirection
import com.vigilante.shiftsalaryplanner.weather.formatWindSpeed
import com.vigilante.shiftsalaryplanner.weather.workWindWeatherPresentationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused JVM tests for the M15.2 Task 1 normalized weather domain.
 *
 * Covers the pure Kotlin contract: coordinate finite/range validation, canonical
 * SI-ish values (Celsius, m/s, hPa, mm, metres visibility, degrees direction),
 * per-metric unit conversions/formatting, wind-direction compass formatting,
 * BASIC/WORK_WIND/CUSTOM presentation presets, built-in-to-Custom copy semantics
 * and the allowed 6/12/24/48h and 3/5/7/10d horizons. Optional metrics stay
 * nullable. The implementation must be pure Kotlin with no Android APIs.
 */
class M15WeatherDomainTest {

    // --- Coordinate validation helpers ---

    @Test
    fun coordinatesAcceptFiniteInRangeValues() {
        assertTrue(WeatherCoordinates.validLatitude(55.7558))
        assertTrue(WeatherCoordinates.validLongitude(37.6173))
        assertTrue(WeatherCoordinates.isValid(55.7558, 37.6173))
        val coordinates = WeatherCoordinates.of(55.7558, 37.6173)
        assertNotNull(coordinates)
        assertEquals(55.7558, coordinates!!.latitude, 1e-12)
        assertEquals(37.6173, coordinates.longitude, 1e-12)
    }

    @Test
    fun coordinatesRejectOutOfRangeLatitude() {
        assertFalse(WeatherCoordinates.validLatitude(90.5))
        assertFalse(WeatherCoordinates.validLatitude(-90.5))
        assertFalse(WeatherCoordinates.isValid(91.0, 0.0))
        assertNull(WeatherCoordinates.of(91.0, 0.0))
    }

    @Test
    fun coordinatesRejectOutOfRangeLongitude() {
        assertFalse(WeatherCoordinates.validLongitude(180.5))
        assertFalse(WeatherCoordinates.validLongitude(-181.0))
        assertFalse(WeatherCoordinates.isValid(0.0, 360.0))
        assertNull(WeatherCoordinates.of(0.0, 360.0))
    }

    @Test
    fun coordinatesRejectNonFiniteValues() {
        assertFalse(WeatherCoordinates.isValid(Double.NaN, 0.0))
        assertFalse(WeatherCoordinates.isValid(0.0, Double.POSITIVE_INFINITY))
        assertFalse(WeatherCoordinates.isValid(Double.NEGATIVE_INFINITY, Double.NaN))
        assertNull(WeatherCoordinates.of(Double.NaN, 0.0))
    }

    @Test
    fun coordinatesAcceptExactRangeBoundaries() {
        assertTrue(WeatherCoordinates.isValid(90.0, 180.0))
        assertTrue(WeatherCoordinates.isValid(-90.0, -180.0))
        assertNotNull(WeatherCoordinates.of(90.0, -180.0))
    }

    // --- Canonical unit conversions ---
    @Test
    fun convertsCanonicalCelsiusToRequestedTemperatureUnit() {
        assertEquals(23.0, convertTemperature(23.0, TemperatureUnit.CELSIUS), 1e-9)
        assertEquals(32.0, convertTemperature(0.0, TemperatureUnit.FAHRENHEIT), 1e-9)
        assertEquals(212.0, convertTemperature(100.0, TemperatureUnit.FAHRENHEIT), 1e-9)
        assertEquals(-40.0, convertTemperature(-40.0, TemperatureUnit.FAHRENHEIT), 1e-9)
    }

    @Test
    fun convertsCanonicalWindSpeedToRequestedUnit() {
        assertEquals(2.5, convertWindSpeed(2.5, WindSpeedUnit.METRES_PER_SECOND), 1e-9)
        assertEquals(3.6, convertWindSpeed(1.0, WindSpeedUnit.KILOMETRES_PER_HOUR), 1e-9)
        assertEquals(2.23694, convertWindSpeed(1.0, WindSpeedUnit.MILES_PER_HOUR), 1e-3)
        assertEquals(1.94384, convertWindSpeed(1.0, WindSpeedUnit.KNOTS), 1e-3)
    }

    @Test
    fun convertsCanonicalPressureToRequestedUnit() {
        assertEquals(1013.25, convertPressure(1013.25, PressureUnit.HECTOPASCAL), 1e-9)
        assertEquals(1013.25, convertPressure(1013.25, PressureUnit.MILLIBAR), 1e-9)
        assertEquals(750.06, convertPressure(1000.0, PressureUnit.MILLIMETRES_MERCURY), 1e-1)
        assertEquals(29.92, convertPressure(1013.25, PressureUnit.INCHES_MERCURY), 1e-2)
    }

    @Test
    fun convertsCanonicalMillimetresToRequestedUnit() {
        assertEquals(25.4, convertPrecipitation(25.4, PrecipitationUnit.MILLIMETRES), 1e-9)
        assertEquals(1.0, convertPrecipitation(25.4, PrecipitationUnit.INCHES), 1e-9)
    }

    @Test
    fun convertsCanonicalVisibilityToRequestedUnit() {
        assertEquals(10_000.0, convertVisibility(10_000.0, VisibilityUnit.METRES), 1e-9)
        assertEquals(10.0, convertVisibility(10_000.0, VisibilityUnit.KILOMETRES), 1e-9)
        assertEquals(1.0, convertVisibility(1_609.344, VisibilityUnit.MILES), 1e-6)
    }

    // --- Formatting helpers ---

    @Test
    fun formatsTemperatureWithUnitSymbol() {
        assertEquals("23°C", formatTemperature(23.4, TemperatureUnit.CELSIUS))
        assertEquals("73°F", formatTemperature(23.0, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun formatsWindSpeedWithUnitSuffix() {
        assertEquals("3.4 m/s", formatWindSpeed(3.35, WindSpeedUnit.METRES_PER_SECOND))
        assertEquals("18.0 km/h", formatWindSpeed(5.0, WindSpeedUnit.KILOMETRES_PER_HOUR))
        assertEquals("7.6 mph", formatWindSpeed(3.4, WindSpeedUnit.MILES_PER_HOUR))
        assertEquals("6.6 kn", formatWindSpeed(3.4, WindSpeedUnit.KNOTS))
    }

    @Test
    fun formatsPressureWithUnitSuffix() {
        assertEquals("1013 hPa", formatPressure(1013.25, PressureUnit.HECTOPASCAL))
        assertEquals("760 mmHg", formatPressure(1013.25, PressureUnit.MILLIMETRES_MERCURY))
        assertEquals("29.92 inHg", formatPressure(1013.25, PressureUnit.INCHES_MERCURY))
    }

    @Test
    fun formatsPrecipitationWithUnitSuffix() {
        assertEquals("5.0 mm", formatPrecipitation(5.0, PrecipitationUnit.MILLIMETRES))
        assertEquals("0.20 in", formatPrecipitation(5.0, PrecipitationUnit.INCHES))
    }

    @Test
    fun formatsVisibilityWithUnitSuffix() {
        assertEquals("10000 m", formatVisibility(10_000.0, VisibilityUnit.METRES))
        assertEquals("10.0 km", formatVisibility(10_000.0, VisibilityUnit.KILOMETRES))
        assertEquals("6.2 mi", formatVisibility(10_000.0, VisibilityUnit.MILES))
    }

    // --- Wind direction formatting ---
    @Test
    fun compassDirectionMapsSixteenPointCompass() {
        assertEquals("N", compassDirection(0.0))
        assertEquals("NNE", compassDirection(22.5))
        assertEquals("NE", compassDirection(45.0))
        assertEquals("E", compassDirection(90.0))
        assertEquals("SE", compassDirection(135.0))
        assertEquals("S", compassDirection(180.0))
        assertEquals("SW", compassDirection(225.0))
        assertEquals("W", compassDirection(270.0))
        assertEquals("NW", compassDirection(315.0))
        assertEquals("NNW", compassDirection(337.5))
        assertEquals("N", compassDirection(360.0))
        assertEquals("N", compassDirection(350.0))
        assertEquals("E", compassDirection(450.0))
        assertEquals("W", compassDirection(-90.0))
    }

    @Test
    fun formatsWindDirectionPerSelectedFormat() {
        assertEquals("270°", formatWindDirection(270.0, WindDirectionFormat.DEGREES))
        assertEquals("W", formatWindDirection(270.0, WindDirectionFormat.COMPASS))
        assertEquals("270° W", formatWindDirection(270.0, WindDirectionFormat.DEGREES_AND_COMPASS))
        assertEquals("45° NE", formatWindDirection(45.0, WindDirectionFormat.DEGREES_AND_COMPASS))
        assertEquals("273°", formatWindDirection(273.4, WindDirectionFormat.DEGREES))
        assertEquals("N", formatWindDirection(350.0, WindDirectionFormat.COMPASS))
    }

    // --- Presentation presets ---

    @Test
    fun basicPresetExposesExpectedLayerAndMetricProfile() {
        val basic = basicWeatherPresentationConfig()
        assertEquals(WeatherPresetId.BASIC, basic.presetId)
        assertEquals(setOf(WeatherLayerId.CURRENT, WeatherLayerId.DAILY), basic.layers)
        assertEquals(HourlyHorizon.H24, basic.hourlyHorizon)
        assertEquals(DailyHorizon.D7, basic.dailyHorizon)
        assertEquals(
            setOf(
                WeatherMetric.TEMPERATURE,
                WeatherMetric.CONDITION,
                WeatherMetric.FEELS_LIKE,
                WeatherMetric.HUMIDITY,
                WeatherMetric.WIND_SPEED,
                WeatherMetric.WIND_DIRECTION,
                WeatherMetric.PRECIPITATION,
            ),
            basic.metrics.filter { it.visible }.map { it.metric }.toSet(),
        )
    }

    @Test
    fun workWindPresetIsWindFocusedAndAddsHourlyLayer() {
        val workWind = workWindWeatherPresentationConfig()
        assertEquals(WeatherPresetId.WORK_WIND, workWind.presetId)
        assertEquals(
            setOf(WeatherLayerId.CURRENT, WeatherLayerId.HOURLY, WeatherLayerId.DAILY),
            workWind.layers,
        )
        assertEquals(HourlyHorizon.H24, workWind.hourlyHorizon)
        assertEquals(DailyHorizon.D5, workWind.dailyHorizon)
        val visible = workWind.metrics.filter { it.visible }.map { it.metric }.toSet()
        assertTrue(
            visible.containsAll(
                setOf(WeatherMetric.WIND_SPEED, WeatherMetric.WIND_GUST, WeatherMetric.WIND_DIRECTION),
            ),
        )
        assertTrue(visible.contains(WeatherMetric.TEMPERATURE))
        assertFalse(visible.contains(WeatherMetric.UV_INDEX))
    }

    @Test
    fun builtInMetricOrdersAreSequentialAndMatchListPosition() {
        val basic = basicWeatherPresentationConfig()
        assertEquals((0 until basic.metrics.size).toList(), basic.metrics.map { it.order })
        assertEquals(basic.metrics.map { it.metric }, basic.metrics.sortedBy { it.order }.map { it.metric })
        val workWind = workWindWeatherPresentationConfig()
        assertEquals((0 until workWind.metrics.size).toList(), workWind.metrics.map { it.order })
    }

    // --- Built-in-to-Custom copy semantics ---
    @Test
    fun toCustomCopiesBuiltInToIndependentCustomPreset() {
        val basic = basicWeatherPresentationConfig()
        val custom = basic.toCustom()

        assertEquals(WeatherPresetId.CUSTOM, custom.presetId)
        assertEquals(basic.layers, custom.layers)
        assertEquals(basic.hourlyHorizon, custom.hourlyHorizon)
        assertEquals(basic.dailyHorizon, custom.dailyHorizon)
        assertEquals(basic.metrics, custom.metrics)
        assertNotSame(basic, custom)
        assertNotSame(basic.metrics, custom.metrics)
    }

    @Test
    fun toCustomDoesNotMutateBuiltInPresets() {
        val beforeBasic = basicWeatherPresentationConfig()
        val beforeWorkWind = workWindWeatherPresentationConfig()

        beforeBasic.toCustom()
        beforeWorkWind.toCustom()

        assertEquals(beforeBasic, basicWeatherPresentationConfig())
        assertEquals(WeatherPresetId.BASIC, basicWeatherPresentationConfig().presetId)
        assertEquals(beforeWorkWind, workWindWeatherPresentationConfig())
        assertEquals(WeatherPresetId.WORK_WIND, workWindWeatherPresentationConfig().presetId)
    }

    @Test
    fun toCustomIsDeterministicAndIdempotent() {
        val first = basicWeatherPresentationConfig().toCustom()
        val second = basicWeatherPresentationConfig().toCustom()
        val again = first.toCustom()

        assertEquals(first, second)
        assertEquals(first, again)
        assertEquals(WeatherPresetId.CUSTOM, again.presetId)
    }

    // --- Allowed horizons ---

    @Test
    fun hourlyHorizonAllowsOnlyConfiguredHours() {
        assertEquals(listOf(6, 12, 24, 48), HourlyHorizon.values().map { it.hours })
        assertEquals(HourlyHorizon.H6, HourlyHorizon.fromHours(6))
        assertEquals(HourlyHorizon.H12, HourlyHorizon.fromHours(12))
        assertEquals(HourlyHorizon.H24, HourlyHorizon.fromHours(24))
        assertEquals(HourlyHorizon.H48, HourlyHorizon.fromHours(48))
        assertNull(HourlyHorizon.fromHours(0))
        assertNull(HourlyHorizon.fromHours(7))
        assertNull(HourlyHorizon.fromHours(72))
    }

    @Test
    fun dailyHorizonAllowsOnlyConfiguredDays() {
        assertEquals(listOf(3, 5, 7, 10), DailyHorizon.values().map { it.days })
        assertEquals(DailyHorizon.D3, DailyHorizon.fromDays(3))
        assertEquals(DailyHorizon.D5, DailyHorizon.fromDays(5))
        assertEquals(DailyHorizon.D7, DailyHorizon.fromDays(7))
        assertEquals(DailyHorizon.D10, DailyHorizon.fromDays(10))
        assertNull(DailyHorizon.fromDays(1))
        assertNull(DailyHorizon.fromDays(4))
        assertNull(DailyHorizon.fromDays(11))
    }

    // --- Normalized models and canonical values ---
    @Test
    fun normalizedWeatherCarriesCanonicalValuesAndTimestamps() {
        val coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!
        val current = WeatherCurrent(
            observedAtMillis = 1_700_000_000_000L,
            temperatureCelsius = 21.0,
            feelsLikeCelsius = 19.5,
            conditionCode = "partly-cloudy",
            relativeHumidityPercent = 64.0,
            pressureHpa = 1013.0,
            windSpeedMs = 4.2,
            windGustMs = 7.1,
            windDirectionDegrees = 260.0,
            precipitationMm = 0.5,
            visibilityMetres = 12_000.0,
            uvIndex = 2.0,
        )
        val hourly = listOf(
            WeatherHourlyPoint(
                forecastTimeMillis = 1_700_003_600_000L,
                temperatureCelsius = 20.0,
                windSpeedMs = 4.0,
                windDirectionDegrees = 250.0,
                precipitationProbabilityPercent = 10.0,
            ),
        )
        val daily = listOf(
            WeatherDailyPoint(
                forecastDayMillis = 1_700_064_000_000L,
                minTemperatureCelsius = 12.0,
                maxTemperatureCelsius = 22.0,
                conditionCode = "sunny",
                precipitationMm = 0.0,
            ),
        )
        val weather = NormalizedWeather(
            coordinates = coordinates,
            fetchedAtMillis = 1_700_000_000_000L,
            current = current,
            hourly = hourly,
            daily = daily,
        )

        assertEquals(coordinates, weather.coordinates)
        assertEquals(21.0, weather.current!!.temperatureCelsius, 1e-9)
        assertEquals(4.2, weather.current!!.windSpeedMs!!, 1e-9)
        assertEquals(0.5, weather.current!!.precipitationMm!!, 1e-9)
        assertEquals(12_000.0, weather.current!!.visibilityMetres!!, 1e-9)
        assertEquals(260.0, weather.current!!.windDirectionDegrees!!, 1e-9)
        assertEquals(1_700_000_000_000L, weather.current!!.observedAtMillis)
        assertEquals(1_700_003_600_000L, weather.hourly.single().forecastTimeMillis)
        assertEquals(10.0, weather.hourly.single().precipitationProbabilityPercent!!, 1e-9)
        assertEquals("sunny", weather.daily.single().conditionCode)
        assertEquals(12.0, weather.daily.single().minTemperatureCelsius, 1e-9)
        assertEquals(22.0, weather.daily.single().maxTemperatureCelsius, 1e-9)
    }

    @Test
    fun optionalWeatherMetricsAreNullableByDefault() {
        val current = WeatherCurrent(observedAtMillis = 1L, temperatureCelsius = 20.0)
        assertNull(current.feelsLikeCelsius)
        assertNull(current.conditionCode)
        assertNull(current.relativeHumidityPercent)
        assertNull(current.pressureHpa)
        assertNull(current.windSpeedMs)
        assertNull(current.windGustMs)
        assertNull(current.windDirectionDegrees)
        assertNull(current.precipitationMm)
        assertNull(current.visibilityMetres)
        assertNull(current.uvIndex)

        val hourly = WeatherHourlyPoint(forecastTimeMillis = 2L, temperatureCelsius = 19.0)
        assertNull(hourly.precipitationProbabilityPercent)
        assertNull(hourly.visibilityMetres)

        val daily = WeatherDailyPoint(
            forecastDayMillis = 3L,
            minTemperatureCelsius = 8.0,
            maxTemperatureCelsius = 21.0,
        )
        assertNull(daily.conditionCode)
        assertNull(daily.sunriseMillis)
        assertNull(daily.sunsetMillis)
        assertNull(daily.uvIndexMax)

        val weather = NormalizedWeather(
            coordinates = WeatherCoordinates.of(1.0, 2.0)!!,
            fetchedAtMillis = 4L,
        )
        assertNull(weather.current)
        assertTrue(weather.hourly.isEmpty())
        assertTrue(weather.daily.isEmpty())
    }

    @Test
    fun metricDisplayExposesVisibilityAndOrderIndependently() {
        val display = WeatherMetricDisplay(metric = WeatherMetric.WIND_GUST, visible = false, order = 3)
        assertEquals(WeatherMetric.WIND_GUST, display.metric)
        assertFalse(display.visible)
        assertEquals(3, display.order)
    }
}
