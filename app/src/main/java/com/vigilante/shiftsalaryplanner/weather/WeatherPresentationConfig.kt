package com.vigilante.shiftsalaryplanner.weather

/**
 * Pure Kotlin weather presentation configuration for M15.2.
 *
 * Built-in BASIC and WORK_WIND presets are immutable value factories; editing
 * them flows through [WeatherPresentationConfig.toCustom] which copies the
 * built-in to an independent CUSTOM preset without mutating the built-in.
 */

enum class WeatherPresetId { BASIC, WORK_WIND, CUSTOM }

enum class WeatherLayerId { CURRENT, HOURLY, DAILY }

/** Normalized weather metrics that can be shown or hidden and reordered. */
enum class WeatherMetric {
    TEMPERATURE,
    FEELS_LIKE,
    CONDITION,
    HUMIDITY,
    PRESSURE,
    WIND_SPEED,
    WIND_GUST,
    WIND_DIRECTION,
    PRECIPITATION,
    PRECIPITATION_PROBABILITY,
    VISIBILITY,
    UV_INDEX,
}

/** Allowed hourly forecast horizons. */
enum class HourlyHorizon(val hours: Int) {
    H6(6),
    H12(12),
    H24(24),
    H48(48);

    companion object {
        /** Returns the horizon for exactly [hours], or null for unsupported values. */
        fun fromHours(hours: Int): HourlyHorizon? = values().firstOrNull { it.hours == hours }
    }
}

/** Allowed daily forecast horizons. */
enum class DailyHorizon(val days: Int) {
    D3(3),
    D5(5),
    D7(7),
    D10(10);

    companion object {
        /** Returns the horizon for exactly [days], or null for unsupported values. */
        fun fromDays(days: Int): DailyHorizon? = values().firstOrNull { it.days == days }
    }
}

/** Per-metric display entry: visibility switch plus explicit ordering position. */
data class WeatherMetricDisplay(
    val metric: WeatherMetric,
    val visible: Boolean,
    val order: Int,
)

/**
 * Active weather presentation: enabled layers, horizon lengths and ordered
 * per-metric display entries.
 */
data class WeatherPresentationConfig(
    val presetId: WeatherPresetId,
    val layers: Set<WeatherLayerId>,
    val hourlyHorizon: HourlyHorizon,
    val dailyHorizon: DailyHorizon,
    val metrics: List<WeatherMetricDisplay>,
) {
    /**
     * Copy of this built-in (or custom) config as an independent CUSTOM preset.
     * The receiver (built-in or custom) is never mutated.
     */
    fun toCustom(): WeatherPresentationConfig = copy(
        presetId = WeatherPresetId.CUSTOM,
        layers = layers.toSet(),
        metrics = metrics.toList(),
    )
}

/** Default BASIC preset: concise current + daily summary. */
fun basicWeatherPresentationConfig(): WeatherPresentationConfig = WeatherPresentationConfig(
    presetId = WeatherPresetId.BASIC,
    layers = setOf(WeatherLayerId.CURRENT, WeatherLayerId.DAILY),
    hourlyHorizon = HourlyHorizon.H24,
    dailyHorizon = DailyHorizon.D7,
    metrics = metricDisplayList(
        WeatherMetric.TEMPERATURE to true,
        WeatherMetric.CONDITION to true,
        WeatherMetric.FEELS_LIKE to true,
        WeatherMetric.HUMIDITY to true,
        WeatherMetric.WIND_SPEED to true,
        WeatherMetric.WIND_DIRECTION to true,
        WeatherMetric.PRECIPITATION to true,
        WeatherMetric.WIND_GUST to false,
        WeatherMetric.PRESSURE to false,
        WeatherMetric.PRECIPITATION_PROBABILITY to false,
        WeatherMetric.VISIBILITY to false,
        WeatherMetric.UV_INDEX to false,
    ),
)

/** WORK_WIND preset: wind-focused metrics with the hourly layer enabled. */
fun workWindWeatherPresentationConfig(): WeatherPresentationConfig = WeatherPresentationConfig(
    presetId = WeatherPresetId.WORK_WIND,
    layers = setOf(WeatherLayerId.CURRENT, WeatherLayerId.HOURLY, WeatherLayerId.DAILY),
    hourlyHorizon = HourlyHorizon.H24,
    dailyHorizon = DailyHorizon.D5,
    metrics = metricDisplayList(
        WeatherMetric.WIND_SPEED to true,
        WeatherMetric.WIND_GUST to true,
        WeatherMetric.WIND_DIRECTION to true,
        WeatherMetric.TEMPERATURE to true,
        WeatherMetric.CONDITION to true,
        WeatherMetric.PRECIPITATION to true,
        WeatherMetric.PRECIPITATION_PROBABILITY to true,
        WeatherMetric.PRESSURE to true,
        WeatherMetric.FEELS_LIKE to false,
        WeatherMetric.HUMIDITY to false,
        WeatherMetric.VISIBILITY to false,
        WeatherMetric.UV_INDEX to false,
    ),
)

private fun metricDisplayList(vararg entries: Pair<WeatherMetric, Boolean>): List<WeatherMetricDisplay> =
    entries.mapIndexed { index, (metric, visible) ->
        WeatherMetricDisplay(metric = metric, visible = visible, order = index)
    }
