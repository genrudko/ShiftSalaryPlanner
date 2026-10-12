package com.vigilante.shiftsalaryplanner.weather

import com.vigilante.shiftsalaryplanner.ui.today.TimelineItem
import com.vigilante.shiftsalaryplanner.ui.today.TimelineKind
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSeverity
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSource
import kotlin.math.abs

/** Rules are user-owned, stable-ID configuration; no provider-specific data or side effects. */
enum class WeatherRuleCondition { AT_LEAST, AT_MOST, ABSOLUTE_CHANGE_AT_LEAST }
enum class WeatherRuleSeverity { LOW, NORMAL, HIGH, CRITICAL }
enum class WeatherRefreshMode(val intervalMillis: Long) {
    OFF(0L), ON_OPEN(0L), AUTO(30 * 60_000L),
    MINUTES_15(15 * 60_000L), MINUTES_30(30 * 60_000L),
    HOURS_1(60 * 60_000L), HOURS_2(2 * 60 * 60_000L);
}
data class WeatherRefreshPolicy(
    val mode: WeatherRefreshMode = WeatherRefreshMode.ON_OPEN,
    val backgroundEnabled: Boolean = false,
    val wifiOnly: Boolean = false,
    val allowRoaming: Boolean = true,
)
data class WeatherRule(
    val ruleId: String,
    val metric: WeatherMetric,
    val condition: WeatherRuleCondition,
    val threshold: Double,
    val windowMillis: Long = 0L,
    val severity: WeatherRuleSeverity = WeatherRuleSeverity.HIGH,
    val timelineEnabled: Boolean = true,
    val weatherHighlightEnabled: Boolean = true,
    val notificationEnabled: Boolean = false,
    val enabled: Boolean = true,
) {
    fun isValid(): Boolean = ruleId.isNotBlank() && threshold.isFinite() &&
        threshold >= 0.0 && windowMillis in 0L..(24 * 60 * 60_000L)
}

/** A missing override inherits global. DISABLED suppresses, REPLACE changes, ADD introduces. */
sealed interface WeatherRuleOverride {
    data object Disabled : WeatherRuleOverride
    data class Replacement(val rule: WeatherRule) : WeatherRuleOverride
}
data class WeatherRulesConfig(
    val globalRules: List<WeatherRule> = emptyList(),
    val locationOverrides: Map<String, Map<String, WeatherRuleOverride>> = emptyMap(),
    val refreshPolicy: WeatherRefreshPolicy = WeatherRefreshPolicy(),
)

fun resolveWeatherRules(config: WeatherRulesConfig, locationKey: String?): List<WeatherRule> {
    val overrides = locationKey?.let(config.locationOverrides::get).orEmpty()
    val base = config.globalRules.associateBy { it.ruleId }.toMutableMap()
    overrides.forEach { (id, override) ->
        when (override) {
            WeatherRuleOverride.Disabled -> base.remove(id)
            is WeatherRuleOverride.Replacement -> base[id] = override.rule.copy(ruleId = id)
        }
    }
    return base.values.filter { it.enabled && it.isValid() }.sortedBy { it.ruleId }
}
fun weatherRuleLocationKey(source: WeatherLocationSource): String = when (source) {
    is WeatherLocationSource.Workplace -> "workplace:${source.workplaceId}"
    is WeatherLocationSource.Saved -> "saved:${source.savedLocationId}"
    WeatherLocationSource.CurrentDevice -> "current"
}

/** Weather events are semantic threshold transitions, never one item per forecast sample. */
data class WeatherRuleTransition(
    val ruleId: String,
    val eventAtMillis: Long,
    val active: Boolean,
    val severity: WeatherRuleSeverity,
    val metric: WeatherMetric,
    val observedValue: Double,
    val locationKey: String,
    val notificationEnabled: Boolean,
    val timelineEnabled: Boolean,
    val weatherHighlightEnabled: Boolean,
)

private fun metricValue(point: WeatherHourlyPoint, metric: WeatherMetric): Double? = when (metric) {
    WeatherMetric.TEMPERATURE -> point.temperatureCelsius
    WeatherMetric.FEELS_LIKE -> point.feelsLikeCelsius
    WeatherMetric.HUMIDITY -> point.relativeHumidityPercent
    WeatherMetric.PRESSURE -> point.pressureHpa
    WeatherMetric.WIND_SPEED -> point.windSpeedMs
    WeatherMetric.WIND_GUST -> point.windGustMs
    WeatherMetric.WIND_DIRECTION -> point.windDirectionDegrees
    WeatherMetric.PRECIPITATION -> point.precipitationMm
    WeatherMetric.PRECIPITATION_PROBABILITY -> point.precipitationProbabilityPercent
    WeatherMetric.VISIBILITY -> point.visibilityMetres
    WeatherMetric.UV_INDEX -> null
    WeatherMetric.CONDITION -> null
}
private fun isCrossed(value: Double, rule: WeatherRule, previous: Double?): Boolean = when (rule.condition) {
    WeatherRuleCondition.AT_LEAST -> value >= rule.threshold
    WeatherRuleCondition.AT_MOST -> value <= rule.threshold
    WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST ->
        previous != null && abs(value - previous) >= rule.threshold
}
fun evaluateWeatherRuleTransitions(
    weather: NormalizedWeather, rules: List<WeatherRule>, locationKey: String,
): List<WeatherRuleTransition> = buildList {
    val hours = weather.hourly.sortedBy { it.forecastTimeMillis }
        .distinctBy { it.forecastTimeMillis }
    rules.filter { it.isValid() && it.enabled }.forEach { rule ->
        var active = false
        var startedAt: Long? = null
        var lastTime: Long? = null
        hours.forEachIndexed { i, point ->
            val time = point.forecastTimeMillis
            // Breaks above 1.5 h cannot prove continuity across a duration window.
            if (lastTime != null && time - lastTime!! > 90 * 60_000L) {
                active = false
                startedAt = null
            }
            lastTime = time
            val value = metricValue(point, rule.metric)
            // For change rules use the latest sample at or before the window anchor;
            // never treat an unknown earlier value as zero.
            val previous = if (rule.condition == WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST) {
                val anchor = time - rule.windowMillis
                hours.take(i).lastOrNull { it.forecastTimeMillis <= anchor }
                    ?.let { metricValue(it, rule.metric) }
            } else null
            val crossed = value != null && isCrossed(value, rule, previous)
            if (!crossed) {
                if (active && value != null) add(WeatherRuleTransition(rule.ruleId, time, false,
                    rule.severity, rule.metric, value, locationKey, rule.notificationEnabled,
                    rule.timelineEnabled, rule.weatherHighlightEnabled))
                active = false
                startedAt = null
            } else {
                if (startedAt == null) startedAt = time
                val matured = time - startedAt!! >= if (rule.condition == WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST) 0 else rule.windowMillis
                if (!active && matured) {
                    active = true
                    add(WeatherRuleTransition(rule.ruleId, time, true, rule.severity,
                        rule.metric, value!!, locationKey, rule.notificationEnabled,
                        rule.timelineEnabled, rule.weatherHighlightEnabled))
                }
            }
        }
    }
}.sortedWith(compareBy({ it.eventAtMillis }, { it.ruleId }, { it.active }))

fun weatherTransitionsToTimeline(transitions: List<WeatherRuleTransition>): List<TimelineItem> =
    transitions.filter { it.timelineEnabled }.map { event ->
        TimelineItem(
            stableId = "weather:${event.locationKey}:${event.ruleId}:${event.eventAtMillis}:${event.active}",
            source = TimelineSource.WEATHER, kind = TimelineKind.WEATHER_EVENT,
            startMillis = event.eventAtMillis,
            title = if (event.active) "Погодный порог достигнут" else "Погодное ограничение снято",
            subtitle = "${event.ruleId} · ${event.observedValue}",
            severity = when (event.severity) {
                WeatherRuleSeverity.LOW -> TimelineSeverity.LOW
                WeatherRuleSeverity.NORMAL -> TimelineSeverity.NORMAL
                WeatherRuleSeverity.HIGH -> TimelineSeverity.HIGH
                WeatherRuleSeverity.CRITICAL -> TimelineSeverity.CRITICAL
            }, sourceLabel = "Погода"
        )
    }

/** One saved location explicitly eligible for background refresh or alert evaluation. */
data class WeatherBackgroundTarget(val key: String, val coordinates: WeatherCoordinates)

/**
 * Refresh the default workplace/saved location and extra places with opt-in
 * notifications. No temporary GPS points can enter the background queue.
 * The limit bounds network and battery consumption on profiles with many saved places.
 */
fun weatherBackgroundTargets(
    workplaces: Map<String, com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation>,
    saved: List<com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation>,
    rules: WeatherRulesConfig,
    maxLocations: Int = 12,
): List<WeatherBackgroundTarget> {
    if (maxLocations <= 0) return emptyList()
    val candidates = buildList {
        workplaces.toSortedMap(compareBy<String> { it != com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID }
            .thenBy { it }).forEach { (id, item) ->
            add(WeatherBackgroundTarget("workplace:$id", item.coordinates))
        }
        saved.sortedWith(compareBy({ it.order }, { it.id })).forEach { item ->
            add(WeatherBackgroundTarget("saved:${item.id}", item.coordinates))
        }
    }
    if (candidates.isEmpty()) return emptyList()
    val default = candidates.first()
    val extras = candidates.drop(1).filter { target ->
        resolveWeatherRules(rules, target.key).any { it.notificationEnabled }
    }
    return (listOf(default) + extras).take(maxLocations)
}

/** Stateful notification qualification across irregular background jobs. */
data class WeatherRuleLiveState(
    val crossingSinceMillis: Long? = null,
    val notified: Boolean = false,
    val initialized: Boolean = false,
)
data class WeatherRuleLiveResult(val state: WeatherRuleLiveState, val trigger: Boolean)

/**
 * Qualifies a live transition without issuing notifications. Initial observations
 * are a baseline, not an alert; a sustained rule must remain crossed until the
 * configured duration, and an existing alert requires recovery before rearming.
 */
fun evaluateLiveWeatherRule(
    rule: WeatherRule,
    value: Double?,
    nowMillis: Long,
    lastValueAtWindow: Double?,
    previous: WeatherRuleLiveState,
): WeatherRuleLiveResult {
    if (!rule.enabled || !rule.isValid() || value == null || !value.isFinite()) {
        return WeatherRuleLiveResult(previous.copy(crossingSinceMillis = null), false)
    }
    val crossed = isCrossed(value, rule, lastValueAtWindow)
    if (!previous.initialized) {
        return WeatherRuleLiveResult(
            WeatherRuleLiveState(if (crossed) nowMillis else null,
                notified = crossed, initialized = true), false,
        )
    }
    if (!crossed) return WeatherRuleLiveResult(WeatherRuleLiveState(initialized = true), false)
    val since = previous.crossingSinceMillis ?: nowMillis
    val neededDuration = if (rule.condition == WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST) 0L
        else rule.windowMillis
    val enoughTime = nowMillis >= since && nowMillis - since >= neededDuration
    val fire = !previous.notified && enoughTime
    return WeatherRuleLiveResult(
        WeatherRuleLiveState(since, notified = previous.notified || fire, initialized = true),
        fire,
    )
}
