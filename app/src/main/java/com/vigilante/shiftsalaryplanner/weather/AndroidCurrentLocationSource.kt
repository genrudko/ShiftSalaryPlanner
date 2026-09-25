package com.vigilante.shiftsalaryplanner.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Bounded timeout for the single-shot current-location request; the picker
 * degrades to [CurrentLocationResult.NoLocationFix] when no provider delivers
 * a fix within this window.
 */
const val DEFAULT_CURRENT_LOCATION_TIMEOUT_MILLIS = 15_000L

/**
 * Normalized outcome of a current-location request (M15.2 Task 5).
 *
 * Coordinates are authoritative and normalized through
 * [normalizeCurrentLocationResult]; unavailable (permission/provider/no-fix)
 * and error outcomes are explicit so callers never guess why the location is
 * missing and never mutate saved coordinates on failure.
 */
sealed class CurrentLocationResult {
    data class Available(
        val coordinates: WeatherCoordinates,
        val accuracyMetres: Float? = null,
        val provider: String? = null,
        val observedAtMillis: Long? = null,
    ) : CurrentLocationResult()

    /** Neither fine nor coarse location permission was granted. */
    object PermissionDenied : CurrentLocationResult()

    /** No enabled location provider (network/GPS) exists on the device. */
    object ProviderDisabled : CurrentLocationResult()

    /** No last-known fix and no single-shot result before the timeout. */
    object NoLocationFix : CurrentLocationResult()

    data class Failed(val message: String) : CurrentLocationResult()
}

/**
 * Normalizes a raw location fix into [CurrentLocationResult.Available] with
 * finite, in-range [WeatherCoordinates], or into an explicit
 * [CurrentLocationResult.Failed] for invalid/non-finite values. Pure and
 * JVM-testable: no Android types cross this boundary.
 */
fun normalizeCurrentLocationResult(
    latitude: Double,
    longitude: Double,
    accuracyMetres: Float? = null,
    provider: String? = null,
    observedAtMillis: Long? = null,
): CurrentLocationResult =
    WeatherCoordinates.of(latitude, longitude)
        ?.let { coordinates ->
            CurrentLocationResult.Available(
                coordinates = coordinates,
                accuracyMetres = accuracyMetres,
                provider = provider,
                observedAtMillis = observedAtMillis,
            )
        }
        ?: CurrentLocationResult.Failed("Invalid location coordinates")

/** Platform-independent current-location contract consumed by the weather UI. */
interface CurrentLocationSource {
    suspend fun currentLocation(): CurrentLocationResult
}
/**
 * Android `LocationManager`-based current-location source (M15.2 Task 5).
 *
 * Behavior:
 * 1. permission check first: fine OR coarse granted is enough;
 * 2. provider check: at least one of network/GPS must be enabled;
 * 3. last-known location from network then GPS is returned immediately;
 * 4. otherwise a single-shot update is requested on every enabled provider
 *    with a bounded timeout before degrading to [CurrentLocationResult.NoLocationFix];
 * 5. every path returns a normalized [CurrentLocationResult] and never
 *    references a background-location permission; no writes are performed.
 */
class AndroidCurrentLocationSource(
    private val context: Context,
    private val locationManager: LocationManager? = null,
    private val singleShotTimeoutMillis: Long = DEFAULT_CURRENT_LOCATION_TIMEOUT_MILLIS,
) : CurrentLocationSource {

    private val manager: LocationManager? = locationManager
        ?: runCatching { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }.getOrNull()

    override suspend fun currentLocation(): CurrentLocationResult = withContext(Dispatchers.IO) {
        val locationManager = manager
        when {
            locationManager == null -> CurrentLocationResult.Failed("LocationManager unavailable")
            !hasLocationPermission(context) -> CurrentLocationResult.PermissionDenied
            !hasEnabledProvider(locationManager) -> CurrentLocationResult.ProviderDisabled
            else -> try {
                lastKnownLocation(locationManager)?.toCurrentLocationResult()
                    ?: requestSingleShot(locationManager)
                    ?: CurrentLocationResult.NoLocationFix
            } catch (e: SecurityException) {
                CurrentLocationResult.PermissionDenied
            } catch (e: Exception) {
                CurrentLocationResult.Failed(e.message ?: "Failed to obtain current location")
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasEnabledProvider(locationManager: LocationManager): Boolean =
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(locationManager: LocationManager): Location? =
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleShot(locationManager: LocationManager): CurrentLocationResult? {
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { locationManager.isProviderEnabled(it) }
        if (providers.isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (continuation.isActive) {
                        terminateSingleShot(locationManager, this, handler)
                        continuation.resume(location.toCurrentLocationResult())
                    }
                }
            }
            val timeoutRunnable = Runnable {
                if (continuation.isActive) {
                    terminateSingleShot(locationManager, listener, handler)
                    continuation.resume(null)
                }
            }
            handler.postDelayed(timeoutRunnable, singleShotTimeoutMillis)
            var successfulRegistrations = 0
            providers.forEach { provider ->
                if (
                    runCatching {
                        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    }.isSuccess
                ) {
                    successfulRegistrations += 1
                }
            }
            if (successfulRegistrations == 0 && continuation.isActive) {
                terminateSingleShot(locationManager, listener, handler)
                continuation.resume(null)
            }
            continuation.invokeOnCancellation {
                terminateSingleShot(locationManager, listener, handler)
            }
        }
    }

    private fun terminateSingleShot(
        locationManager: LocationManager,
        listener: LocationListener,
        handler: Handler,
    ) {
        handler.removeCallbacksAndMessages(null)
        runCatching { locationManager.removeUpdates(listener) }
    }

    private fun Location.toCurrentLocationResult(): CurrentLocationResult =
        normalizeCurrentLocationResult(
            latitude = latitude,
            longitude = longitude,
            accuracyMetres = accuracy,
            provider = provider,
            observedAtMillis = time,
        )
}
