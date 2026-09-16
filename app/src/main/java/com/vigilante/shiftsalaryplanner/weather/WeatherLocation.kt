package com.vigilante.shiftsalaryplanner.weather

import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation

/**
 * Pure M15.2 weather-location source model and resolver.
 *
 * Coordinates are authoritative; labels and addresses are display metadata.
 * The resolver is a read-only pure function: it never writes to any store, so
 * a Today temporary selection can never mutate the persisted Workplace default
 * or the saved-location list.
 */
sealed class WeatherLocationSource {
    data class Workplace(val workplaceId: String) : WeatherLocationSource()
    object CurrentDevice : WeatherLocationSource()
    data class Saved(val savedLocationId: String) : WeatherLocationSource()
}

/**
 * A caller-persisted location choice. [defaultSource] is normally the active
 * Workplace; [temporaryOverride] is an in-memory Today session choice that must
 * never be persisted as a new default.
 */
data class WeatherLocationSelection(
    val defaultSource: WeatherLocationSource,
    val temporaryOverride: WeatherLocationSource? = null,
)

/** Fully resolved location with authoritative coordinates and display metadata. */
data class ResolvedWeatherLocation(
    val source: WeatherLocationSource,
    val coordinates: WeatherCoordinates,
    val label: String,
    val address: String? = null,
)

/** Display label used when a resolution falls back to the current device. */
const val CURRENT_DEVICE_LOCATION_LABEL: String = "Current device"

/**
 * Resolves a [WeatherLocationSelection] to authoritative coordinates.
 *
 * Priority:
 * 1. [WeatherLocationSelection.temporaryOverride] when it resolves;
 * 2. the default source's persisted coordinates (active Workplace location or
 *    a saved location by stable id);
 * 3. the current device, used only as a last-resort fallback and only when the
 *    caller explicitly passes [allowCurrentDeviceFallback] = true together with
 *    [currentDeviceCoordinates].
 *
 * Current device never resolves in any position without the explicit caller
 * allowance. An unresolvable override falls back to the default, and an
 * unresolvable default falls back to current device only when allowed; final
 * failure returns null. The function never throws and never mutates state.
 */
fun resolveWeatherLocation(
    selection: WeatherLocationSelection,
    workplaceLocations: Map<String, WorkplaceWeatherLocation> = emptyMap(),
    savedLocations: Map<String, SavedWeatherLocation> = emptyMap(),
    currentDeviceCoordinates: WeatherCoordinates? = null,
    allowCurrentDeviceFallback: Boolean = false,
): ResolvedWeatherLocation? {
    fun resolveSource(source: WeatherLocationSource): ResolvedWeatherLocation? = when (source) {
        is WeatherLocationSource.Workplace ->
            workplaceLocations[source.workplaceId]?.let { location ->
                ResolvedWeatherLocation(
                    source = source,
                    coordinates = location.coordinates,
                    label = location.label,
                    address = location.address,
                )
            }
        is WeatherLocationSource.Saved ->
            savedLocations[source.savedLocationId]?.let { location ->
                ResolvedWeatherLocation(
                    source = source,
                    coordinates = location.coordinates,
                    label = location.name,
                    address = location.address,
                )
            }
        WeatherLocationSource.CurrentDevice ->
            if (allowCurrentDeviceFallback && currentDeviceCoordinates != null) {
                ResolvedWeatherLocation(
                    source = source,
                    coordinates = currentDeviceCoordinates,
                    label = CURRENT_DEVICE_LOCATION_LABEL,
                )
            } else {
                null
            }
    }

    selection.temporaryOverride?.let { override ->
        resolveSource(override)?.let { return it }
    }
    resolveSource(selection.defaultSource)?.let { return it }

    if (allowCurrentDeviceFallback && currentDeviceCoordinates != null) {
        return ResolvedWeatherLocation(
            source = WeatherLocationSource.CurrentDevice,
            coordinates = currentDeviceCoordinates,
            label = CURRENT_DEVICE_LOCATION_LABEL,
        )
    }
    return null
}
