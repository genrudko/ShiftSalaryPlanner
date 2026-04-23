package com.vigilante.shiftsalaryplanner

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant

const val PREF_NAME_PAYROLL_SETTINGS = "payroll_settings"
const val PREF_NAME_PAYROLL_YTD = "payroll_ytd"
const val PREF_NAME_ADDITIONAL_PAYMENTS = "additional_payments"
const val PREF_NAME_PAYROLL_DEDUCTIONS = "payroll_deductions"
const val PREF_NAME_PATTERN_TEMPLATES = "pattern_templates"
const val PREF_NAME_APPEARANCE_SETTINGS = "appearance_settings"
const val PREF_NAME_REPORT_VISIBILITY_SETTINGS = "report_visibility_settings"
const val PREF_NAME_WORK_ASSIGNMENTS = "work_assignments"
const val PREF_NAME_WORKPLACE_PAYROLL_SETTINGS = "workplace_payroll_settings"
const val PREF_NAME_WORKPLACE_PAYROLL_SALARIES_LEGACY = "workplace_payroll_salaries"
const val PREF_NAME_SHIFT_ALARM_SETTINGS = "shift_alarm_settings"
const val PREF_NAME_SHIFT_ALARM_SCHEDULER = "shift_alarm_scheduler"
const val PREF_NAME_SHIFT_COLORS = "shift_colors"
const val PREF_NAME_SHIFT_SPECIAL_RULES = "shift_special_rules"
const val PREF_NAME_MANUAL_HOLIDAYS = "manual_holidays"
const val PREF_NAME_CALENDAR_SYNC_META = "calendar_sync_meta"
const val PREF_NAME_WIDGET_SETTINGS = "widget_settings"
const val PREF_NAME_GOOGLE_DRIVE_SYNC_META = "google_drive_sync_meta"
const val PREF_NAME_SICK_LIMITS_CACHE = "sick_limits_cache"

data class AppBackupData(
    val sharedPrefs: Map<String, JSONObject>,
    val shiftDays: List<ShiftDayEntity>,
    val shiftTemplates: List<ShiftTemplateEntity>
)

fun exportAppBackupJson(
    prefSnapshots: List<Pair<String, SharedPreferences>>,
    shiftDays: List<ShiftDayEntity>,
    shiftTemplates: List<ShiftTemplateEntity>
): String {
    val root = JSONObject().apply {
        put("schemaVersion", 1)
        put("app", "ShiftSalaryPlanner")
        put("exportedAt", Instant.now().toString())
    }

    val prefsObject = JSONObject()
    prefSnapshots.forEach { (name, prefs) ->
        prefsObject.put(name, sharedPreferencesToJson(prefs))
    }
    root.put("sharedPrefs", prefsObject)

    val shiftDaysArray = JSONArray()
    shiftDays.forEach { day ->
        shiftDaysArray.put(
            JSONObject().apply {
                put("date", day.date)
                put("shiftCode", day.shiftCode)
            }
        )
    }
    root.put("shiftDays", shiftDaysArray)

    val shiftTemplatesArray = JSONArray()
    shiftTemplates.forEach { template ->
        shiftTemplatesArray.put(
            JSONObject().apply {
                put("code", template.code)
                put("title", template.title)
                put("iconKey", template.iconKey)
                put("totalHours", template.totalHours)
                put("breakHours", template.breakHours)
                put("nightHours", template.nightHours)
                put("colorHex", template.colorHex)
                put("isWeekendPaid", template.isWeekendPaid)
                put("active", template.active)
                put("sortOrder", template.sortOrder)
            }
        )
    }
    root.put("shiftTemplates", shiftTemplatesArray)

    return root.toString(2)
}

fun parseAppBackupJson(raw: String): AppBackupData {
    val root = JSONObject(raw)

    val sharedPrefsObject = root.optJSONObject("sharedPrefs") ?: JSONObject()
    val sharedPrefs = buildMap {
        val keys = sharedPrefsObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            sharedPrefsObject.optJSONObject(key)?.let { put(key, it) }
        }
    }

    val shiftDays = buildList {
        val array = root.optJSONArray("shiftDays") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val date = item.optString("date")
            val shiftCode = item.optString("shiftCode")
            if (date.isNotBlank() && shiftCode.isNotBlank()) {
                add(
                    ShiftDayEntity(
                        date = date,
                        shiftCode = shiftCode
                    )
                )
            }
        }
    }

    val shiftTemplates = buildList {
        val array = root.optJSONArray("shiftTemplates") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val code = item.optString("code")
            if (code.isBlank()) continue

            add(
                ShiftTemplateEntity(
                    code = code,
                    title = item.optString("title"),
                    iconKey = item.optString("iconKey"),
                    totalHours = item.optDouble("totalHours", 0.0),
                    breakHours = item.optDouble("breakHours", 0.0),
                    nightHours = item.optDouble("nightHours", 0.0),
                    colorHex = item.optString("colorHex", "#BDBDBD"),
                    isWeekendPaid = item.optBoolean("isWeekendPaid", false),
                    active = item.optBoolean("active", true),
                    sortOrder = item.optInt("sortOrder", 0)
                )
            )
        }
    }

    return AppBackupData(
        sharedPrefs = sharedPrefs,
        shiftDays = shiftDays,
        shiftTemplates = shiftTemplates
    )
}

fun applySharedPreferencesSnapshot(
    prefs: SharedPreferences,
    snapshot: JSONObject
) {
    prefs.edit().clear().apply()

    prefs.edit().apply {
        val keys = snapshot.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = snapshot.optJSONObject(key) ?: continue
            when (item.optString("type")) {
                "Boolean" -> putBoolean(key, item.optBoolean("value", false))
                "Int" -> putInt(key, item.optInt("value", 0))
                "Long" -> putLong(key, item.optLong("value", 0L))
                "Float" -> putFloat(key, item.optDouble("value", 0.0).toFloat())
                "String" -> putString(key, item.optString("value"))
                "StringSet" -> {
                    val array = item.optJSONArray("value") ?: JSONArray()
                    val set = buildSet {
                        for (index in 0 until array.length()) {
                            add(array.optString(index))
                        }
                    }
                    putStringSet(key, set)
                }
            }
        }
        apply()
    }
}

private fun sharedPreferencesToJson(prefs: SharedPreferences): JSONObject {
    val root = JSONObject()
    prefs.all.forEach { (key, value) ->
        val item = JSONObject()
        when (value) {
            is Boolean -> {
                item.put("type", "Boolean")
                item.put("value", value)
            }
            is Int -> {
                item.put("type", "Int")
                item.put("value", value)
            }
            is Long -> {
                item.put("type", "Long")
                item.put("value", value)
            }
            is Float -> {
                item.put("type", "Float")
                item.put("value", value.toDouble())
            }
            is String -> {
                item.put("type", "String")
                item.put("value", value)
            }
            is Set<*> -> {
                item.put("type", "StringSet")
                val array = JSONArray()
                value.mapNotNull { it as? String }.forEach { array.put(it) }
                item.put("value", array)
            }
            else -> return@forEach
        }
        root.put(key, item)
    }
    return root
}
@Composable
fun BackupRestoreScreen(
    shiftDaysCount: Int,
    shiftTemplatesCount: Int,
    additionalPaymentsCount: Int,
    patternTemplatesCount: Int,
    manualHolidayCount: Int,
    googleAccountEmail: String,
    lastUploadAtMillis: Long,
    lastRestoreAtMillis: Long,
    lastCloudModifiedAtMillis: Long,
    autoUploadEnabled: Boolean,
    autoUploadIntervalHours: Int,
    statusMessage: String?,
    oauthPackageName: String,
    oauthSha1: String,
    oauthSha256: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onUploadToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onAutoUploadEnabledChange: (Boolean) -> Unit,
    onAutoUploadIntervalHoursChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isGoogleConnected = googleAccountEmail.isNotBlank()
    val intervalOptions = listOf(
        24 to "24ч",
        24 * 3 to "3д",
        24 * 7 to "7д",
        24 * 14 to "14д",
        24 * 30 to "30д"
    )
    val intervalRows = intervalOptions.chunked(3)
    val presetHours = intervalOptions.map { it.first }.toSet()
    var customDaysInput by rememberSaveable(autoUploadIntervalHours) {
        mutableStateOf(
            if (autoUploadIntervalHours !in presetHours) {
                (autoUploadIntervalHours / 24).coerceAtLeast(1).toString()
            } else {
                ""
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Резервная копия",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(appScreenPadding())
            ) {
                SettingsSectionCard(
                    title = "Что входит в копию",
                    subtitle = "Экспортируются все основные пользовательские данные",
                    content = {
                        Text("Смены в календаре: $shiftDaysCount")
                        Text("Шаблоны смен: $shiftTemplatesCount")
                        Text("Доплаты и премии: $additionalPaymentsCount")
                        Text("Удержания: сохраняются")
                        Text("Шаблоны чередований: $patternTemplatesCount")
                        Text("Ручные праздники: $manualHolidayCount")
                        Text("Также сохраняются: внешний вид, виджеты, видимость блоков, календарь, зарплатные настройки, будильники, цвета и спецправила смен, метаданные синхронизации Google Drive.")
                    }
                )

                Spacer(modifier = Modifier.height(appSectionSpacing()))

                SettingsSectionCard(
                    title = "Действия",
                    subtitle = "Экспорт и импорт резервной копии",
                    content = {
                        Text(
                            text = "Экспорт создаёт JSON-файл, который можно перенести на другое устройство. Импорт заменяет текущие данные приложения данными из файла.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(appSectionSpacing()))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExport()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .appLargeButtonSizing()
                            ) {
                                Text("Экспорт")
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onImport()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .appLargeButtonSizing()
                            ) {
                                Text("Импорт")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(appSectionSpacing()))

                SettingsSectionCard(
                    title = "Google Drive",
                    subtitle = "Синхронизация через скрытую папку приложения (AppData)",
                    content = {
                        Text(
                            text = if (isGoogleConnected) {
                                "Подключён аккаунт: $googleAccountEmail"
                            } else {
                                "Аккаунт не подключён"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (isGoogleConnected) {
                            val lastUploadLabel = formatBackupTimestamp(lastUploadAtMillis)
                            val lastRestoreLabel = formatBackupTimestamp(lastRestoreAtMillis)
                            val lastCloudLabel = formatBackupTimestamp(lastCloudModifiedAtMillis)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Последняя загрузка в облако: $lastUploadLabel",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Последнее восстановление: $lastRestoreLabel",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Последнее обновление файла в облаке: $lastCloudLabel",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(appSectionSpacing()))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Автозагрузка",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = autoUploadEnabled,
                                    onCheckedChange = { enabled ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAutoUploadEnabledChange(enabled)
                                    }
                                )
                            }

                            if (autoUploadEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Интервал автозагрузки",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                intervalRows.forEachIndexed { rowIndex, rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { (hours, label) ->
                                            val selected = autoUploadIntervalHours == hours
                                            if (selected) {
                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onAutoUploadIntervalHoursChange(hours)
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .appLargeButtonSizing(base = 44.dp)
                                                ) {
                                                    Text(label)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onAutoUploadIntervalHoursChange(hours)
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .appLargeButtonSizing(base = 44.dp)
                                                ) {
                                                    Text(label)
                                                }
                                            }
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (rowIndex != intervalRows.lastIndex) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Свой интервал (дней)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customDaysInput,
                                    onValueChange = { value ->
                                        customDaysInput = value.filter { it.isDigit() }.take(3)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("например, 45") }
                                )
                                val customDays = customDaysInput.toIntOrNull()
                                val customDaysValid = customDays != null && customDays in 1..365
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onAutoUploadIntervalHoursChange(customDays!! * 24)
                                        },
                                        enabled = customDaysValid,
                                        modifier = Modifier.appLargeButtonSizing(base = 42.dp)
                                    ) {
                                        Text("Применить")
                                    }
                                }
                                if (autoUploadIntervalHours !in presetHours) {
                                    Text(
                                        text = "Текущий свой интервал: ${
                                            if (autoUploadIntervalHours % 24 == 0) {
                                                "${autoUploadIntervalHours / 24}д"
                                            } else {
                                                "${autoUploadIntervalHours}ч"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(appSectionSpacing()))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isGoogleConnected) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onGoogleSignOut()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .appLargeButtonSizing()
                                ) {
                                    Text("Выйти")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onGoogleSignIn()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .appLargeButtonSizing()
                                ) {
                                    Text("Войти")
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUploadToCloud()
                                },
                                enabled = isGoogleConnected,
                                modifier = Modifier
                                    .weight(1f)
                                    .appLargeButtonSizing()
                            ) {
                                Text("В облако")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRestoreFromCloud()
                            },
                            enabled = isGoogleConnected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .appLargeButtonSizing()
                        ) {
                            Text("Восстановить из облака")
                        }

                        Spacer(modifier = Modifier.height(appSectionSpacing()))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(appCornerRadius(12.dp)),
                            color = appBubbleBackgroundColor(defaultAlpha = 0.30f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                appPanelBorderColor().copy(alpha = 0.85f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(appCardPadding()),
                                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
                            ) {
                                Text(
                                    text = "Диагностика OAuth",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Package: $oauthPackageName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appListSecondaryTextColor()
                                )
                                Text(
                                    text = "SHA-1: ${oauthSha1.ifBlank { "не удалось определить" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appListSecondaryTextColor()
                                )
                                Text(
                                    text = "SHA-256: ${oauthSha256.ifBlank { "не удалось определить" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appListSecondaryTextColor()
                                )
                                Text(
                                    text = "Добавь этот package+SHA-1 в Android OAuth client в Google Cloud.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appListSecondaryTextColor()
                                )
                            }
                        }
                    }
                )

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(appSectionSpacing()))
                    AppFeedbackCard(
                        message = statusMessage,
                        state = inferAppFeedbackState(statusMessage),
                        title = "Статус операции"
                    )
                }
            }
        }
    }
}

private fun formatBackupTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "ещё не было"
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}
