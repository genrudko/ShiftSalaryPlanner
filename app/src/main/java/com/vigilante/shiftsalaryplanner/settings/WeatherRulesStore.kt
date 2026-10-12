package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.weather.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private fun WeatherRule.toJson(): JSONObject = JSONObject()
    .put("ruleId", ruleId).put("metric", metric.name).put("condition", condition.name)
    .put("threshold", threshold).put("windowMillis", windowMillis)
    .put("severity", severity.name).put("timelineEnabled", timelineEnabled)
    .put("weatherHighlightEnabled", weatherHighlightEnabled)
    .put("notificationEnabled", notificationEnabled).put("enabled", enabled)

private fun JSONObject.toWeatherRule(): WeatherRule? = runCatching {
    WeatherRule(
        ruleId = getString("ruleId"),
        metric = WeatherMetric.valueOf(getString("metric")),
        condition = WeatherRuleCondition.valueOf(getString("condition")),
        threshold = getDouble("threshold"),
        windowMillis = optLong("windowMillis", 0L),
        severity = WeatherRuleSeverity.valueOf(optString("severity", WeatherRuleSeverity.HIGH.name)),
        timelineEnabled = optBoolean("timelineEnabled", true),
        weatherHighlightEnabled = optBoolean("weatherHighlightEnabled", true),
        notificationEnabled = optBoolean("notificationEnabled", false),
        enabled = optBoolean("enabled", true),
    ).takeIf(WeatherRule::isValid)
}.getOrNull()

fun encodeWeatherRulesConfig(config: WeatherRulesConfig): String {
    val locations = JSONObject()
    config.locationOverrides.toSortedMap().forEach { (location, rules) ->
        val overrides = JSONObject()
        rules.toSortedMap().forEach { (id, override) ->
            overrides.put(id, when (override) {
                WeatherRuleOverride.Disabled -> JSONObject().put("mode", "disabled")
                is WeatherRuleOverride.Replacement -> JSONObject().put("mode", "replace")
                    .put("rule", override.rule.copy(ruleId = id).toJson())
            })
        }
        locations.put(location, overrides)
    }
    return JSONObject()
        .put("schemaVersion", 1)
        .put("globalRules", JSONArray().apply {
            config.globalRules.filter(WeatherRule::isValid).forEach { put(it.toJson()) }
        })
        .put("locationOverrides", locations)
        .put("refreshPolicy", JSONObject()
            .put("mode", config.refreshPolicy.mode.name)
            .put("backgroundEnabled", config.refreshPolicy.backgroundEnabled)
            .put("wifiOnly", config.refreshPolicy.wifiOnly)
            .put("allowRoaming", config.refreshPolicy.allowRoaming))
        .toString()
}
fun decodeWeatherRulesConfig(raw: String): WeatherRulesConfig = runCatching {
    val doc = JSONObject(raw)
    val globalArray = doc.optJSONArray("globalRules") ?: JSONArray()
    val rules = (0 until globalArray.length()).mapNotNull { globalArray.optJSONObject(it)?.toWeatherRule() }
        .distinctBy { it.ruleId }
    val locationObj = doc.optJSONObject("locationOverrides") ?: JSONObject()
    val locationRules = buildMap {
        locationObj.keys().forEach { key ->
            val entries = locationObj.optJSONObject(key) ?: return@forEach
            val overrides = buildMap {
                entries.keys().forEach { id ->
                    val node = entries.optJSONObject(id) ?: return@forEach
                    val value = when (node.optString("mode")) {
                        "disabled" -> WeatherRuleOverride.Disabled
                        "replace" -> node.optJSONObject("rule")?.toWeatherRule()
                            ?.let { WeatherRuleOverride.Replacement(it.copy(ruleId = id)) }
                        else -> null
                    }
                    if (value != null) put(id, value)
                }
            }
            if (overrides.isNotEmpty()) put(key, overrides)
        }
    }
    val policy = doc.optJSONObject("refreshPolicy")
    WeatherRulesConfig(
        globalRules = rules, locationOverrides = locationRules,
        refreshPolicy = WeatherRefreshPolicy(
            mode = runCatching { WeatherRefreshMode.valueOf(policy?.optString("mode").orEmpty()) }
                .getOrDefault(WeatherRefreshMode.ON_OPEN),
            backgroundEnabled = policy?.optBoolean("backgroundEnabled", false) ?: false,
            wifiOnly = policy?.optBoolean("wifiOnly", false) ?: false,
            allowRoaming = policy?.optBoolean("allowRoaming", true) ?: true,
        ),
    )
}.getOrDefault(WeatherRulesConfig())

class WeatherRulesStore(context: Context) {
    private val prefs = context.profileSharedPreferences("weather_rules")
    private val state = MutableStateFlow(decodeWeatherRulesConfig(prefs.getString("rules_json", "").orEmpty()))
    val configFlow: Flow<WeatherRulesConfig> = state.asStateFlow()
    fun snapshot(): WeatherRulesConfig = state.value
    fun save(config: WeatherRulesConfig) {
        prefs.edit { putString("rules_json", encodeWeatherRulesConfig(config)) }
        state.value = config
    }
}
