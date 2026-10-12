package com.vigilante.shiftsalaryplanner.weather

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.vigilante.shiftsalaryplanner.R
import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocationStore
import com.vigilante.shiftsalaryplanner.settings.WeatherCacheStore
import com.vigilante.shiftsalaryplanner.settings.WeatherRulesStore
import com.vigilante.shiftsalaryplanner.settings.WeatherSettingsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocationStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val WEATHER_JOB_ID = 151503
private const val WEATHER_NOTICE_CHANNEL = "weather_rules"
const val MIN_WEATHER_BACKGROUND_INTERVAL_MILLIS = 15 * 60_000L

/** Android JobScheduler is inexact and may defer work; never use alarm scheduling for weather. */
fun synchronizeWeatherRefreshSchedule(context: Context, policy: WeatherRefreshPolicy) {
    val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
    val interval = policy.mode.intervalMillis
    if (!policy.backgroundEnabled || interval <= 0L || policy.mode == WeatherRefreshMode.OFF ||
        policy.mode == WeatherRefreshMode.ON_OPEN) {
        scheduler.cancel(WEATHER_JOB_ID)
        return
    }
    val networkType = if (policy.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY
    val effectiveInterval = interval.coerceAtLeast(MIN_WEATHER_BACKGROUND_INTERVAL_MILLIS)
    val job = JobInfo.Builder(WEATHER_JOB_ID, ComponentName(context, WeatherRefreshJobService::class.java))
        .setRequiredNetworkType(networkType)
        .setPersisted(true)
        .setPeriodic(effectiveInterval)
        .build()
    // Do not replace an equivalent persisted job on every Compose recomposition.
    if (scheduler.allPendingJobs.any {
        it.id == WEATHER_JOB_ID && it.intervalMillis == effectiveInterval &&
            it.networkType == networkType
    }) return
    scheduler.schedule(job)
}

/** Fetches only previously saved coordinates; never requests background device location. */
class WeatherRefreshJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            try { refreshForSavedLocation() }
            catch (_: CancellationException) { }
            catch (_: Exception) { /* Platform job retry decisions are intentionally conservative */ }
            finally { jobFinished(params, false) }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return false // next periodic execution is managed by JobScheduler
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private suspend fun refreshForSavedLocation() {
        val rulesConfig = WeatherRulesStore(this).snapshot()
        val policy = rulesConfig.refreshPolicy
        if (!policy.backgroundEnabled || policy.mode.intervalMillis <= 0) return
        if (!policy.allowRoaming) {
            val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = connectivity?.activeNetwork
            val caps = network?.let { connectivity.getNetworkCapabilities(it) }
            if (caps == null || !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING))
                return
        }
        val workplaces = WorkplaceWeatherLocationStore(this).locationsFlow.first()
        val saved = SavedWeatherLocationStore(this).savedLocationsFlow.first()
        val targets = weatherBackgroundTargets(workplaces, saved, rulesConfig)
        if (targets.isEmpty()) return
        val settings = WeatherSettingsStore(this).snapshot()
        val repository = WeatherRepositoryImpl(
            mapOf(WeatherProviderId.OPEN_METEO to OpenMeteoWeatherProvider(),
                WeatherProviderId.MET_NORWAY to MetNorwayWeatherProvider()),
            WeatherCacheStore(this),
        )
        // One fetch for each coordinate, even if multiple saved places use it.
        // Only the default place and additional notification opt-ins are refreshed.
        targets.groupBy { it.coordinates }.forEach { (coordinates, places) ->
            val result = repository.load(
                WeatherFetchRequest(coordinates,
                    hourlyHours = settings.presentationConfig.hourlyHorizon.hours,
                    dailyDays = settings.presentationConfig.dailyHorizon.days,
                    requiredMetrics = emptySet(),
                    optionalMetrics = WeatherMetric.values().toSet()),
                settings.primaryProviderId, settings.fallbackProviderId, System.currentTimeMillis(),
            )
            // Never derive a new weather notification from a stale cache entry.
            val current = result.data?.current ?: return@forEach
            if (result.stale) return@forEach
            val now = System.currentTimeMillis()
            places.forEach { place ->
                processWeatherAlerts(current, place.key, rulesConfig, now)
            }
        }
    }

    private fun processWeatherAlerts(
        current: WeatherCurrent,
        key: String,
        rulesConfig: WeatherRulesConfig,
        now: Long,
    ) {
        val states = profileSharedPreferences("weather_rule_notification_state")
        resolveWeatherRules(rulesConfig, key).filter { it.notificationEnabled }.forEach { rule ->
            val value = when (rule.metric) {
                WeatherMetric.WIND_SPEED -> current.windSpeedMs
                WeatherMetric.WIND_GUST -> current.windGustMs
                WeatherMetric.PRECIPITATION -> current.precipitationMm
                WeatherMetric.PRESSURE -> current.pressureHpa
                else -> null
            } ?: return@forEach
            val stateKey = "${key}:${rule.ruleId}"
            val rawHistory = states.getString("$stateKey:history", "").orEmpty()
            val observations = rawHistory.split(';').mapNotNull { entry ->
                val pair = entry.split(':')
                if (pair.size != 2) null else {
                    val timestamp = pair[0].toLongOrNull()
                    val number = pair[1].toDoubleOrNull()
                    if (timestamp != null && number != null && number.isFinite()) timestamp to number else null
                }
            }.filter { it.first >= now - 25 * 3_600_000L && it.first <= now }
            val prior = observations.lastOrNull { it.first <= now - rule.windowMillis }?.second
            val prevState = WeatherRuleLiveState(
                crossingSinceMillis = states.getLong("$stateKey:since", -1L).takeIf { it >= 0 },
                notified = states.getBoolean("$stateKey:notified", false),
                initialized = states.getBoolean("$stateKey:initialized", false),
            )
            val evaluated = evaluateLiveWeatherRule(rule, value, now, prior, prevState)
            val history = (observations + (now to value)).takeLast(100)
                .joinToString(";") { "${it.first}:${it.second}" }
            states.edit()
                .putString("$stateKey:history", history)
                .putLong("$stateKey:since", evaluated.state.crossingSinceMillis ?: -1L)
                .putBoolean("$stateKey:notified", evaluated.state.notified)
                .putBoolean("$stateKey:initialized", evaluated.state.initialized)
                .apply()
            if (evaluated.trigger) sendWeatherRuleNotification(stateKey, rule, value)
        }
    }

    private fun sendWeatherRuleNotification(stateKey: String, rule: WeatherRule, value: Double) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(WEATHER_NOTICE_CHANNEL) == null) {
            manager.createNotificationChannel(NotificationChannel(
                WEATHER_NOTICE_CHANNEL, "Погодные предупреждения", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = Notification.Builder(this, WEATHER_NOTICE_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Погодный порог достигнут")
            .setContentText("${rule.ruleId}: ${value} (порог ${rule.threshold})")
            .setAutoCancel(true).build()
        manager.notify(stateKey, 0, notification)
    }
}
