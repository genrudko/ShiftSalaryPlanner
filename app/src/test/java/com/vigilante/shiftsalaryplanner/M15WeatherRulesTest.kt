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

    @Test fun notificationsDoNotFireOnFirstObservationAndRespectSustainedWindow() {
        val rule = wind.copy(notificationEnabled = true, windowMillis = 60_000L)
        val first = evaluateLiveWeatherRule(rule, 14.0, 1_000L, null, WeatherRuleLiveState())
        assertFalse(first.trigger)
        val recovered = evaluateLiveWeatherRule(rule, 8.0, 2_000L, null, first.state)
        val crossed = evaluateLiveWeatherRule(rule, 14.0, 3_000L, null, recovered.state)
        assertFalse(crossed.trigger)
        val sustained = evaluateLiveWeatherRule(rule, 15.0, 64_000L, null, crossed.state)
        assertTrue(sustained.trigger)
        assertFalse(evaluateLiveWeatherRule(rule, 16.0, 125_000L, null, sustained.state).trigger)
        val reset = evaluateLiveWeatherRule(rule, 8.0, 126_000L, null, sustained.state)
        assertFalse(reset.state.notified)
    }

    @Test fun absolutePressureChangeNeedsPreviousSampleAndIsDeduplicated() {
        val rule = wind.copy(
            metric=WeatherMetric.PRESSURE,
            condition=WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST,
            threshold=5.0,windowMillis=60_000L,notificationEnabled=true
        )
        val baseline = evaluateLiveWeatherRule(rule, 1010.0, 1_000L, null, WeatherRuleLiveState())
        assertFalse(baseline.trigger)
        val trigger = evaluateLiveWeatherRule(rule, 1002.0, 61_000L, 1010.0, baseline.state)
        assertTrue(trigger.trigger)
        assertFalse(evaluateLiveWeatherRule(rule, 1001.0, 62_000L, 1010.0, trigger.state).trigger)
    }

    @Test fun backgroundTargetsPreferMainWorkplaceAndIncludeOptInAlertsOnly() {
        val workplaces = mapOf(
            "work_extra" to com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation(
                "work_extra", WeatherCoordinates(46.0,42.0),"Extra"),
            "work_main" to com.vigilante.shiftsalaryplanner.settings.WorkplaceWeatherLocation(
                "work_main", WeatherCoordinates(44.0,41.0),"Main")
        )
        val saved = listOf(com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation(
            "home","Home",WeatherCoordinates(45.0,41.0),order=0))
        val rules = WeatherRulesConfig(
            locationOverrides = mapOf("saved:home" to mapOf(
                "alert" to WeatherRuleOverride.Replacement(
                    wind.copy(ruleId="alert",notificationEnabled=true)))))
        assertEquals(listOf("workplace:work_main","saved:home"),
            weatherBackgroundTargets(workplaces,saved,rules).map { it.key })
        assertEquals(1,weatherBackgroundTargets(workplaces,saved,rules,maxLocations=1).size)
        assertTrue(weatherBackgroundTargets(workplaces,saved,rules,maxLocations=0).isEmpty())
        assertFalse(weatherBackgroundTargets(emptyMap(),emptyList(),rules)
            .any { it.key == "current" })
    }

}
