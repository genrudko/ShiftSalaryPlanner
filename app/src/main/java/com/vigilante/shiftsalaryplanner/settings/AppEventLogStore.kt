package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppEventLogItem(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val title: String,
    val message: String = "",
    val category: String = "INFO"
)

class AppEventLogStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _eventsFlow = MutableStateFlow(loadFromPrefs())

    val eventsFlow: Flow<List<AppEventLogItem>> = _eventsFlow.asStateFlow()

    fun add(title: String, message: String = "", category: String = "INFO") {
        val next = (listOf(
            AppEventLogItem(
                title = title.trim().ifBlank { "Событие" },
                message = message.trim(),
                category = category.trim().ifBlank { "INFO" }
            )
        ) + loadFromPrefs()).take(MAX_EVENTS)
        saveToPrefs(next)
        _eventsFlow.value = next
    }

    fun clear() {
        saveToPrefs(emptyList())
        _eventsFlow.value = emptyList()
    }

    private fun loadFromPrefs(): List<AppEventLogItem> {
        return runCatching {
            val array = JSONArray(prefs.getString(KEY_EVENTS_JSON, "[]") ?: "[]")
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        AppEventLogItem(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            timestampMillis = obj.optLong("timestampMillis", 0L).takeIf { it > 0L }
                                ?: System.currentTimeMillis(),
                            title = obj.optString("title", "Событие"),
                            message = obj.optString("message", ""),
                            category = obj.optString("category", "INFO")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun saveToPrefs(items: List<AppEventLogItem>) {
        val array = JSONArray()
        items.take(MAX_EVENTS).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("timestampMillis", item.timestampMillis)
                    put("title", item.title)
                    put("message", item.message)
                    put("category", item.category)
                }
            )
        }
        prefs.edit {
            putString(KEY_EVENTS_JSON, array.toString())
        }
    }

    companion object {
        const val PREFS_NAME = "app_event_log"
        private const val KEY_EVENTS_JSON = "events_json"
        private const val MAX_EVENTS = 120
    }
}
