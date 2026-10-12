package com.vigilante.shiftsalaryplanner.ui.weather

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.appCardPadding
import com.vigilante.shiftsalaryplanner.appCornerRadius
import com.vigilante.shiftsalaryplanner.appScaledSpacing
import com.vigilante.shiftsalaryplanner.settings.WeatherSettings
import com.vigilante.shiftsalaryplanner.weather.NormalizedWeather
import com.vigilante.shiftsalaryplanner.weather.ResolvedWeatherLocation
import com.vigilante.shiftsalaryplanner.weather.WeatherCurrent
import com.vigilante.shiftsalaryplanner.weather.WeatherDailyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherFetchRequest
import com.vigilante.shiftsalaryplanner.weather.WeatherHourlyPoint
import com.vigilante.shiftsalaryplanner.weather.WeatherLayerId
import com.vigilante.shiftsalaryplanner.weather.WeatherLoadResult
import com.vigilante.shiftsalaryplanner.weather.WeatherLocationSource
import com.vigilante.shiftsalaryplanner.weather.WeatherMetric
import com.vigilante.shiftsalaryplanner.weather.WeatherRuleTransition
import com.vigilante.shiftsalaryplanner.weather.WeatherPresentationConfig
import com.vigilante.shiftsalaryplanner.weather.formatPrecipitation
import com.vigilante.shiftsalaryplanner.weather.formatPressure
import com.vigilante.shiftsalaryplanner.weather.formatTemperature
import com.vigilante.shiftsalaryplanner.weather.formatVisibility
import com.vigilante.shiftsalaryplanner.weather.formatWindDirection
import com.vigilante.shiftsalaryplanner.weather.formatWindSpeed
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** A session-only location option; no persistence or provider detail reaches Today. */
data class TodayWeatherLocationChoice(
    val source: WeatherLocationSource,
    val label: String,
)

/** Do not turn optional metrics into hard requirements that would defeat fallback. */
fun buildTodayWeatherRequest(
    coordinates: com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates,
    settings: WeatherSettings,
): WeatherFetchRequest {
    val presentation = settings.presentationConfig
    return WeatherFetchRequest(
        coordinates = coordinates,
        hourlyHours = if (WeatherLayerId.HOURLY in presentation.layers) presentation.hourlyHorizon.hours else 0,
        dailyDays = if (WeatherLayerId.DAILY in presentation.layers) presentation.dailyHorizon.days else 0,
        requiredMetrics = emptySet(),
        optionalMetrics = WeatherMetric.values().toSet(),
    )
}

fun hourlyWeatherPoints(
    weather: NormalizedWeather,
    config: WeatherPresentationConfig,
    nowMillis: Long,
): List<WeatherHourlyPoint> =
    if (WeatherLayerId.HOURLY in config.layers)
        weather.hourly.filter { it.forecastTimeMillis >= nowMillis }
            .sortedBy { it.forecastTimeMillis }.take(config.hourlyHorizon.hours)
    else emptyList()

fun dailyWeatherPoints(
    weather: NormalizedWeather,
    config: WeatherPresentationConfig,
): List<WeatherDailyPoint> =
    if (WeatherLayerId.DAILY in config.layers)
        weather.daily.sortedBy { it.forecastDayMillis }.take(config.dailyHorizon.days)
    else emptyList()

/** Missing optional measurements are omitted; configured order and visibility remain authoritative. */
fun currentWeatherMetricRows(
    current: WeatherCurrent,
    settings: WeatherSettings,
): List<Pair<WeatherMetric, String>> =
    settings.presentationConfig.metrics.asSequence()
        .filter { it.visible }
        .sortedBy { it.order }
        .mapNotNull { entry ->
            val text = when (entry.metric) {
                WeatherMetric.TEMPERATURE -> formatTemperature(current.temperatureCelsius, settings.temperatureUnit)
                WeatherMetric.FEELS_LIKE -> current.feelsLikeCelsius?.let { formatTemperature(it, settings.temperatureUnit) }
                WeatherMetric.CONDITION -> current.conditionCode?.let(::weatherConditionLabel)
                WeatherMetric.HUMIDITY -> current.relativeHumidityPercent?.let { "${it.roundToInt()}%" }
                WeatherMetric.PRESSURE -> current.pressureHpa?.let { formatPressure(it, settings.pressureUnit) }
                WeatherMetric.WIND_SPEED -> current.windSpeedMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_GUST -> current.windGustMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_DIRECTION -> current.windDirectionDegrees?.let { formatWindDirection(it, settings.windDirectionFormat) }
                WeatherMetric.PRECIPITATION -> current.precipitationMm?.let { formatPrecipitation(it, settings.precipitationUnit) }
                WeatherMetric.PRECIPITATION_PROBABILITY -> null // hourly/daily only
                WeatherMetric.VISIBILITY -> current.visibilityMetres?.let { formatVisibility(it, settings.visibilityUnit) }
                WeatherMetric.UV_INDEX -> current.uvIndex?.let { "${(it * 10).roundToInt() / 10.0}" }
            }
            text?.let { entry.metric to it }
        }.toList()

fun hourlyWeatherMetricRows(
    point: WeatherHourlyPoint,
    settings: WeatherSettings,
): List<Pair<WeatherMetric, String>> =
    settings.presentationConfig.metrics.asSequence().filter { it.visible }.sortedBy { it.order }
        .mapNotNull { entry ->
            val value = when (entry.metric) {
                WeatherMetric.TEMPERATURE -> formatTemperature(point.temperatureCelsius, settings.temperatureUnit)
                WeatherMetric.FEELS_LIKE -> point.feelsLikeCelsius?.let { formatTemperature(it, settings.temperatureUnit) }
                WeatherMetric.CONDITION -> point.conditionCode?.let(::weatherConditionLabel)
                WeatherMetric.HUMIDITY -> point.relativeHumidityPercent?.let { "${it.roundToInt()}%" }
                WeatherMetric.PRESSURE -> point.pressureHpa?.let { formatPressure(it, settings.pressureUnit) }
                WeatherMetric.WIND_SPEED -> point.windSpeedMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_GUST -> point.windGustMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_DIRECTION -> point.windDirectionDegrees?.let { formatWindDirection(it, settings.windDirectionFormat) }
                WeatherMetric.PRECIPITATION -> point.precipitationMm?.let { formatPrecipitation(it, settings.precipitationUnit) }
                WeatherMetric.PRECIPITATION_PROBABILITY -> point.precipitationProbabilityPercent?.let { "${it.roundToInt()}%" }
                WeatherMetric.VISIBILITY -> point.visibilityMetres?.let { formatVisibility(it, settings.visibilityUnit) }
                WeatherMetric.UV_INDEX -> null
            }
            value?.let { entry.metric to it }
        }.toList()

fun dailyWeatherMetricRows(
    point: WeatherDailyPoint,
    settings: WeatherSettings,
): List<Pair<WeatherMetric, String>> =
    settings.presentationConfig.metrics.asSequence().filter { it.visible }.sortedBy { it.order }
        .mapNotNull { entry ->
            val value = when (entry.metric) {
                WeatherMetric.TEMPERATURE -> "${formatTemperature(point.minTemperatureCelsius, settings.temperatureUnit)}…${formatTemperature(point.maxTemperatureCelsius, settings.temperatureUnit)}"
                WeatherMetric.FEELS_LIKE -> null
                WeatherMetric.CONDITION -> point.conditionCode?.let(::weatherConditionLabel)
                WeatherMetric.HUMIDITY -> point.relativeHumidityPercent?.let { "${it.roundToInt()}%" }
                WeatherMetric.PRESSURE -> point.pressureHpa?.let { formatPressure(it, settings.pressureUnit) }
                WeatherMetric.WIND_SPEED -> point.windSpeedMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_GUST -> point.windGustMs?.let { formatWindSpeed(it, settings.windSpeedUnit) }
                WeatherMetric.WIND_DIRECTION -> point.windDirectionDegrees?.let { formatWindDirection(it, settings.windDirectionFormat) }
                WeatherMetric.PRECIPITATION -> point.precipitationMm?.let { formatPrecipitation(it, settings.precipitationUnit) }
                WeatherMetric.PRECIPITATION_PROBABILITY -> point.precipitationProbabilityPercent?.let { "${it.roundToInt()}%" }
                WeatherMetric.VISIBILITY -> null
                WeatherMetric.UV_INDEX -> point.uvIndexMax?.let { "${(it * 10).roundToInt() / 10.0}" }
            }
            value?.let { entry.metric to it }
        }.toList()

private fun weatherMetricLabel(metric: WeatherMetric): String = when (metric) {
    WeatherMetric.TEMPERATURE -> "Температура"
    WeatherMetric.FEELS_LIKE -> "Ощущается"
    WeatherMetric.CONDITION -> "Условия"
    WeatherMetric.HUMIDITY -> "Влажность"
    WeatherMetric.PRESSURE -> "Давление"
    WeatherMetric.WIND_SPEED -> "Ветер"
    WeatherMetric.WIND_GUST -> "Порывы"
    WeatherMetric.WIND_DIRECTION -> "Направление"
    WeatherMetric.PRECIPITATION -> "Осадки"
    WeatherMetric.PRECIPITATION_PROBABILITY -> "Вероятность осадков"
    WeatherMetric.VISIBILITY -> "Видимость"
    WeatherMetric.UV_INDEX -> "УФ-индекс"
}

/** Normalize provider-neutral condition codes from either adapter for the Russian UI. */
fun weatherConditionLabel(raw: String): String {
    val code = raw.lowercase().substringBefore("_day").substringBefore("_night")
        .substringBefore("_polartwilight")
    return when {
        code in setOf("clear", "clearsky", "mainly_clear", "fair") -> "Ясно"
        code.contains("partly") -> "Переменная облачность"
        code.contains("cloud") -> "Облачно"
        code.contains("fog") -> "Туман"
        code.contains("thunder") -> "Гроза"
        code.contains("snow") -> "Снег"
        code.contains("sleet") -> "Мокрый снег"
        code.contains("rain") || code.contains("drizzle") -> "Дождь"
        else -> "Данные о погоде"
    }
}

private fun providerName(result: WeatherLoadResult): String =
    when (result.sourceProvider?.name) {
        "OPEN_METEO" -> "Open-Meteo"
        "MET_NORWAY" -> "MET Norway"
        else -> "Источник неизвестен"
    }

private fun timeLabel(timestamp: Long, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag("ru-RU"))
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))

/** Fixed Hero content remains concise even when the expanded forecast exposes many metrics. */
@Composable
fun TodayWeatherHero(
    location: ResolvedWeatherLocation?,
    result: WeatherLoadResult?,
    loading: Boolean,
    settings: WeatherSettings,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))) {
        if (location == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Погода: место не задано", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onOpenSettings) { Text("Настроить") }
            }
        } else {
            val current = result?.data?.current
            Text("Погода · ${location.label}", style = MaterialTheme.typography.labelLarge)
            when {
                current != null -> {
                    val pieces = buildList {
                        add(weatherConditionLabel(current.conditionCode ?: "unknown"))
                        current.windDirectionDegrees?.let { add(formatWindDirection(it, settings.windDirectionFormat)) }
                        current.windSpeedMs?.let { add("ветер ${formatWindSpeed(it, settings.windSpeedUnit)}") }
                        current.windGustMs?.let { add("порывы ${formatWindSpeed(it, settings.windSpeedUnit)}") }
                    }
                    Text(
                        "${formatTemperature(current.temperatureCelsius, settings.temperatureUnit)} · ${pieces.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (result.stale) Text("Кэшированные данные", style = MaterialTheme.typography.labelSmall)
                }
                loading -> Text("Загружаем прогноз…", style = MaterialTheme.typography.bodyMedium)
                else -> Text("Прогноз пока недоступен", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Standalone fixed-weather module (not part of the draggable legacy bubbles). */
@Composable
fun TodayWeatherModule(
    location: ResolvedWeatherLocation?,
    result: WeatherLoadResult?,
    settings: WeatherSettings,
    loading: Boolean,
    locationChoices: List<TodayWeatherLocationChoice>,
    temporaryLocation: WeatherLocationSource?,
    onSelectLocation: (WeatherLocationSource?) -> Unit,
    onUseCurrentDevice: () -> Unit,
    locationMessage: String?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    nowMillis: Long = System.currentTimeMillis(),
    ruleHighlights: List<WeatherRuleTransition> = emptyList(),
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text("Прогноз погоды", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Свернуть" else "Подробнее") }
            }
            Text(
                when {
                    location == null -> "Выберите рабочее место или сохранённый адрес"
                    loading && result?.data == null -> "Загрузка данных…"
                    result?.data?.current != null -> {
                        val current = result.data.current
                        "${location.label} · ${formatTemperature(current.temperatureCelsius, settings.temperatureUnit)} · ${weatherConditionLabel(current.conditionCode.orEmpty())}"
                    }
                    else -> "${location.label} · прогноз не получен"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            val significant = ruleHighlights.filter { it.active && it.weatherHighlightEnabled }
                .take(3)
            if (significant.isNotEmpty()) {
                significant.forEach { event ->
                    val formatted = when (event.metric) {
                        WeatherMetric.WIND_SPEED, WeatherMetric.WIND_GUST ->
                            formatWindSpeed(event.observedValue, settings.windSpeedUnit)
                        WeatherMetric.PRESSURE ->
                            formatPressure(event.observedValue, settings.pressureUnit)
                        WeatherMetric.PRECIPITATION ->
                            formatPrecipitation(event.observedValue, settings.precipitationUnit)
                        else -> event.observedValue.toString()
                    }
                    Text("Внимание: ${weatherMetricLabel(event.metric)} · $formatted",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (expanded) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))) {
                    FilterChip(
                        selected = temporaryLocation == null,
                        onClick = { onSelectLocation(null) },
                        label = { Text("Рабочее место") },
                    )
                    locationChoices.forEach { choice ->
                        FilterChip(
                            selected = temporaryLocation == choice.source,
                            onClick = { onSelectLocation(choice.source) },
                            label = { Text(choice.label) },
                        )
                    }
                    AssistChip(onClick = onUseCurrentDevice, label = { Text("Текущее GPS") })
                }
                locationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                    TextButton(onClick = onRefresh, enabled = location != null && !loading) { Text("Обновить") }
                    TextButton(onClick = onOpenSettings) { Text("Настройки погоды") }
                }
                if (result != null) {
                    Text(
                        buildString {
                            append(providerName(result))
                            if (result.usedFallback) append(" · резервный")
                            if (result.stale) append(" · устаревшие данные")
                            result.fetchedAtMillis?.let { append(" · обновлено ${timeLabel(it, "dd.MM HH:mm")}") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val data = result?.data
                if (result?.usedFallback == true && data != null &&
                    WeatherLayerId.DAILY in settings.presentationConfig.layers && data.daily.isEmpty()
                ) {
                    Text("Резервный источник не предоставляет прогноз по дням",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (data != null) {
                    val current = data.current
                    if (current != null && WeatherLayerId.CURRENT in settings.presentationConfig.layers) {
                        Text("Сейчас", fontWeight = FontWeight.SemiBold)
                        currentWeatherMetricRows(current, settings).forEach { (metric, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(weatherMetricLabel(metric), style = MaterialTheme.typography.bodyMedium)
                                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    val hours = hourlyWeatherPoints(data, settings.presentationConfig, nowMillis)
                    if (hours.isNotEmpty()) {
                        Text("По часам · ${settings.presentationConfig.hourlyHorizon.hours} ч", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))) {
                            hours.forEach { point ->
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Column(Modifier.padding(appScaledSpacing(10.dp))) {
                                        Text(timeLabel(point.forecastTimeMillis, "HH:mm"), style = MaterialTheme.typography.labelSmall)
                                        hourlyWeatherMetricRows(point, settings).forEach { (metric, value) ->
                                            Text("${weatherMetricLabel(metric)}: $value", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val days = dailyWeatherPoints(data, settings.presentationConfig)
                    if (days.isNotEmpty()) {
                        Text("По дням · ${settings.presentationConfig.dailyHorizon.days} дн.", fontWeight = FontWeight.SemiBold)
                        days.forEach { day ->
                            val metrics = dailyWeatherMetricRows(day, settings)
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp)),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(timeLabel(day.forecastDayMillis, "EEE, dd.MM"),
                                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        metrics.firstOrNull { it.first == WeatherMetric.TEMPERATURE }?.second
                                            ?: "${formatTemperature(day.minTemperatureCelsius, settings.temperatureUnit)}…${formatTemperature(day.maxTemperatureCelsius, settings.temperatureUnit)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                val extras = metrics.filterNot { it.first == WeatherMetric.TEMPERATURE }
                                if (extras.isNotEmpty()) {
                                    Row(
                                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
                                    ) {
                                        extras.forEach { (metric, value) ->
                                            Text(
                                                "${weatherMetricLabel(metric)}: $value",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!loading && location != null) {
                    Text("Нет данных. Повторите обновление позже.", style = MaterialTheme.typography.bodySmall)
                }
                if (result?.stale == true) {
                    Text("Нет связи с источниками; показан последний сохранённый прогноз.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
