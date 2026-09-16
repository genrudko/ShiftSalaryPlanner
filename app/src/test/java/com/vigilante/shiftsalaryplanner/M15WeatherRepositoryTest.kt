package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.weather.NormalizedWeather
import com.vigilante.shiftsalaryplanner.weather.WeatherCache
import com.vigilante.shiftsalaryplanner.weather.WeatherCacheEntry
import com.vigilante.shiftsalaryplanner.weather.WeatherCapability
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherCurrent
import com.vigilante.shiftsalaryplanner.weather.WeatherFailureReason
import com.vigilante.shiftsalaryplanner.weather.WeatherFetchRequest
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherLoadFailure
import com.vigilante.shiftsalaryplanner.weather.WeatherLoadResult
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherProvider
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderFailure
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderResult
import com.vigilante.shiftsalaryplanner.weather.WeatherRepository
import com.vigilante.shiftsalaryplanner.weather.WeatherRepositoryImpl
import com.vigilante.shiftsalaryplanner.weather.supports
import com.vigilante.shiftsalaryplanner.weather.weatherCacheKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused JVM tests for the M15.2 Task 2 provider contract, repository fallback
 * and cache/stale semantics.
 *
 * Covers pure Kotlin behavior only: primary success, transport failure fallback,
 * required-capability mismatch fallback (the mismatched provider is never
 * fetched), optional-metric absence without discarding usable fallback data,
 * cache write on successful loads, stale cached return when both providers fail,
 * and a total-failure result with no cached data. No HTTP/JSON/provider
 * adapters/settings/location/UI/Android APIs participate.
 */
class M15WeatherRepositoryTest {

    private val coordinates = WeatherCoordinates.of(55.7558, 37.6173)!!

    private val nowMillis = 1_700_000_000_000L

    // --- Fakes ---

    private class FakeWeatherProvider(
        override val id: WeatherProviderId,
        override val capabilities: Set<WeatherCapability>,
        private val result: WeatherProviderResult,
    ) : WeatherProvider {
        val fetchCalls = mutableListOf<WeatherFetchRequest>()

        override suspend fun fetch(request: WeatherFetchRequest): WeatherProviderResult {
            fetchCalls += request
            return result
        }
    }

    private class InMemoryWeatherCache : WeatherCache {
        val entries = mutableMapOf<String, WeatherCacheEntry>()

        override fun read(key: String): WeatherCacheEntry? = entries[key]

        override fun write(key: String, entry: WeatherCacheEntry) {
            entries[key] = entry
        }
    }

    // --- Fixture helpers ---

    private fun normalizedWeather(
        fetchedAtMillis: Long,
        temperatureCelsius: Double,
        visibilityMetres: Double? = null,
        uvIndex: Double? = null,
    ): NormalizedWeather = NormalizedWeather(
        coordinates = coordinates,
        fetchedAtMillis = fetchedAtMillis,
        current = WeatherCurrent(
            observedAtMillis = fetchedAtMillis,
            temperatureCelsius = temperatureCelsius,
            visibilityMetres = visibilityMetres,
            uvIndex = uvIndex,
        ),
    )

    private fun request(
        requiredMetrics: Set<WeatherMetric> = emptySet(),
        optionalMetrics: Set<WeatherMetric> = emptySet(),
        hourlyHours: Int = 24,
        dailyDays: Int = 7,
    ): WeatherFetchRequest = WeatherFetchRequest(
        coordinates = coordinates,
        hourlyHours = hourlyHours,
        dailyDays = dailyDays,
        requiredMetrics = requiredMetrics,
        optionalMetrics = optionalMetrics,
    )

    private fun fullCapabilities(): Set<WeatherCapability> = WeatherCapability.values().toSet()

    private fun repository(
        primary: FakeWeatherProvider,
        fallback: FakeWeatherProvider,
        cache: InMemoryWeatherCache = InMemoryWeatherCache(),
    ): WeatherRepository = WeatherRepositoryImpl(
        providers = mapOf(
            WeatherProviderId.OPEN_METEO to primary,
            WeatherProviderId.MET_NORWAY to fallback,
        ),
        cache = cache,
    )

    // --- Capability contract ---

    @Test
    fun capabilityEnumCoversLayersAndAllMetrics() {
        assertEquals(WeatherCapability.CURRENT, WeatherCapability.of(WeatherLayerId.CURRENT))
        assertEquals(WeatherCapability.HOURLY, WeatherCapability.of(WeatherLayerId.HOURLY))
        assertEquals(WeatherCapability.DAILY, WeatherCapability.of(WeatherLayerId.DAILY))

        WeatherMetric.values().forEach { metric ->
            val capability = WeatherCapability.of(metric)
            assertEquals(metric, capability.metric)
            assertTrue(WeatherCapability.values().toSet() supports metric)
        }

        assertTrue(WeatherCapability.values().toSet() supports WeatherLayerId.CURRENT)
        assertTrue(WeatherCapability.values().toSet() supports WeatherLayerId.HOURLY)
        assertTrue(WeatherCapability.values().toSet() supports WeatherLayerId.DAILY)
    }

    // --- Primary success ---

    @Test
    fun primarySuccessReturnsFreshPrimaryData() = runBlocking {
        val primaryWeather = normalizedWeather(nowMillis, temperatureCelsius = 21.0)
        val fallbackWeather = normalizedWeather(nowMillis, temperatureCelsius = 18.0)
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Success(primaryWeather),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Success(fallbackWeather),
        )
        val cache = InMemoryWeatherCache()
        val repository = repository(primary, fallback, cache)
        val fetchRequest = request()

        val result: WeatherLoadResult = repository.load(
            fetchRequest,
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(primaryWeather, result.data)
        assertEquals(21.0, result.data!!.current!!.temperatureCelsius, 1e-9)
        assertEquals(WeatherProviderId.OPEN_METEO, result.sourceProvider)
        assertFalse(result.usedFallback)
        assertFalse(result.stale)
        assertEquals(nowMillis, result.fetchedAtMillis)
        assertNull(result.failure)
        assertEquals(listOf(fetchRequest), primary.fetchCalls)
        assertTrue(fallback.fetchCalls.isEmpty())
    }

    // --- Transport fallback + cache write on fallback success ---

    @Test
    fun transportFailureFallsBackToSecondaryProviderAndWritesCache() = runBlocking {
        val fallbackWeather = normalizedWeather(nowMillis, temperatureCelsius = 15.0)
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.TRANSPORT),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Success(fallbackWeather),
        )
        val cache = InMemoryWeatherCache()
        val repository = repository(primary, fallback, cache)
        val fetchRequest = request()

        val result = repository.load(
            fetchRequest,
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(fallbackWeather, result.data)
        assertEquals(WeatherProviderId.MET_NORWAY, result.sourceProvider)
        assertTrue(result.usedFallback)
        assertFalse(result.stale)
        assertEquals(nowMillis, result.fetchedAtMillis)
        assertNull(result.failure)
        assertEquals(listOf(fetchRequest), primary.fetchCalls)
        assertEquals(listOf(fetchRequest), fallback.fetchCalls)

        val cached = cache.entries[weatherCacheKey(coordinates)]
        assertNotNull("fallback success must be written to cache", cached)
        assertEquals(fallbackWeather, cached!!.weather)
        assertEquals(WeatherProviderId.MET_NORWAY, cached.provider)
        assertEquals(nowMillis, cached.fetchedAtMillis)
    }

    // --- Required-capability mismatch fallback ---

    @Test
    fun requiredCapabilityMismatchFallsBackWithoutFetchingPrimary() = runBlocking {
        val fallbackWeather = normalizedWeather(nowMillis, temperatureCelsius = 16.0)
        val primaryWithoutWindGust = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities() - WeatherCapability.WIND_GUST,
            WeatherProviderResult.Success(normalizedWeather(nowMillis, temperatureCelsius = 25.0)),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Success(fallbackWeather),
        )
        val repository = repository(primaryWithoutWindGust, fallback)
        val fetchRequest = request(requiredMetrics = setOf(WeatherMetric.WIND_GUST))

        val result = repository.load(
            fetchRequest,
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(fallbackWeather, result.data)
        assertEquals(WeatherProviderId.MET_NORWAY, result.sourceProvider)
        assertTrue(result.usedFallback)
        assertFalse(result.stale)
        assertNull(result.failure)
        assertTrue("primary lacking a required capability must not be fetched", primaryWithoutWindGust.fetchCalls.isEmpty())
        assertEquals(listOf(fetchRequest), fallback.fetchCalls)
    }

    // --- Optional metric absence is accepted ---

    @Test
    fun optionalMetricAbsenceDoesNotInvalidateUsableFallbackData() = runBlocking {
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.SERVER),
        )
        // Fallback returns no visibility/UV even though the caller asked for them as optional.
        val fallbackWithoutOptionalMetrics = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Success(
                normalizedWeather(nowMillis, temperatureCelsius = 17.0),
            ),
        )
        val repository = repository(primary, fallbackWithoutOptionalMetrics)
        val fetchRequest = request(optionalMetrics = setOf(WeatherMetric.VISIBILITY, WeatherMetric.UV_INDEX))

        val result = repository.load(
            fetchRequest,
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(WeatherProviderId.MET_NORWAY, result.sourceProvider)
        assertTrue(result.usedFallback)
        assertFalse(result.stale)
        assertNull(result.failure)
        assertEquals(17.0, result.data!!.current!!.temperatureCelsius, 1e-9)
        assertNull(result.data!!.current!!.visibilityMetres)
        assertNull(result.data!!.current!!.uvIndex)
    }

    // --- Cache write on primary success ---

    @Test
    fun successfulLoadWritesNormalizedDataToCache() = runBlocking {
        val primaryWeather = normalizedWeather(nowMillis, temperatureCelsius = 22.0)
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Success(primaryWeather),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.TRANSPORT),
        )
        val cache = InMemoryWeatherCache()
        val repository = repository(primary, fallback, cache)

        val result = repository.load(
            request(),
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(primaryWeather, result.data)
        assertNull(result.failure)
        val cached = cache.entries[weatherCacheKey(coordinates)]
        assertNotNull("primary success must be written to cache", cached)
        assertEquals(primaryWeather, cached!!.weather)
        assertEquals(WeatherProviderId.OPEN_METEO, cached.provider)
        assertEquals(nowMillis, cached.fetchedAtMillis)
    }

    // --- Stale cached return when both providers fail ---

    @Test
    fun bothProvidersFailReturnsLastCachedDataAsStaleWithoutClearing() = runBlocking {
        val cachedFetchedAtMillis = nowMillis - 3_600_000L
        val cachedWeather = normalizedWeather(cachedFetchedAtMillis, temperatureCelsius = 12.0)
        val cache = InMemoryWeatherCache()
        cache.write(
            weatherCacheKey(coordinates),
            WeatherCacheEntry(
                weather = cachedWeather,
                provider = WeatherProviderId.MET_NORWAY,
                fetchedAtMillis = cachedFetchedAtMillis,
            ),
        )
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.TRANSPORT),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.SERVER),
        )
        val repository = repository(primary, fallback, cache)

        val result = repository.load(
            request(),
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertEquals(cachedWeather, result.data)
        assertEquals(WeatherProviderId.MET_NORWAY, result.sourceProvider)
        assertTrue(result.stale)
        assertTrue(result.usedFallback)
        assertEquals(cachedFetchedAtMillis, result.fetchedAtMillis)
        val failure = result.failure
        assertNotNull("both providers failed, so failure metadata must be present", failure)
        assertEquals(
            listOf(
                WeatherProviderFailure(WeatherProviderId.OPEN_METEO, WeatherFailureReason.TRANSPORT),
                WeatherProviderFailure(WeatherProviderId.MET_NORWAY, WeatherFailureReason.SERVER),
            ),
            failure!!.failures,
        )
        assertEquals(
            "cached data must survive a failed load",
            cachedWeather,
            cache.entries[weatherCacheKey(coordinates)]!!.weather,
        )
    }

    // --- Total failure with no cache ---

    @Test
    fun bothProvidersFailWithEmptyCacheReturnsFailureWithoutData() = runBlocking {
        val primary = FakeWeatherProvider(
            WeatherProviderId.OPEN_METEO,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.PARSE),
        )
        val fallback = FakeWeatherProvider(
            WeatherProviderId.MET_NORWAY,
            fullCapabilities(),
            WeatherProviderResult.Failure(WeatherFailureReason.CAPABILITY),
        )
        val cache = InMemoryWeatherCache()
        val repository = repository(primary, fallback, cache)

        val result = repository.load(
            request(),
            WeatherProviderId.OPEN_METEO,
            WeatherProviderId.MET_NORWAY,
            nowMillis,
        )

        assertNull(result.data)
        assertNull(result.sourceProvider)
        assertFalse(result.stale)
        assertNull(result.fetchedAtMillis)
        val failure: WeatherLoadFailure? = result.failure
        assertNotNull(failure)
        assertEquals(
            listOf(
                WeatherProviderFailure(WeatherProviderId.OPEN_METEO, WeatherFailureReason.PARSE),
                WeatherProviderFailure(WeatherProviderId.MET_NORWAY, WeatherFailureReason.CAPABILITY),
            ),
            failure!!.failures,
        )
        assertTrue(cache.entries.isEmpty())
    }
}
