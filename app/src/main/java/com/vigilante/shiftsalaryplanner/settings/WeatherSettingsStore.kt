package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.weather.DailyHorizon
import com.vigilante.shiftsalaryplanner.weather.HourlyHorizon
import com.vigilante.shiftsalaryplanner.weather.PrecipitationUnit
import com.vigilante.shiftsalaryplanner.weather.PressureUnit
import com.vigilante.shiftsalaryplanner.weather.TemperatureUnit
import com.vigilante.shiftsalaryplanner.weather.VisibilityUnit
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherMetricDisplay
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import com.vigilante.shiftsalaryplanner.weather.WeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import com.vigilante.shiftsalaryplanner.weather.WindDirectionFormat
import com.vigilante.shiftsalaryplanner.weather.WindSpeedUnit
import com.vigilante.shiftsalaryplanner.weather.basicWeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.workWindWeatherPresentationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Persisted M15.2 weather settings.
 *
 * Carries the primary/fallback provider ids, per-metric display units, wind
 * direction format, presentation preset/config and the foreground freshness
 * default. M15.3 may later expose scheduling policy without changing these
 * contracts. Values are canonical SI references; display formatting uses the
 * pure helpers from the weather package.
 */
data class WeatherSettings(
    val primaryProviderId: WeatherProviderId = WeatherProviderId.OPEN_METEO,
    val fallbackProviderId: WeatherProviderId = WeatherProviderId.MET_NORWAY,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.METRES_PER_SECOND,
    val pressureUnit: PressureUnit = PressureUnit.HECTOPASCAL,
    val precipitationUnit: PrecipitationUnit = PrecipitationUnit.MILLIMETRES,
    val visibilityUnit: VisibilityUnit = VisibilityUnit.METRES,
    val windDirectionFormat: WindDirectionFormat = WindDirectionFormat.DEGREES_AND_COMPASS,
    val presentationConfig: WeatherPresentationConfig = basicWeatherPresentationConfig(),
    val foregroundFreshnessMillis: Long = DEFAULT_FOREGROUND_FRESHNESS_MILLIS,
)

/** Default foreground freshness threshold: re-fetch when cached data is older. */
const val DEFAULT_FOREGROUND_FRESHNESS_MILLIS: Long = 30 * 60 * 1000L

/** Default weather settings: Open-Meteo primary, MET Norway fallback, SI units. */
fun defaultWeatherSettings(): WeatherSettings = WeatherSettings()

/**
 * Deterministic schema-versioned JSON encoding of [WeatherSettings].
 *
 * The presentation config (preset, layers, horizons, per-metric visibility and
 * order) is embedded so a later settings UI can persist any preset without
 * special-casing prefs keys.
 */
fun encodeWeatherSettings(settings: WeatherSettings): String {
    val metrics = JSONArray()
    settings.presentationConfig.metrics.forEach { display ->
        metrics.put(
            JSONObject()
                .put("metric", display.metric.name)
                .put("visible", display.visible)
                .put("order", display.order),
        )
    }
    val presentation = JSONObject()
    presentation.put("presetId", settings.presentationConfig.presetId.name)
    presentation.put("layers", JSONArray(settings.presentationConfig.layers.map { it.name }.sorted()))
    presentation.put("hourlyHorizonHours", settings.presentationConfig.hourlyHorizon.hours)
    presentation.put("dailyHorizonDays", settings.presentationConfig.dailyHorizon.days)
    presentation.put("metrics", metrics)

    val root = JSONObject()
    root.put("schemaVersion", WeatherSettingsStore.SCHEMA_VERSION)
    root.put("primaryProviderId", settings.primaryProviderId.name)
    root.put("fallbackProviderId", settings.fallbackProviderId.name)
    root.put("temperatureUnit", settings.temperatureUnit.name)
    root.put("windSpeedUnit", settings.windSpeedUnit.name)
    root.put("pressureUnit", settings.pressureUnit.name)
    root.put("precipitationUnit", settings.precipitationUnit.name)
    root.put("visibilityUnit", settings.visibilityUnit.name)
    root.put("windDirectionFormat", settings.windDirectionFormat.name)
    root.put("foregroundFreshnessMillis", settings.foregroundFreshnessMillis)
    root.put("presentation", presentation)
    return root.toString()
}

/**
 * Forgiving decode of [WeatherSettings] JSON.
 *
 * Malformed documents and unknown/missing enum values degrade to defaults so a
 * corrupted or forward-compatible settings blob can never crash the app.
 */
fun decodeWeatherSettings(raw: String): WeatherSettings {
    val defaults = defaultWeatherSettings()
    return try {
        val root = JSONObject(raw)
        WeatherSettings(
            primaryProviderId = enumValueByStableName(WeatherProviderId.values(), root.optStringOrNull("primaryProviderId"))
                ?: defaults.primaryProviderId,
            fallbackProviderId = enumValueByStableName(WeatherProviderId.values(), root.optStringOrNull("fallbackProviderId"))
                ?: defaults.fallbackProviderId,
            temperatureUnit = enumValueByStableName(TemperatureUnit.values(), root.optStringOrNull("temperatureUnit"))
                ?: defaults.temperatureUnit,
            windSpeedUnit = enumValueByStableName(WindSpeedUnit.values(), root.optStringOrNull("windSpeedUnit"))
                ?: defaults.windSpeedUnit,
            pressureUnit = enumValueByStableName(PressureUnit.values(), root.optStringOrNull("pressureUnit"))
                ?: defaults.pressureUnit,
            precipitationUnit = enumValueByStableName(PrecipitationUnit.values(), root.optStringOrNull("precipitationUnit"))
                ?: defaults.precipitationUnit,
            visibilityUnit = enumValueByStableName(VisibilityUnit.values(), root.optStringOrNull("visibilityUnit"))
                ?: defaults.visibilityUnit,
            windDirectionFormat = enumValueByStableName(WindDirectionFormat.values(), root.optStringOrNull("windDirectionFormat"))
                ?: defaults.windDirectionFormat,
            presentationConfig = decodeWeatherPresentationConfig(root.optJSONObject("presentation")),
            foregroundFreshnessMillis = root.optLong("foregroundFreshnessMillis").takeIf { it > 0L }
                ?: defaults.foregroundFreshnessMillis,
        )
    } catch (e: JSONException) {
        defaults
    }
}

private fun decodeWeatherPresentationConfig(obj: JSONObject?): WeatherPresentationConfig {
    if (obj == null) return basicWeatherPresentationConfig()
    val presetId = enumValueByStableName(WeatherPresetId.values(), obj.optStringOrNull("presetId"))
        ?: WeatherPresetId.BASIC
    val builtIn = when (presetId) {
        WeatherPresetId.BASIC -> basicWeatherPresentationConfig()
        WeatherPresetId.WORK_WIND -> workWindWeatherPresentationConfig()
        WeatherPresetId.CUSTOM -> basicWeatherPresentationConfig()
    }
    val layers = decodeLayerIds(obj.optJSONArray("layers"))?.takeIf { it.isNotEmpty() }
        ?: builtIn.layers
    val hourly = HourlyHorizon.fromHours(obj.optInt("hourlyHorizonHours", builtIn.hourlyHorizon.hours))
        ?: builtIn.hourlyHorizon
    val daily = DailyHorizon.fromDays(obj.optInt("dailyHorizonDays", builtIn.dailyHorizon.days))
        ?: builtIn.dailyHorizon
    val metrics = decodeMetricDisplays(obj.optJSONArray("metrics")) ?: builtIn.metrics
    return WeatherPresentationConfig(
        presetId = presetId,
        layers = layers.toSet(),
        hourlyHorizon = hourly,
        dailyHorizon = daily,
        metrics = metrics,
    )
}

private fun decodeLayerIds(array: JSONArray?): Set<WeatherLayerId>? {
    return try {
        if (array == null) return null
        val layers = buildSet {
            for (index in 0 until array.length()) {
                val name = array.getString(index)
                val layer = WeatherLayerId.values().firstOrNull { it.name == name } ?: return null
                add(layer)
            }
        }
        layers
    } catch (e: JSONException) {
        null
    }
}

private fun decodeMetricDisplays(array: JSONArray?): List<WeatherMetricDisplay>? {
    return try {
        if (array == null) return null
        val displays = mutableListOf<WeatherMetricDisplay>()
        for (index in 0 until array.length()) {
            val element = array.optJSONObject(index) ?: return null
            val metric = enumValueByStableName(WeatherMetric.values(), element.optStringOrNull("metric")) ?: return null
            displays += WeatherMetricDisplay(
                metric = metric,
                visible = element.optBoolean("visible", false),
                order = element.optInt("order", 0),
            )
        }
        displays
    } catch (e: JSONException) {
        null
    }
}

private inline fun <reified T : Enum<T>> enumValueByStableName(values: Array<T>, name: String?): T? =
    name?.let { requested -> values.firstOrNull { it.name == requested } }

private fun JSONObject.optStringOrNull(name: String): String? = try {
    if (!has(name) || isNull(name)) null else getString(name).takeIf { it.isNotBlank() }
} catch (e: JSONException) {
    null
}

/**
 * Profile-scoped persistent store for [WeatherSettings].
 *
 * Wraps the deterministic codec in the `weather_settings` prefs file; the pure
 * codec is JVM-tested while this Android store stays a thin persistence shell.
 */
class WeatherSettingsStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(load())

    val settingsFlow: Flow<WeatherSettings> = _settingsFlow.asStateFlow()

    /** Latest decoded settings without subscribing to the flow. */
    fun snapshot(): WeatherSettings = _settingsFlow.value

    fun save(settings: WeatherSettings) {
        prefs.edit { putString(KEY_JSON, encodeWeatherSettings(settings)) }
        _settingsFlow.value = settings
    }

    private fun load(): WeatherSettings {
        val raw = prefs.getString(KEY_JSON, null) ?: ""
        return decodeWeatherSettings(raw)
    }

    companion object {
        const val PREFS_NAME = "weather_settings"
        const val KEY_JSON = "settings_json"
        const val SCHEMA_VERSION = 1
    }
}
