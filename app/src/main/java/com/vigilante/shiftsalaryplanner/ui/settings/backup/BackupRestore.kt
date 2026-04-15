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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

const val PREF_NAME_PAYROLL_SETTINGS = "payroll_settings"
const val PREF_NAME_ADDITIONAL_PAYMENTS = "additional_payments"
const val PREF_NAME_PATTERN_TEMPLATES = "pattern_templates"
const val PREF_NAME_SHIFT_ALARM_SETTINGS = "shift_alarm_settings"
const val PREF_NAME_SHIFT_COLORS = "shift_colors"
const val PREF_NAME_SHIFT_SPECIAL_RULES = "shift_special_rules"
const val PREF_NAME_MANUAL_HOLIDAYS = "manual_holidays"

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
    statusMessage: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
                        Text("Шаблоны чередований: $patternTemplatesCount")
                        Text("Ручные праздники: $manualHolidayCount")
                        Text("Также сохраняются: зарплатные настройки, будильники, цвета и спецправила смен.")
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Экспорт")
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onImport()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Импорт")
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
