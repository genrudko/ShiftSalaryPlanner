package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.defaultWeatherSettings
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
import com.vigilante.shiftsalaryplanner.ui.weather.WeatherSettingsScreen
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import com.vigilante.shiftsalaryplanner.weather.WeatherRule
import com.vigilante.shiftsalaryplanner.weather.WeatherRulesConfig
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherRuleCondition
import com.vigilante.shiftsalaryplanner.weather.workWindWeatherPresentationConfig

private val m15WeatherSettings = defaultWeatherSettings().copy(
    presentationConfig = workWindWeatherPresentationConfig().copy(presetId = WeatherPresetId.WORK_WIND)
)

private val m15SavedLocations = listOf(
    SavedWeatherLocation(
        id = "saved-1",
        name = "Кочубеевская ВЭС",
        coordinates = WeatherCoordinates(44.664, 41.846),
        address = "Ставропольский край",
        order = 0,
    ),
    SavedWeatherLocation(
        id = "saved-2",
        name = "Дом",
        coordinates = WeatherCoordinates(45.035, 41.969),
        address = "Ставрополь",
        order = 1,
    ),
)

private val m15WeatherWorkplaces = listOf(
    Workplace(WORKPLACE_MAIN_ID, "Северный парк"),
    Workplace(WORKPLACE_SECOND_ID, "Подстанция"),
)

private val m15WorkplaceWeather = mapOf(
    WORKPLACE_MAIN_ID to WorkplaceWeatherLocation(
        WORKPLACE_MAIN_ID,
        WeatherCoordinates(44.664, 41.846),
        "Кочубеевская ВЭС",
        "Ставропольский край",
    ),
    WORKPLACE_SECOND_ID to WorkplaceWeatherLocation(
        WORKPLACE_SECOND_ID,
        WeatherCoordinates(45.035, 41.969),
        "Ставрополь",
        null,
    ),
)

@Composable
private fun WeatherPreviewTheme(dark: Boolean, content: @Composable () -> Unit) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE,
        )
    ) {
        val roles = evolutionColorRoles()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = roles.appBackground,
            contentColor = roles.contentPrimary,
        ) { content() }
    }
}

@Composable
private fun WeatherSettingsPreview(dark: Boolean, showRules: Boolean = false) = WeatherPreviewTheme(dark) {
    WeatherSettingsScreen(
        settings = m15WeatherSettings,
        savedLocations = m15SavedLocations,
        onBack = {},
        onSettingsChange = {},
        onAddSavedLocation = {},
        onEditSavedLocation = {},
        onDeleteSavedLocation = {},
        onReorderSavedLocations = {},
        rulesConfig = WeatherRulesConfig(globalRules = listOf(
            WeatherRule("wind-high", WeatherMetric.WIND_SPEED, WeatherRuleCondition.AT_LEAST, 12.0),
            WeatherRule("gust-high", WeatherMetric.WIND_GUST, WeatherRuleCondition.AT_LEAST, 20.0),
        )),
        workplacesWithWeather = listOf(WORKPLACE_MAIN_ID to "Северный парк"),
        initiallyShowAdvancedRules = showRules,
        initiallyScrollToRules = showRules,
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewTest
@Preview(name = "M15 Weather settings light", widthDp = 412, heightDp = 2800, showBackground = true)
@Composable
fun m15WeatherSettingsLight() = WeatherSettingsPreview(false)

@PreviewTest
@Preview(
    name = "M15 Weather settings dark",
    widthDp = 412,
    heightDp = 2800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun m15WeatherSettingsDark() = WeatherSettingsPreview(true)

@PreviewTest
@Preview(
    name = "M15 Weather settings large font",
    widthDp = 412,
    heightDp = 3600,
    fontScale = 1.3f,
    showBackground = true,
)
@Composable
fun m15WeatherSettingsLargeFont() = WeatherSettingsPreview(false)

@PreviewTest
@Preview(name = "M15 Workplace weather locations", widthDp = 412, heightDp = 1000, showBackground = true)
@Composable
fun m15WeatherWorkplaceLocations() = WeatherPreviewTheme(false) {
    WorkplacesScreen(
        workplaces = m15WeatherWorkplaces,
        activeWorkplaceId = WORKPLACE_MAIN_ID,
        onBack = {},
        onSelectWorkplace = {},
        onRenameWorkplaces = {},
        onOpenPayrollSettings = {},
        weatherLocations = m15WorkplaceWeather,
        onOpenWeatherLocation = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@PreviewTest
@Preview(name = "M15 Weather rules expanded", widthDp = 412, heightDp = 2300, showBackground = true)
@Composable
fun m15WeatherRulesExpanded() = WeatherSettingsPreview(dark = false, showRules = true)
