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
        val place = workplaces.values.sortedBy { it.workplaceId }.firstOrNull()
        val other = saved.sortedWith(compareBy({ it.order }, { it.id })).firstOrNull()
        val coordinates = place?.coordinates ?: other?.coordinates ?: return
        val key = if (place != null) "workplace:${place.workplaceId}" else "saved:${other!!.id}"
        val settings = WeatherSettingsStore(this).snapshot()
        val repository = WeatherRepositoryImpl(
            mapOf(WeatherProviderId.OPEN_METEO to OpenMeteoWeatherProvider(),
                WeatherProviderId.MET_NORWAY to MetNorwayWeatherProvider()),
            WeatherCacheStore(this),
        )
        val result = repository.load(
            WeatherFetchRequest(coordinates, hourlyHours = settings.presentationConfig.hourlyHorizon.hours,
                dailyDays = settings.presentationConfig.dailyHorizon.days,
                requiredMetrics = emptySet(), optionalMetrics = WeatherMetric.values().toSet()),
            settings.primaryProviderId, settings.fallbackProviderId, System.currentTimeMillis(),
        )
        // A stale cache or a missing live current reading cannot trigger a new alert.
        val current = result.data?.current ?: return
        if (result.stale) return
        val rules = resolveWeatherRules(rulesConfig, key)
        val states = profileSharedPreferences("weather_rule_notification_state")
        rules.filter { it.notificationEnabled }.forEach { rule ->
            val value = when (rule.metric) {
                WeatherMetric.WIND_SPEED -> current.windSpeedMs
                WeatherMetric.WIND_GUST -> current.windGustMs
                WeatherMetric.PRECIPITATION -> current.precipitationMm
                WeatherMetric.PRESSURE -> current.pressureHpa
                else -> null
            } ?: return@forEach
            if (rule.condition == WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST) return@forEach
            val active = when (rule.condition) {
                WeatherRuleCondition.AT_LEAST -> value >= rule.threshold
                WeatherRuleCondition.AT_MOST -> value <= rule.threshold
                else -> false
            }
            val stateKey = "${key}:${rule.ruleId}"
            val previous = states.getString(stateKey, null)
            states.edit().putString(stateKey, active.toString()).apply()
            if (previous == "false" && active && rule.windowMillis == 0L) {
                sendWeatherRuleNotification(rule, value)
            }
        }
    }

    private fun sendWeatherRuleNotification(rule: WeatherRule, value: Double) {
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
        manager.notify(rule.ruleId.hashCode(), notification)
    }
}
