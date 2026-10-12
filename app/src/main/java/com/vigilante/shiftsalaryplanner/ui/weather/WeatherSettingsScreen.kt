package com.vigilante.shiftsalaryplanner.ui.weather

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.EvolutionSurface
import com.vigilante.shiftsalaryplanner.EvolutionSurfaceRole
import com.vigilante.shiftsalaryplanner.appBlockSpacing
import com.vigilante.shiftsalaryplanner.appCardPadding
import com.vigilante.shiftsalaryplanner.appCornerRadius
import com.vigilante.shiftsalaryplanner.appScaledSpacing
import com.vigilante.shiftsalaryplanner.appScreenPadding
import com.vigilante.shiftsalaryplanner.settings.SavedWeatherLocation
import com.vigilante.shiftsalaryplanner.settings.WeatherSettings
import com.vigilante.shiftsalaryplanner.weather.DailyHorizon
import com.vigilante.shiftsalaryplanner.weather.HourlyHorizon
import com.vigilante.shiftsalaryplanner.weather.PrecipitationUnit
import com.vigilante.shiftsalaryplanner.weather.PressureUnit
import com.vigilante.shiftsalaryplanner.weather.TemperatureUnit
import com.vigilante.shiftsalaryplanner.weather.VisibilityUnit
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherMetricDisplay
import com.vigilante.shiftsalaryplanner.weather.WeatherPresetId
import com.vigilante.shiftsalaryplanner.weather.WeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.WeatherProviderId
import com.vigilante.shiftsalaryplanner.weather.WindDirectionFormat
import com.vigilante.shiftsalaryplanner.weather.WindSpeedUnit
import com.vigilante.shiftsalaryplanner.weather.basicWeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.workWindWeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.WeatherRulesConfig
import com.vigilante.shiftsalaryplanner.weather.WeatherRule
import com.vigilante.shiftsalaryplanner.weather.WeatherRuleOverride
import com.vigilante.shiftsalaryplanner.weather.WeatherRefreshMode
import com.vigilante.shiftsalaryplanner.weather.WeatherRuleCondition
import com.vigilante.shiftsalaryplanner.weather.WeatherRuleSeverity
import com.vigilante.shiftsalaryplanner.weather.resolveWeatherRules

fun mutateWeatherPresentation(
    settings: WeatherSettings,
    transform: WeatherPresentationConfig.() -> WeatherPresentationConfig,
): WeatherSettings {
    val custom = settings.presentationConfig.toCustom()
    return settings.copy(
        presentationConfig = custom.transform().copy(presetId = WeatherPresetId.CUSTOM),
    )
}

fun selectWeatherPreset(settings: WeatherSettings, presetId: WeatherPresetId): WeatherSettings =
    settings.copy(
        presentationConfig = when (presetId) {
            WeatherPresetId.BASIC -> basicWeatherPresentationConfig()
            WeatherPresetId.WORK_WIND -> workWindWeatherPresentationConfig()
            WeatherPresetId.CUSTOM -> settings.presentationConfig.toCustom()
        },
    )

@Composable
fun WeatherSettingsScreen(
    settings: WeatherSettings,
    savedLocations: List<SavedWeatherLocation>,
    onBack: () -> Unit,
    onSettingsChange: (WeatherSettings) -> Unit,
    onAddSavedLocation: () -> Unit,
    onEditSavedLocation: (String) -> Unit,
    onDeleteSavedLocation: (String) -> Unit,
    onReorderSavedLocations: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    rulesConfig: WeatherRulesConfig = WeatherRulesConfig(),
    onRulesChange: (WeatherRulesConfig) -> Unit = {},
    workplacesWithWeather: List<Pair<String, String>> = emptyList(),
    onRequestNotificationPermission: () -> Unit = {},
    initiallyShowAdvancedRules: Boolean = false,
    initiallyScrollToRules: Boolean = false,
) {
    val presentation = settings.presentationConfig
    var showAdvancedRules by rememberSaveable { mutableStateOf(initiallyShowAdvancedRules) }
    var ruleScope by remember { mutableStateOf<String?>(null) }
    val activeRules = resolveWeatherRules(rulesConfig, ruleScope)
    fun updateRule(rule: WeatherRule) {
        if (ruleScope == null) onRulesChange(rulesConfig.copy(
            globalRules = (rulesConfig.globalRules.filterNot { it.ruleId == rule.ruleId } + rule)
        ))
        else {
            val scope = ruleScope ?: return
            val existing = rulesConfig.locationOverrides[scope].orEmpty()
            onRulesChange(rulesConfig.copy(locationOverrides = rulesConfig.locationOverrides +
                (scope to (existing + (rule.ruleId to WeatherRuleOverride.Replacement(rule))))))
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = appScreenPadding()),
        state = rememberLazyListState(initialFirstVisibleItemIndex = if (initiallyScrollToRules) 6 else 0),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing()),
    ) {
        item("weather-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = appScreenPadding()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("Назад") }
                Text("Погода", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(appScaledSpacing(52.dp)))
            }
            Text(
                "Источники, единицы, состав прогноза и сохранённые места",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item("weather-providers") {
            SettingsSection("Источники") {
                OptionRow(
                    title = "Основной",
                    values = WeatherProviderId.values().toList(),
                    selected = settings.primaryProviderId,
                    label = ::providerLabel,
                    onSelect = { onSettingsChange(settings.copy(primaryProviderId = it)) },
                )
                OptionRow(
                    title = "Резервный",
                    values = WeatherProviderId.values().toList(),
                    selected = settings.fallbackProviderId,
                    label = ::providerLabel,
                    onSelect = { onSettingsChange(settings.copy(fallbackProviderId = it)) },
                )
            }
        }

        item("weather-units") {
            SettingsSection("Единицы измерения") {
                OptionRow("Температура", TemperatureUnit.values().toList(), settings.temperatureUnit, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(temperatureUnit = it))
                }
                OptionRow("Ветер", WindSpeedUnit.values().toList(), settings.windSpeedUnit, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(windSpeedUnit = it))
                }
                OptionRow("Давление", PressureUnit.values().toList(), settings.pressureUnit, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(pressureUnit = it))
                }
                OptionRow("Осадки", PrecipitationUnit.values().toList(), settings.precipitationUnit, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(precipitationUnit = it))
                }
                OptionRow("Видимость", VisibilityUnit.values().toList(), settings.visibilityUnit, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(visibilityUnit = it))
                }
                OptionRow("Направление ветра", WindDirectionFormat.values().toList(), settings.windDirectionFormat, ::weatherUnitLabel) {
                    onSettingsChange(settings.copy(windDirectionFormat = it))
                }
            }
        }

        item("weather-preset") {
            SettingsSection("Состав прогноза") {
                OptionRow("Профиль", WeatherPresetId.values().toList(), presentation.presetId, ::presetLabel) {
                    onSettingsChange(selectWeatherPreset(settings, it))
                }
                Text("Слои", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                WeatherLayerId.values().forEach { layer ->
                    ToggleRow(title = layerLabel(layer), checked = layer in presentation.layers) { enabled ->
                        onSettingsChange(
                            mutateWeatherPresentation(settings) {
                                copy(layers = if (enabled) layers + layer else layers - layer)
                            }
                        )
                    }
                }
                OptionRow(
                    "Почасовой прогноз", HourlyHorizon.values().toList(), presentation.hourlyHorizon,
                    { it.hours.toString() + " ч" },
                ) { horizon ->
                    onSettingsChange(mutateWeatherPresentation(settings) { copy(hourlyHorizon = horizon) })
                }
                OptionRow(
                    "По дням", DailyHorizon.values().toList(), presentation.dailyHorizon,
                    { it.days.toString() + " дн." },
                ) { horizon ->
                    onSettingsChange(mutateWeatherPresentation(settings) { copy(dailyHorizon = horizon) })
                }
            }
        }

        item("weather-metrics") {
            SettingsSection("Показатели") {
                presentation.metrics.sortedBy { it.order }.forEachIndexed { index, display ->
                    MetricRow(
                        display = display,
                        canMoveUp = index > 0,
                        canMoveDown = index < presentation.metrics.lastIndex,
                        onVisibleChange = { visible ->
                            onSettingsChange(mutateWeatherPresentation(settings) {
                                copy(metrics = metrics.map {
                                    if (it.metric == display.metric) it.copy(visible = visible) else it
                                })
                            })
                        },
                        onMoveUp = { onSettingsChange(moveMetric(settings, display.metric, -1)) },
                        onMoveDown = { onSettingsChange(moveMetric(settings, display.metric, 1)) },
                    )
                    if (index < presentation.metrics.lastIndex) HorizontalDivider()
                }
            }
        }

        item("weather-refresh") {
            SettingsSection("Обновление погоды") {
                Text("Обновление в фоне может быть отложено Android; точное время не гарантируется.",
                    style = MaterialTheme.typography.bodySmall)
                OptionRow(
                    title = "Период",
                    values = WeatherRefreshMode.values().toList(),
                    selected = rulesConfig.refreshPolicy.mode,
                    label = { when (it) {
                        WeatherRefreshMode.OFF -> "Выкл."
                        WeatherRefreshMode.ON_OPEN -> "При открытии"
                        WeatherRefreshMode.AUTO -> "Авто"
                        WeatherRefreshMode.MINUTES_15 -> "15 мин"
                        WeatherRefreshMode.MINUTES_30 -> "30 мин"
                        WeatherRefreshMode.HOURS_1 -> "1 час"
                        WeatherRefreshMode.HOURS_2 -> "2 часа"
                    } },
                    onSelect = { onRulesChange(rulesConfig.copy(
                        refreshPolicy = rulesConfig.refreshPolicy.copy(mode = it))) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Фоновое обновление (по желанию)")
                    Switch(
                        checked = rulesConfig.refreshPolicy.backgroundEnabled,
                        onCheckedChange = {
                            onRulesChange(rulesConfig.copy(refreshPolicy =
                                rulesConfig.refreshPolicy.copy(backgroundEnabled = it)))
                        })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Только Wi-Fi")
                    Switch(
                        checked = rulesConfig.refreshPolicy.wifiOnly,
                        onCheckedChange = { onRulesChange(rulesConfig.copy(
                            refreshPolicy = rulesConfig.refreshPolicy.copy(wifiOnly = it))) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Разрешить роуминг")
                    Switch(
                        checked = rulesConfig.refreshPolicy.allowRoaming,
                        onCheckedChange = { onRulesChange(rulesConfig.copy(
                            refreshPolicy = rulesConfig.refreshPolicy.copy(allowRoaming = it))) })
                }
            }
        }
        item("weather-rules") {
            SettingsSection("Погодные правила") {
                Text("Общие правила наследуются местоположениями. Оповещения по умолчанию выключены.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showAdvancedRules = !showAdvancedRules }) {
                    Text(if (showAdvancedRules) "Скрыть редактор правил" else "Настроить погодные правила")
                }
                if (showAdvancedRules) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                    FilterChip(selected = ruleScope == null, onClick = { ruleScope = null },
                        label = { Text("Общие") })
                    workplacesWithWeather.forEach { (id, name) ->
                        val key = "workplace:$id"
                        FilterChip(selected = ruleScope == key, onClick = { ruleScope = key },
                            label = { Text(name) })
                    }
                    savedLocations.forEach { location ->
                        val key = "saved:${location.id}"
                        FilterChip(selected = ruleScope == key, onClick = { ruleScope = key },
                            label = { Text(location.name) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
                    listOf(
                        Triple("Ветер", WeatherMetric.WIND_SPEED, 12.0),
                        Triple("Порывы", WeatherMetric.WIND_GUST, 18.0),
                        Triple("Осадки", WeatherMetric.PRECIPITATION, 5.0),
                        Triple("Давление", WeatherMetric.PRESSURE, 990.0),
                    ).forEach { (name, metric, threshold) ->
                        TextButton(onClick = {
                            val id = "rule_${System.currentTimeMillis()}_${metric.name}"
                            updateRule(WeatherRule(id, metric, WeatherRuleCondition.AT_LEAST,
                                threshold, severity = WeatherRuleSeverity.HIGH))
                        }) { Text("+ $name") }
                    }
                }
                if (ruleScope != null && rulesConfig.locationOverrides[ruleScope].orEmpty().isNotEmpty()) {
                    TextButton(onClick = {
                        val scope = ruleScope ?: return@TextButton
                        onRulesChange(rulesConfig.copy(locationOverrides = rulesConfig.locationOverrides - scope))
                    }) { Text("Сбросить локальные переопределения") }
                }
                activeRules.forEach { rule ->
                    val title = when(rule.metric) {
                        WeatherMetric.WIND_SPEED -> "Ветер"
                        WeatherMetric.WIND_GUST -> "Порывы ветра"
                        WeatherMetric.PRECIPITATION -> "Осадки"
                        WeatherMetric.PRESSURE -> "Давление"
                        else -> rule.metric.name
                    }
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Порог ${rule.threshold}")
                        Row {
                            TextButton(onClick = { updateRule(rule.copy(threshold = (rule.threshold - 1.0).coerceAtLeast(0.0))) }) { Text("−") }
                            TextButton(onClick = { updateRule(rule.copy(threshold = rule.threshold + 1.0)) }) { Text("+") }
                        }
                    }
                    OptionRow(
                        title = "Условие",
                        values = WeatherRuleCondition.values().toList(),
                        selected = rule.condition,
                        label = { when(it) {
                            WeatherRuleCondition.AT_LEAST -> "Не ниже"
                            WeatherRuleCondition.AT_MOST -> "Не выше"
                            WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST -> "Изменение за период"
                        } },
                        onSelect = { condition -> updateRule(rule.copy(
                            condition = condition,
                            windowMillis = if (condition == WeatherRuleCondition.ABSOLUTE_CHANGE_AT_LEAST &&
                                rule.windowMillis == 0L) 3 * 3_600_000L else rule.windowMillis)) }
                    )
                    OptionRow(
                        title = "Период",
                        values = listOf(0L, 3_600_000L, 2 * 3_600_000L, 3 * 3_600_000L, 6 * 3_600_000L),
                        selected = rule.windowMillis,
                        label = { if (it == 0L) "Без задержки" else "${it / 3_600_000L} ч" },
                        onSelect = { updateRule(rule.copy(windowMillis = it)) }
                    )
                    OptionRow(
                        title = "Важность",
                        values = WeatherRuleSeverity.values().toList(),
                        selected = rule.severity,
                        label = { when(it) {
                            WeatherRuleSeverity.LOW -> "Низкая"
                            WeatherRuleSeverity.NORMAL -> "Обычная"
                            WeatherRuleSeverity.HIGH -> "Высокая"
                            WeatherRuleSeverity.CRITICAL -> "Критическая"
                        } },
                        onSelect = { updateRule(rule.copy(severity = it)) }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Выделять в прогнозе")
                        Switch(checked = rule.weatherHighlightEnabled,
                            onCheckedChange = { updateRule(rule.copy(weatherHighlightEnabled = it)) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Событие в расписании")
                        Switch(checked = rule.timelineEnabled,
                            onCheckedChange = { updateRule(rule.copy(timelineEnabled = it)) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Уведомление")
                        Switch(checked = rule.notificationEnabled, onCheckedChange = {
                            if (it) onRequestNotificationPermission()
                            updateRule(rule.copy(notificationEnabled = it))
                        })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            if (ruleScope == null) onRulesChange(rulesConfig.copy(
                                globalRules = rulesConfig.globalRules.filterNot { it.ruleId == rule.ruleId }))
                            else {
                                val scope = ruleScope ?: return@TextButton
                                onRulesChange(rulesConfig.copy(locationOverrides = rulesConfig.locationOverrides +
                                    (scope to (rulesConfig.locationOverrides[scope].orEmpty() +
                                        (rule.ruleId to WeatherRuleOverride.Disabled)))))
                            }
                        }) { Text("Удалить / отключить") }
                        if (ruleScope != null) TextButton(onClick = {
                            val scope = ruleScope ?: return@TextButton
                            val remaining = rulesConfig.locationOverrides[scope].orEmpty() - rule.ruleId
                            onRulesChange(rulesConfig.copy(locationOverrides = rulesConfig.locationOverrides +
                                (scope to remaining)))
                        }) { Text("Наследовать") }
                    }
                    HorizontalDivider()
                }
                if (activeRules.isEmpty()) Text("Для выбранного места правил нет")
                }
            }
        }

        item("weather-saved-locations") {
            SettingsSection("Сохранённые места") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (savedLocations.isEmpty()) "Пока нет сохранённых мест"
                        else savedLocations.size.toString() + " мест",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onAddSavedLocation) { Text("Добавить") }
                }
                savedLocations.sortedWith(compareBy({ it.order }, { it.id })).forEachIndexed { index, location ->
                    SavedLocationRow(
                        location = location,
                        canMoveUp = index > 0,
                        canMoveDown = index < savedLocations.lastIndex,
                        onEdit = { onEditSavedLocation(location.id) },
                        onDelete = { onDeleteSavedLocation(location.id) },
                        onMoveUp = { onReorderSavedLocations(movedIds(savedLocations, location.id, -1)) },
                        onMoveDown = { onReorderSavedLocations(movedIds(savedLocations, location.id, 1)) },
                    )
                }
            }
        }

        item("weather-bottom") {
            Spacer(Modifier.padding(bottom = appScaledSpacing(24.dp)))
        }
    }
}

private fun moveMetric(settings: WeatherSettings, metric: WeatherMetric, delta: Int): WeatherSettings =
    mutateWeatherPresentation(settings) {
        val ordered = metrics.sortedBy { it.order }.toMutableList()
        val from = ordered.indexOfFirst { it.metric == metric }
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from >= 0 && from != to) {
            val moved = ordered.removeAt(from)
            ordered.add(to, moved)
        }
        copy(metrics = ordered.mapIndexed { index, display -> display.copy(order = index) })
    }

private fun movedIds(locations: List<SavedWeatherLocation>, id: String, delta: Int): List<String> {
    val ids = locations.sortedWith(compareBy({ it.order }, { it.id })).map { it.id }.toMutableList()
    val from = ids.indexOf(id)
    if (from < 0) return ids
    val to = (from + delta).coerceIn(0, ids.lastIndex)
    if (from != to) ids.add(to, ids.removeAt(from))
    return ids
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EvolutionSurface(
            modifier = Modifier.fillMaxWidth(),
            role = EvolutionSurfaceRole.SOFT,
            shape = RoundedCornerShape(appCornerRadius(22.dp)),
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(appCardPadding()),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            ) { content() }
        }
    }
}

@Composable
private fun <T> OptionRow(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MetricRow(
    display: WeatherMetricDisplay,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(metricLabel(display.metric), style = MaterialTheme.typography.bodyMedium)
            Row {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("Выше") }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("Ниже") }
            }
        }
        Switch(checked = display.visible, onCheckedChange = onVisibleChange)
    }
}

@Composable
private fun SavedLocationRow(
    location: SavedWeatherLocation,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    EvolutionSurface(
        modifier = Modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.PRIMARY,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(appScaledSpacing(10.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
        ) {
            Text(location.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            location.address?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("↑") }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("↓") }
                TextButton(onClick = onEdit) { Text("Изменить") }
                TextButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

private fun providerLabel(value: WeatherProviderId): String = when (value) {
    WeatherProviderId.OPEN_METEO -> "Open-Meteo"
    WeatherProviderId.MET_NORWAY -> "MET Norway"
}

private fun presetLabel(value: WeatherPresetId): String = when (value) {
    WeatherPresetId.BASIC -> "Базовый"
    WeatherPresetId.WORK_WIND -> "Работа / ветер"
    WeatherPresetId.CUSTOM -> "Свой"
}

private fun layerLabel(value: WeatherLayerId): String = when (value) {
    WeatherLayerId.CURRENT -> "Сейчас"
    WeatherLayerId.HOURLY -> "По часам"
    WeatherLayerId.DAILY -> "По дням"
}

private fun metricLabel(value: WeatherMetric): String = when (value) {
    WeatherMetric.TEMPERATURE -> "Температура"
    WeatherMetric.FEELS_LIKE -> "Ощущается"
    WeatherMetric.CONDITION -> "Состояние"
    WeatherMetric.HUMIDITY -> "Влажность"
    WeatherMetric.PRESSURE -> "Давление"
    WeatherMetric.WIND_SPEED -> "Скорость ветра"
    WeatherMetric.WIND_GUST -> "Порывы"
    WeatherMetric.WIND_DIRECTION -> "Направление ветра"
    WeatherMetric.PRECIPITATION -> "Осадки"
    WeatherMetric.PRECIPITATION_PROBABILITY -> "Вероятность осадков"
    WeatherMetric.VISIBILITY -> "Видимость"
    WeatherMetric.UV_INDEX -> "УФ-индекс"
}

/** Human-readable choices for the Russian settings UI; never expose enum identifiers. */
fun weatherUnitLabel(value: Any): String = when (value) {
    TemperatureUnit.CELSIUS -> "°C"
    TemperatureUnit.FAHRENHEIT -> "°F"
    WindSpeedUnit.METRES_PER_SECOND -> "м/с"
    WindSpeedUnit.KILOMETRES_PER_HOUR -> "км/ч"
    WindSpeedUnit.MILES_PER_HOUR -> "миль/ч"
    WindSpeedUnit.KNOTS -> "узлы"
    PressureUnit.HECTOPASCAL -> "гПа"
    PressureUnit.MILLIBAR -> "мбар"
    PressureUnit.MILLIMETRES_MERCURY -> "мм рт. ст."
    PressureUnit.INCHES_MERCURY -> "дюйм рт. ст."
    PrecipitationUnit.MILLIMETRES -> "мм"
    PrecipitationUnit.INCHES -> "дюймы"
    VisibilityUnit.METRES -> "м"
    VisibilityUnit.KILOMETRES -> "км"
    VisibilityUnit.MILES -> "мили"
    WindDirectionFormat.DEGREES -> "Градусы"
    WindDirectionFormat.COMPASS -> "Румбы"
    WindDirectionFormat.DEGREES_AND_COMPASS -> "Градусы и румбы"
    else -> error("Unexpected weather unit: $value")
}
