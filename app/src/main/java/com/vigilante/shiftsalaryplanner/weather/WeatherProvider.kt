package com.vigilante.shiftsalaryplanner.weather

/**
 * Pure Kotlin provider contract for the M15.2 weather subsystem.
 *
 * Providers are adapters over external weather services (Open-Meteo primary,
 * MET Norway fallback in M15.3-style wiring). They advertise their [id] and
 * [capabilities], and normalize whatever they receive into
 * [NormalizedWeather] before returning it. No provider-specific JSON, HTTP,
 * settings, location or UI types live here; repository and UI never branch on
 * provider identity.
 */

/** Stable identity of a weather data provider. */
enum class WeatherProviderId { OPEN_METEO, MET_NORWAY }

/**
 * Capability of a provider. Layer capabilities (CURRENT/HOURLY/DAILY) map to
 * the normalized forecast layers; metric capabilities map one-to-one to
 * [WeatherMetric]. A provider must advertise a capability for every required
 * layer/metric before it may serve a request.
 */
enum class WeatherCapability {
    CURRENT,
    HOURLY,
    DAILY,
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
    UV_INDEX;

    /** The presentation layer this capability serves, or null for metric capabilities. */
    val layer: WeatherLayerId?
        get() = when (this) {
            CURRENT -> WeatherLayerId.CURRENT
            HOURLY -> WeatherLayerId.HOURLY
            DAILY -> WeatherLayerId.DAILY
            else -> null
        }

    /** The normalized metric this capability serves, or null for layer capabilities. */
    val metric: WeatherMetric?
        get() = when (this) {
            TEMPERATURE -> WeatherMetric.TEMPERATURE
            FEELS_LIKE -> WeatherMetric.FEELS_LIKE
            CONDITION -> WeatherMetric.CONDITION
            HUMIDITY -> WeatherMetric.HUMIDITY
            PRESSURE -> WeatherMetric.PRESSURE
            WIND_SPEED -> WeatherMetric.WIND_SPEED
            WIND_GUST -> WeatherMetric.WIND_GUST
            WIND_DIRECTION -> WeatherMetric.WIND_DIRECTION
            PRECIPITATION -> WeatherMetric.PRECIPITATION
            PRECIPITATION_PROBABILITY -> WeatherMetric.PRECIPITATION_PROBABILITY
            VISIBILITY -> WeatherMetric.VISIBILITY
            UV_INDEX -> WeatherMetric.UV_INDEX
            else -> null
        }

    companion object {
        /** The capability that serves [layer]. */
        fun of(layer: WeatherLayerId): WeatherCapability = when (layer) {
            WeatherLayerId.CURRENT -> CURRENT
            WeatherLayerId.HOURLY -> HOURLY
            WeatherLayerId.DAILY -> DAILY
        }

        /** The capability that serves [metric]. */
        fun of(metric: WeatherMetric): WeatherCapability = when (metric) {
            WeatherMetric.TEMPERATURE -> TEMPERATURE
            WeatherMetric.FEELS_LIKE -> FEELS_LIKE
            WeatherMetric.CONDITION -> CONDITION
            WeatherMetric.HUMIDITY -> HUMIDITY
            WeatherMetric.PRESSURE -> PRESSURE
            WeatherMetric.WIND_SPEED -> WIND_SPEED
            WeatherMetric.WIND_GUST -> WIND_GUST
            WeatherMetric.WIND_DIRECTION -> WIND_DIRECTION
            WeatherMetric.PRECIPITATION -> PRECIPITATION
            WeatherMetric.PRECIPITATION_PROBABILITY -> PRECIPITATION_PROBABILITY
            WeatherMetric.VISIBILITY -> VISIBILITY
            WeatherMetric.UV_INDEX -> UV_INDEX
        }
    }
}

/** True when this capability set serves [metric]. */
infix fun Set<WeatherCapability>.supports(metric: WeatherMetric): Boolean =
    WeatherCapability.of(metric) in this

/** True when this capability set serves [layer]. */
infix fun Set<WeatherCapability>.supports(layer: WeatherLayerId): Boolean =
    WeatherCapability.of(layer) in this

/**
 * What a caller wants from a provider.
 *
 * [hourlyHours] > 0 requires the HOURLY layer for that many hours; [dailyDays] > 0
 * requires the DAILY layer for that many days. [requiredMetrics] must all be
 * advertised by the provider; [optionalMetrics] are desired but may be absent
 * without invalidating otherwise usable data. The CURRENT layer is deliberately
 * not implicitly required because [NormalizedWeather.current] is nullable.
 */
data class WeatherFetchRequest(
    val coordinates: WeatherCoordinates,
    val hourlyHours: Int,
    val dailyDays: Int,
    val requiredMetrics: Set<WeatherMetric>,
    val optionalMetrics: Set<WeatherMetric>,
)

/** Outcome of a single provider fetch attempt. */
sealed interface WeatherProviderResult {
    /** Successfully normalized weather payload. */
    data class Success(val weather: NormalizedWeather) : WeatherProviderResult

    /** The provider could not serve this request. */
    data class Failure(val reason: WeatherFailureReason) : WeatherProviderResult
}

/**
 * Why a provider could not serve a request. Distinguishes transport/server/parse
 * failures from capability mismatches so the repository can decide whether a
 * fallback provider should be tried.
 */
enum class WeatherFailureReason { TRANSPORT, SERVER, PARSE, CAPABILITY }

/**
 * A provider adapter. Implementations must normalize provider payloads into the
 * canonical domain before returning [WeatherProviderResult.Success], and must
 * not expose provider JSON/API types to callers.
 */
interface WeatherProvider {
    val id: WeatherProviderId

    /** Advertised layers and metrics this provider can serve. */
    val capabilities: Set<WeatherCapability>

    /** Fetches and normalizes weather for [request], or fails with a reason. */
    suspend fun fetch(request: WeatherFetchRequest): WeatherProviderResult
}
