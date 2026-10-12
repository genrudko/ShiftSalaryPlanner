package com.vigilante.shiftsalaryplanner.weather

/**
 * Repository and cache contracts for the M15.2 weather subsystem, pure Kotlin.
 *
 * [WeatherRepository] applies primary/fallback semantics over
 * [WeatherProvider]s: it tries the primary first, falls back on
 * transport/server/parse failures or required-capability mismatches, writes each
 * successful normalized payload to [WeatherCache], and when both providers fail
 * returns the last cached payload as stale instead of clearing it. Optional
 * metrics may be absent without invalidating otherwise usable fallback data.
 * No HTTP/JSON/provider adapters/settings/location/UI code lives here.
 */

/**
 * Cached normalized payload plus the provider that produced it and when the
 * repository acquired it.
 */
data class WeatherCacheEntry(
    val weather: NormalizedWeather,
    val provider: WeatherProviderId,
    val fetchedAtMillis: Long,
)

/**
 * Persistent cache boundary for normalized weather. Implementations own
 * storage/schema concerns (M15.2 Task 3); the repository only reads the latest
 * entry keyed by coordinates and writes the latest successful payload.
 */
interface WeatherCache {
    fun read(key: String): WeatherCacheEntry?

    fun write(key: String, entry: WeatherCacheEntry)
}

/**
 * Deterministic cache key for [coordinates]. Uses [Double.toString] which is
 * locale-independent on the JVM.
 */
fun weatherCacheKey(coordinates: WeatherCoordinates): String =
    "${coordinates.latitude},${coordinates.longitude}"

/** Which provider failed and why, kept as load metadata for UI diagnostics. */
data class WeatherProviderFailure(
    val provider: WeatherProviderId,
    val reason: WeatherFailureReason,
)

/** Aggregate failure metadata when a load could not be served fresh. */
data class WeatherLoadFailure(
    val failures: List<WeatherProviderFailure>,
)

/**
 * Outcome of one [WeatherRepository.load].
 *
 * - [data]: normalized payload of the load; null only when both providers
 *   failed and no cached data existed.
 * - [sourceProvider]: provider that produced [data] (the cached entry's
 *   provider for stale data); null only when [data] is null.
 * - [usedFallback]: true when [data] came from the fallback provider
 *   (including a stale cached payload originally fetched from the fallback).
 * - [stale]: true when [data] came from cache because both providers failed.
 * - [fetchedAtMillis]: acquisition time of [data] (the load time for fresh
 *   data, the cache entry time for stale data); null only when [data] is null.
 * - [failure]: aggregate provider failures; null whenever the load produced
 *   fresh data, non-null whenever both providers failed.
 */
data class WeatherLoadResult(
    val data: NormalizedWeather?,
    val sourceProvider: WeatherProviderId?,
    val usedFallback: Boolean,
    val stale: Boolean,
    val fetchedAtMillis: Long?,
    val failure: WeatherLoadFailure?,
)

/**
 * Loads normalized weather with primary/fallback and cache/stale semantics.
 */
interface WeatherRepository {
    suspend fun load(
        request: WeatherFetchRequest,
        primaryId: WeatherProviderId,
        fallbackId: WeatherProviderId,
        nowMillis: Long,
    ): WeatherLoadResult
}

/**
 * Default primary-then-fallback repository.
 *
 * A provider is skipped (without calling [WeatherProvider.fetch]) when its
 * advertised capabilities do not cover the required layers/metrics of the
 * request; an unregistered provider id is treated as a CAPABILITY failure.
 * Successful payloads are written to [cache] with the source provider and the
 * acquisition time. When both attempts fail the latest cache entry is returned
 * as stale data and never cleared.
 */
class WeatherRepositoryImpl(
    private val providers: Map<WeatherProviderId, WeatherProvider>,
    private val cache: WeatherCache,
) : WeatherRepository {

    override suspend fun load(
        request: WeatherFetchRequest,
        primaryId: WeatherProviderId,
        fallbackId: WeatherProviderId,
        nowMillis: Long,
    ): WeatherLoadResult {
        val key = weatherCacheKey(request.coordinates)

        val primaryAttempt = attempt(request, providers[primaryId], primaryId)
        if (primaryAttempt is WeatherProviderResult.Success) {
            writeCache(key, primaryAttempt.weather, primaryId, nowMillis)
            return successResult(primaryAttempt.weather, primaryId, fallbackId, nowMillis)
        }

        // A limited fallback must still serve current/hourly conditions when it
        // cannot supply the primary provider's optional forecast layer (e.g.
        // MET Norway has no DAILY forecasts). Explicitly required metrics never
        // degrade; absent layers stay absent rather than being fabricated.
        val fallback = providers[fallbackId]
        val fallbackRequest = fallback?.let { candidate ->
            val supportsRequiredMetrics = request.requiredMetrics.all {
                WeatherCapability.of(it) in candidate.capabilities
            }
            if (!supportsRequiredMetrics) request
            else {
                val hourly = request.hourlyHours.takeIf {
                    WeatherCapability.HOURLY in candidate.capabilities
                } ?: 0
                val daily = request.dailyDays.takeIf {
                    WeatherCapability.DAILY in candidate.capabilities
                } ?: 0
                val servesSomeLayer = WeatherCapability.CURRENT in candidate.capabilities ||
                    hourly > 0 || daily > 0
                if (servesSomeLayer) request.copy(hourlyHours = hourly, dailyDays = daily)
                else request
            }
        } ?: request
        val fallbackAttempt = attempt(fallbackRequest, fallback, fallbackId)
        if (fallbackAttempt is WeatherProviderResult.Success) {
            writeCache(key, fallbackAttempt.weather, fallbackId, nowMillis)
            return successResult(fallbackAttempt.weather, fallbackId, fallbackId, nowMillis)
        }

        val failures = listOfNotNull(
            failureOf(primaryAttempt, primaryId),
            failureOf(fallbackAttempt, fallbackId),
        )

        val cached = cache.read(key)
        if (cached != null) {
            return WeatherLoadResult(
                data = cached.weather,
                sourceProvider = cached.provider,
                usedFallback = cached.provider == fallbackId,
                stale = true,
                fetchedAtMillis = cached.fetchedAtMillis,
                failure = WeatherLoadFailure(failures),
            )
        }
        return WeatherLoadResult(
            data = null,
            sourceProvider = null,
            usedFallback = false,
            stale = false,
            fetchedAtMillis = null,
            failure = WeatherLoadFailure(failures),
        )
    }

    private suspend fun attempt(
        request: WeatherFetchRequest,
        provider: WeatherProvider?,
        providerId: WeatherProviderId,
    ): WeatherProviderResult {
        if (provider == null || !provider.capabilities.containsAll(requiredCapabilities(request))) {
            return WeatherProviderResult.Failure(WeatherFailureReason.CAPABILITY)
        }
        return provider.fetch(request)
    }

    private fun requiredCapabilities(request: WeatherFetchRequest): Set<WeatherCapability> = buildSet {
        if (request.hourlyHours > 0) add(WeatherCapability.of(WeatherLayerId.HOURLY))
        if (request.dailyDays > 0) add(WeatherCapability.of(WeatherLayerId.DAILY))
        request.requiredMetrics.forEach { add(WeatherCapability.of(it)) }
    }

    private fun successResult(
        weather: NormalizedWeather,
        sourceProvider: WeatherProviderId,
        fallbackId: WeatherProviderId,
        nowMillis: Long,
    ): WeatherLoadResult = WeatherLoadResult(
        data = weather,
        sourceProvider = sourceProvider,
        usedFallback = sourceProvider == fallbackId,
        stale = false,
        fetchedAtMillis = nowMillis,
        failure = null,
    )

    private fun writeCache(
        key: String,
        weather: NormalizedWeather,
        provider: WeatherProviderId,
        nowMillis: Long,
    ) {
        cache.write(key, WeatherCacheEntry(weather = weather, provider = provider, fetchedAtMillis = nowMillis))
    }

    private fun failureOf(
        result: WeatherProviderResult,
        providerId: WeatherProviderId,
    ): WeatherProviderFailure? = when (result) {
        is WeatherProviderResult.Success -> null
        is WeatherProviderResult.Failure -> WeatherProviderFailure(providerId, result.reason)
    }
}
