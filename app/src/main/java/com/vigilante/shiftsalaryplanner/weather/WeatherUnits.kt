package com.vigilante.shiftsalaryplanner.weather

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Per-metric display units and pure conversion/format helpers for the M15.2
 * normalized weather domain. All conversions take the canonical value
 * (Celsius, m/s, hPa, mm, metres) and return the requested display unit.
 */

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class WindSpeedUnit { METRES_PER_SECOND, KILOMETRES_PER_HOUR, MILES_PER_HOUR, KNOTS }

enum class PressureUnit { HECTOPASCAL, MILLIBAR, MILLIMETRES_MERCURY, INCHES_MERCURY }

enum class PrecipitationUnit { MILLIMETRES, INCHES }

enum class VisibilityUnit { METRES, KILOMETRES, MILES }

/** How wind direction is rendered: degrees, compass point, or both. */
enum class WindDirectionFormat { DEGREES, COMPASS, DEGREES_AND_COMPASS }

fun convertTemperature(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
    TemperatureUnit.CELSIUS -> celsius
    TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
}

fun convertWindSpeed(metresPerSecond: Double, unit: WindSpeedUnit): Double = when (unit) {
    WindSpeedUnit.METRES_PER_SECOND -> metresPerSecond
    WindSpeedUnit.KILOMETRES_PER_HOUR -> metresPerSecond * 3.6
    WindSpeedUnit.MILES_PER_HOUR -> metresPerSecond * 2.2369362920544025
    WindSpeedUnit.KNOTS -> metresPerSecond * 1.9438444924406046
}

fun convertPressure(hectopascals: Double, unit: PressureUnit): Double = when (unit) {
    PressureUnit.HECTOPASCAL -> hectopascals
    PressureUnit.MILLIBAR -> hectopascals
    PressureUnit.MILLIMETRES_MERCURY -> hectopascals * 0.7500616827041697
    PressureUnit.INCHES_MERCURY -> hectopascals * 0.0295299830714452
}

fun convertPrecipitation(millimetres: Double, unit: PrecipitationUnit): Double = when (unit) {
    PrecipitationUnit.MILLIMETRES -> millimetres
    PrecipitationUnit.INCHES -> millimetres / 25.4
}

fun convertVisibility(metres: Double, unit: VisibilityUnit): Double = when (unit) {
    VisibilityUnit.METRES -> metres
    VisibilityUnit.KILOMETRES -> metres / 1000.0
    VisibilityUnit.MILES -> metres / 1609.344
}

fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
    val value = convertTemperature(celsius, unit)
    return when (unit) {
        TemperatureUnit.CELSIUS -> "${value.roundToInt()}°C"
        TemperatureUnit.FAHRENHEIT -> "${value.roundToInt()}°F"
    }
}

fun formatWindSpeed(metresPerSecond: Double, unit: WindSpeedUnit): String {
    val value = convertWindSpeed(metresPerSecond, unit)
    return when (unit) {
        WindSpeedUnit.METRES_PER_SECOND -> "${oneDecimal(value)} m/s"
        WindSpeedUnit.KILOMETRES_PER_HOUR -> "${oneDecimal(value)} km/h"
        WindSpeedUnit.MILES_PER_HOUR -> "${oneDecimal(value)} mph"
        WindSpeedUnit.KNOTS -> "${oneDecimal(value)} kn"
    }
}

fun formatPressure(hectopascals: Double, unit: PressureUnit): String {
    val value = convertPressure(hectopascals, unit)
    return when (unit) {
        PressureUnit.HECTOPASCAL -> "${value.roundToInt()} hPa"
        PressureUnit.MILLIBAR -> "${value.roundToInt()} mbar"
        PressureUnit.MILLIMETRES_MERCURY -> "${value.roundToInt()} mmHg"
        PressureUnit.INCHES_MERCURY -> "${twoDecimals(value)} inHg"
    }
}

fun formatPrecipitation(millimetres: Double, unit: PrecipitationUnit): String = when (unit) {
    PrecipitationUnit.MILLIMETRES -> "${oneDecimal(millimetres)} mm"
    PrecipitationUnit.INCHES -> "${twoDecimals(convertPrecipitation(millimetres, unit))} in"
}

fun formatVisibility(metres: Double, unit: VisibilityUnit): String = when (unit) {
    VisibilityUnit.METRES -> "${metres.roundToInt()} m"
    VisibilityUnit.KILOMETRES -> "${oneDecimal(convertVisibility(metres, unit))} km"
    VisibilityUnit.MILES -> "${oneDecimal(convertVisibility(metres, unit))} mi"
}

private val COMPASS_POINTS: List<String> = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

/** Maps a direction in degrees (any finite value) to its 16-point compass name. */
fun compassDirection(degrees: Double): String {
    val normalized = ((degrees % 360.0) + 360.0) % 360.0
    val index = (((normalized + 11.25) / 22.5).toInt()) % 16
    return COMPASS_POINTS[index]
}

fun formatWindDirection(degrees: Double, format: WindDirectionFormat): String = when (format) {
    WindDirectionFormat.DEGREES -> "${degrees.roundToInt()}°"
    WindDirectionFormat.COMPASS -> compassDirection(degrees)
    WindDirectionFormat.DEGREES_AND_COMPASS ->
        "${degrees.roundToInt()}° ${compassDirection(degrees)}"
}

/** Locale-independent one-decimal formatting with half-up rounding on the scaled value. */
private fun oneDecimal(value: Double): String {
    val scaled = (value * 10.0).roundToInt()
    val whole = scaled / 10
    val fraction = (scaled % 10).absoluteValue
    return "$whole.$fraction"
}

/** Locale-independent two-decimal formatting with half-up rounding on the scaled value. */
private fun twoDecimals(value: Double): String {
    val scaled = (value * 100.0).roundToInt()
    val whole = scaled / 100
    val fraction = (scaled % 100).absoluteValue.toString().padStart(2, '0')
    return "$whole.$fraction"
}
