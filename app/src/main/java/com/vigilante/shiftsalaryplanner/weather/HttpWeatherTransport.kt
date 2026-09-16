package com.vigilante.shiftsalaryplanner.weather

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bounded HTTPS transport for the M15.2 weather adapters.
 *
 * Contract (M15.2 Task 3):
 * - HTTPS only; non-https URLs are rejected before any connection is opened;
 * - bounded [connectTimeoutMillis] and [readTimeoutMillis];
 * - HTTP status is validated (2xx required, everything else is a server failure);
 * - the response body is always read as UTF-8;
 * - the connection is always disconnected.
 *
 * Errors are normalized into [HttpWeatherException] so provider adapters can map
 * them to the canonical [WeatherFailureReason] vocabulary (TRANSPORT / SERVER)
 * without knowing transport internals.
 */
class HttpWeatherTransport(
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
) {

    /**
     * Executes a HTTPS GET and returns the UTF-8 response body.
     * [headers] are applied verbatim; [MET_NORWAY_USER_AGENT]-style app
     * identification is the caller's responsibility.
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) { getBlocking(url, headers) }

    private fun getBlocking(url: String, headers: Map<String, String>): String {
        require(url.startsWith("https://")) { "HttpWeatherTransport only supports HTTPS, got [$url]" }
        var connection: HttpURLConnection? = null
        return try {
            val http = URL(url).openConnection() as HttpURLConnection
            connection = http
            http.requestMethod = "GET"
            http.connectTimeout = connectTimeoutMillis
            http.readTimeout = readTimeoutMillis
            http.setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> http.setRequestProperty(name, value) }

            val status = http.responseCode
            if (status !in 200..299) {
                throw HttpWeatherException(
                    kind = HttpWeatherFailureKind.HTTP_STATUS,
                    message = "Weather server returned HTTP $status for $url",
                )
            }
            http.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: HttpWeatherException) {
            throw e
        } catch (e: Exception) {
            throw HttpWeatherException(
                kind = HttpWeatherFailureKind.TRANSPORT,
                message = "Weather transport failure for $url",
                cause = e,
            )
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 10_000
    }
}

/** Why the transport could not deliver a response. */
enum class HttpWeatherFailureKind { TRANSPORT, HTTP_STATUS }

/** Transport-level failure carrying the reason needed for canonical mapping. */
class HttpWeatherException(
    val kind: HttpWeatherFailureKind,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * App-identifying User-Agent for every M15 weather HTTP request.
 *
 * MET Norway's terms require a non-generic application User-Agent; this value is
 * the single shared contract and never exposes generic client identifiers such as
 * `okhttp`, `Dalvik`, `Java/` or `curl/`.
 */
const val WEATHER_HTTP_USER_AGENT = "ShiftSalaryPlanner/7.1 (+https://github.com/genrudko/ShiftSalaryPlanner)"
