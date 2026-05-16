package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ReportHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val title: String,
    val periodLabel: String,
    val workplaceLabel: String,
    val gross: Double,
    val ndfl: Double,
    val net: Double,
    val fileName: String,
    val format: String
)

class ReportHistoryStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _itemsFlow = MutableStateFlow(loadFromPrefs())

    val itemsFlow: Flow<List<ReportHistoryItem>> = _itemsFlow.asStateFlow()

    fun add(item: ReportHistoryItem) {
        val next = (listOf(item) + loadFromPrefs()).take(MAX_ITEMS)
        saveToPrefs(next)
        _itemsFlow.value = next
    }

    fun clear() {
        saveToPrefs(emptyList())
        _itemsFlow.value = emptyList()
    }

    private fun loadFromPrefs(): List<ReportHistoryItem> {
        return runCatching {
            val array = JSONArray(prefs.getString(KEY_ITEMS_JSON, "[]") ?: "[]")
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        ReportHistoryItem(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            timestampMillis = obj.optLong("timestampMillis", 0L).takeIf { it > 0L }
                                ?: System.currentTimeMillis(),
                            title = obj.optString("title", "Отчёт"),
                            periodLabel = obj.optString("periodLabel", ""),
                            workplaceLabel = obj.optString("workplaceLabel", ""),
                            gross = obj.optDouble("gross", 0.0),
                            ndfl = obj.optDouble("ndfl", 0.0),
                            net = obj.optDouble("net", 0.0),
                            fileName = obj.optString("fileName", ""),
                            format = obj.optString("format", "")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun saveToPrefs(items: List<ReportHistoryItem>) {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("timestampMillis", item.timestampMillis)
                    put("title", item.title)
                    put("periodLabel", item.periodLabel)
                    put("workplaceLabel", item.workplaceLabel)
                    put("gross", item.gross)
                    put("ndfl", item.ndfl)
                    put("net", item.net)
                    put("fileName", item.fileName)
                    put("format", item.format)
                }
            )
        }
        prefs.edit {
            putString(KEY_ITEMS_JSON, array.toString())
        }
    }

    companion object {
        const val PREFS_NAME = "report_history"
        private const val KEY_ITEMS_JSON = "items_json"
        private const val MAX_ITEMS = 60
    }
}
