package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.defaultWeatherSettings
import com.vigilante.shiftsalaryplanner.ui.weather.mutateWeatherPresentation
import com.vigilante.shiftsalaryplanner.weather.HourlyHorizon
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class M15WeatherSettingsStructureTest {
    private fun root(): File = sequenceOf(File("."), File(".."), File("../..")).first {
        File(it, "app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt").exists()
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `manual presentation edit converts built in preset to custom`() {
        val settings = defaultWeatherSettings()
        val edited = mutateWeatherPresentation(settings) {
            copy(hourlyHorizon = HourlyHorizon.H48)
        }
        assertEquals(WeatherPresetId.CUSTOM, edited.presentationConfig.presetId)
        assertEquals(HourlyHorizon.H48, edited.presentationConfig.hourlyHorizon)
        assertEquals(WeatherPresetId.BASIC, settings.presentationConfig.presetId)
    }

    @Test
    fun `weather settings screen exposes full task 6 controls and saved location actions`() {
        val file = File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherSettingsScreen.kt")
        assertTrue("WeatherSettingsScreen.kt must exist", file.exists())
        val text = file.readText()
        for (marker in listOf(
            "fun WeatherSettingsScreen(", "primaryProviderId", "fallbackProviderId",
            "temperatureUnit", "windSpeedUnit", "pressureUnit", "precipitationUnit",
            "visibilityUnit", "windDirectionFormat", "WeatherPresetId",
            "hourlyHorizon", "dailyHorizon", "WeatherLayerId", "WeatherMetric",
            "onAddSavedLocation", "onEditSavedLocation", "onDeleteSavedLocation",
            "onReorderSavedLocations"
        )) assertTrue("missing Task 6 UI marker: $marker", text.contains(marker))
    }

    @Test
    fun `workplaces exposes weather location setup without persistence coupling`() {
        val text = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt")
        assertTrue(text.contains("onOpenWeatherLocation: (String) -> Unit"))
        assertTrue(text.contains("onOpenWeatherLocation(workplace.id)"))
        assertFalse(text.contains("WorkplaceWeatherLocationStore"))
        assertFalse(text.contains("WorkAssignmentsStore"))
    }

    @Test
    fun `navigation owns weather settings and picker overlays`() {
        val nav = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/navigation/AppNavigationState.kt")
        val main = source("app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt")
        assertTrue(nav.contains("WEATHER_SETTINGS"))
        assertTrue(nav.contains("WEATHER_LOCATION_PICKER"))
        assertTrue(main.contains("WeatherSettingsScreen("))
        assertTrue(main.contains("WeatherLocationPickerScreen("))
        assertTrue(main.contains("saveWeatherSettings("))
        assertTrue(main.contains("saveWorkplaceWeatherLocation("))
        assertTrue(main.contains("saveSavedWeatherLocation("))
        assertTrue(main.contains("reorderSavedWeatherLocations("))
    }

    @Test
    fun `task 6 does not start task 7 today weather module`() {
        assertFalse(File(root(), "app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/TodayWeatherModule.kt").exists())
        val today = source("app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt")
        assertFalse(today.contains("TodayWeatherModule("))
    }
}
