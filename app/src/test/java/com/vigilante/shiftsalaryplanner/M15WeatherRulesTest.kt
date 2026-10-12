package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.settings.decodeWeatherRulesConfig
import com.vigilante.shiftsalaryplanner.settings.encodeWeatherRulesConfig
import com.vigilante.shiftsalaryplanner.weather.*
import org.junit.Assert.*
import org.junit.Test

class M15WeatherRulesTest {
    private val wind = WeatherRule("wind-high", WeatherMetric.WIND_SPEED,
        WeatherRuleCondition.AT_LEAST, 12.0, severity = WeatherRuleSeverity.HIGH)
    private val config = WeatherRulesConfig(globalRules = listOf(wind))

    @Test fun defaultPolicyNeverSchedulesBackgroundOrEnablesNotifications() {
        assertFalse(WeatherRefreshPolicy().backgroundEnabled)
        assertEquals(WeatherRefreshMode.ON_OPEN, WeatherRefreshPolicy().mode)
        assertFalse(wind.notificationEnabled)
    }
    @Test fun stableOverridesDisableReplaceAndAddWithoutMutatingGlobals() {
        val gust = wind.copy(ruleId = "gust", metric = WeatherMetric.WIND_GUST, threshold = 18.0)
        val customized = config.copy(locationOverrides = mapOf(
            "workplace:A" to mapOf("wind-high" to WeatherRuleOverride.Disabled,
                "gust" to WeatherRuleOverride.Replacement(gust))
        ))
        assertEquals(listOf("gust"), resolveWeatherRules(customized, "workplace:A").map { it.ruleId })
        assertEquals(wind, resolveWeatherRules(customized, "workplace:B").single())
        assertEquals(wind, config.globalRules.single())
    }
    @Test fun codecPersistsLocationOverridesAndOptInWithoutImplicitEnable() {
        val settings = config.copy(
            locationOverrides = mapOf("saved:park" to mapOf(
                "wind-high" to WeatherRuleOverride.Replacement(wind.copy(notificationEnabled = true))
            )),
            refreshPolicy = WeatherRefreshPolicy(WeatherRefreshMode.MINUTES_30, true, true, false)
        )
        assertEquals(settings, decodeWeatherRulesConfig(encodeWeatherRulesConfig(settings)))
        assertEquals(WeatherRulesConfig(), decodeWeatherRulesConfig("{ invalid"))
    }
    @Test fun transitionEventsOnlyAtStartAndEndNotEveryHour() {
        val weather = NormalizedWeather(
            coordinates = WeatherCoordinates(44.6,41.8), fetchedAtMillis = 0,
            hourly = listOf(8.0, 13.0, 14.0, 15.0, 7.0).mapIndexed { i, speed ->
                WeatherHourlyPoint(i * 3_600_000L, 19.0, windSpeedMs = speed)
            })
        val events = evaluateWeatherRuleTransitions(weather, listOf(wind), "workplace:A")
        assertEquals(2, events.size)
        assertEquals(listOf(true,false), events.map { it.active })
        assertEquals(3_600_000L, events[0].eventAtMillis)
        assertEquals(4*3_600_000L, events[1].eventAtMillis)
        val timeline = weatherTransitionsToTimeline(events)
        assertEquals(2, timeline.size)
        assertTrue(timeline.all { it.stableId.startsWith("weather:workplace:A:wind-high:") })
    }
    @Test fun durationRequiresContinuousObservationsAndChangeRequiresHistory() {
        val rule = wind.copy(windowMillis = 2*3_600_000L)
        val weather = NormalizedWeather(WeatherCoordinates(44.6,41.8),0,
            hourly = listOf(13.0,14.0,15.0).mapIndexed { i,speed ->
                WeatherHourlyPoint(i*3_600_000L, 20.0, windSpeedMs=speed)
            })
        assertEquals(1, evaluateWeatherRuleTransitions(weather,listOf(rule),"saved:A").size)
        val pressureChange = rule.copy(metric=WeatherMetric.PRESSURE,
            condition=WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST,threshold=5.0)
        assertTrue(evaluateWeatherRuleTransitions(weather,listOf(pressureChange),"saved:A").isEmpty())
    }
}
