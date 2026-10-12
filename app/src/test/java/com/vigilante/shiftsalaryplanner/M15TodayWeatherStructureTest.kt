package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.defaultWeatherSettings
import com.vigilante.shiftsalaryplanner.ui.weather.buildTodayWeatherRequest
import com.vigilante.shiftsalaryplanner.ui.weather.currentWeatherMetricRows
import com.vigilante.shiftsalaryplanner.ui.weather.hourlyWeatherPoints
import com.vigilante.shiftsalaryplanner.ui.weather.dailyWeatherPoints
import com.vigilante.shiftsalaryplanner.weather.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class M15TodayWeatherStructureTest {
    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }
    private fun source(path: String): String = File(root(), path).readText()
    private val coords = WeatherCoordinates(44.66, 41.85)

    @Test fun `presentation request preserves horizons without requiring optional metrics`() {
        val request = buildTodayWeatherRequest(coords, defaultWeatherSettings())
        assertEquals(coords, request.coordinates)
        assertEquals(0, request.hourlyHours)
        assertEquals(7, request.dailyDays)
        assertTrue(request.requiredMetrics.isEmpty())
        assertTrue(WeatherMetric.WIND_GUST in request.optionalMetrics)
    }

    @Test fun `optional metric absence and visibility ordering are handled without crashing`() {
        val settings = defaultWeatherSettings().copy(
            presentationConfig = workWindWeatherPresentationConfig()
        )
        val current = WeatherCurrent(observedAtMillis = 1L, temperatureCelsius = 15.0)
        val rows = currentWeatherMetricRows(current, settings)
        assertTrue(rows.any { it.first == WeatherMetric.TEMPERATURE })
        assertFalse(rows.any { it.first == WeatherMetric.WIND_GUST })
        assertEquals(WeatherMetric.TEMPERATURE, rows.first { it.first == WeatherMetric.TEMPERATURE }.first)
    }

    @Test fun `hourly and daily forecast obey configured windows and ignore past hourly points`() {
        val config = workWindWeatherPresentationConfig().copy(
            hourlyHorizon = HourlyHorizon.H6, dailyHorizon = DailyHorizon.D3
        )
        val weather = NormalizedWeather(
            coordinates = coords, fetchedAtMillis = 0,
            hourly = (0..20).map { WeatherHourlyPoint(it * 3600_000L, it.toDouble()) },
            daily = (0..8).map { WeatherDailyPoint(it * 86_400_000L, 0.0, 10.0) }
        )
        assertEquals(6, hourlyWeatherPoints(weather, config, nowMillis = 3_600_000L).size)
        assertEquals(3_600_000L, hourlyWeatherPoints(weather, config, nowMillis = 3_600_000L).first().forecastTimeMillis)
        assertEquals(3, dailyWeatherPoints(weather, config).size)
    }

    @Test fun `today hero and expanded UI are provider agnostic and wired through repository`() {
        val today = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt")
        val module = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/TodayWeatherModule.kt")
        val main = source("app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt")
        assertTrue(today.contains("TodayWeatherHero("))
        assertTrue(today.contains("TodayWeatherModule("))
        assertTrue(main.contains("weatherRepository.load("))
        assertTrue(main.contains("resolveWeatherLocation("))
        assertTrue(main.contains("temporaryWeatherLocation"))
        assertTrue(main.contains("WeatherLocationSource.CurrentDevice"))
        assertFalse(today.contains("OpenMeteoWeatherProvider"))
        assertFalse(module.contains("MetNorwayWeatherProvider"))
        assertFalse(module.contains("OpenMeteoWeatherProvider"))
        assertFalse(main.contains("saveWorkplaceWeatherLocation(temporaryWeatherLocation"))
    }
}
