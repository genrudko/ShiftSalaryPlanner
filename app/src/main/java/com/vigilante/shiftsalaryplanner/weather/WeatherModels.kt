package com.vigilante.shiftsalaryplanner.weather

/**
 * Pure Kotlin normalized weather domain for M15.2.
 *
 * Coordinates are authoritative with finite/range validation helpers; the
 * canonical values carried by the weather models are SI-ish: Celsius
 * temperature, metres/second wind, hectopascals pressure, millimetres
 * precipitation and metres visibility, with wind direction in degrees.
 * Optional metrics remain nullable so provider adapters can expose missing
 * optional fields without discarding otherwise valid data.
 */

data class WeatherCoordinates(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        /** True when [latitude] is finite and within [-90, 90]. */
        fun validLatitude(latitude: Double): Boolean =
            latitude.isFinite() && latitude in -90.0..90.0

        /** True when [longitude] is finite and within [-180, 180]. */
        fun validLongitude(longitude: Double): Boolean =
            longitude.isFinite() && longitude in -180.0..180.0

        /** True when both coordinates are finite and within their ranges. */
        fun isValid(latitude: Double, longitude: Double): Boolean =
            validLatitude(latitude) && validLongitude(longitude)

        /**
         * Returns coordinates for finite in-range values, or null when either
         * value is non-finite or outside its range.
         */
        fun of(latitude: Double, longitude: Double): WeatherCoordinates? =
            if (isValid(latitude, longitude)) WeatherCoordinates(latitude, longitude) else null
    }
}

/** Normalized current-conditions observation. */
data class WeatherCurrent(
    val observedAtMillis: Long,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double? = null,
    val conditionCode: String? = null,
    val relativeHumidityPercent: Double? = null,
    val pressureHpa: Double? = null,
    val windSpeedMs: Double? = null,
    val windGustMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationMm: Double? = null,
    val visibilityMetres: Double? = null,
    val uvIndex: Double? = null,
)

/** Normalized single-hour forecast point. */
data class WeatherHourlyPoint(
    val forecastTimeMillis: Long,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double? = null,
    val conditionCode: String? = null,
    val relativeHumidityPercent: Double? = null,
    val pressureHpa: Double? = null,
    val windSpeedMs: Double? = null,
    val windGustMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationProbabilityPercent: Double? = null,
    val precipitationMm: Double? = null,
    val visibilityMetres: Double? = null,
)

/** Normalized single-day forecast point. */
data class WeatherDailyPoint(
    val forecastDayMillis: Long,
    val minTemperatureCelsius: Double,
    val maxTemperatureCelsius: Double,
    val conditionCode: String? = null,
    val relativeHumidityPercent: Double? = null,
    val pressureHpa: Double? = null,
    val windSpeedMs: Double? = null,
    val windGustMs: Double? = null,
    val windDirectionDegrees: Double? = null,
    val precipitationProbabilityPercent: Double? = null,
    val precipitationMm: Double? = null,
    val sunriseMillis: Long? = null,
    val sunsetMillis: Long? = null,
    val uvIndexMax: Double? = null,
)

/**
 * Normalized weather payload consumed by the UI. [current] may be absent when a
 * provider cannot expose a current layer; hourly/daily lists are ordered by time.
 */
data class NormalizedWeather(
    val coordinates: WeatherCoordinates,
    val fetchedAtMillis: Long,
    val current: WeatherCurrent? = null,
    val hourly: List<WeatherHourlyPoint> = emptyList(),
    val daily: List<WeatherDailyPoint> = emptyList(),
)
