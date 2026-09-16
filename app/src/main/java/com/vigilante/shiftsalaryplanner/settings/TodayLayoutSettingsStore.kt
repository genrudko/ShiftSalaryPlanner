package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class TodayBubbleId {
    HERO,
    PULSE,
    NEXT_STEP,
    QUICK_ACTIONS,
    TOMORROW,
    NEXT_ALARM,
    MONTH_STATUS,
    PAYMENTS,
    NOTES;

    /**
     * Fixed operational top blocks of the M15 Today hierarchy: the Hero and the
     * Timeline supersede the legacy HERO/PULSE/NEXT_STEP/QUICK_ACTIONS bubbles.
     * These legacy IDs remain parseable in serialized settings, but they are not
     * independently reorderable/editable lower modules after migration.
     */
    val isFixedTopModule: Boolean
        get() = this == HERO || this == PULSE || this == NEXT_STEP || this == QUICK_ACTIONS
}

enum class TodayBubbleSize {
    COMPACT,
    NORMAL,
    LARGE
}

enum class TodayBubbleWidth {
    HALF,
    FULL
}

enum class TodayBubbleBackground {
    DEFAULT,
    SOFT,
    ACCENT,
    GLASS,
    MINT,
    OCEAN,
    SKY,
    LAVENDER,
    ROSE,
    PEACH,
    AMBER,
    FOREST,
    GRAPHITE
}

data class TodayBubbleSettings(
    val id: TodayBubbleId,
    val visible: Boolean = true,
    val size: TodayBubbleSize = TodayBubbleSize.NORMAL,
    val width: TodayBubbleWidth = TodayBubbleWidth.FULL,
    val background: TodayBubbleBackground = TodayBubbleBackground.DEFAULT
)

data class TodayLayoutSettings(
    val bubbles: List<TodayBubbleSettings> = defaultTodayBubbles()
) {
    fun normalized(): TodayLayoutSettings {
        val seen = mutableSetOf<TodayBubbleId>()
        val cleaned = bubbles.mapNotNull { bubble ->
            if (bubble.id.isFixedTopModule) {
                return@mapNotNull null
            }
            if (seen.add(bubble.id)) bubble else null
        }
        val missing = defaultTodayBubbles().filter { it.id !in seen }
        return copy(bubbles = cleaned + missing)
    }
}

fun defaultTodayBubbles(): List<TodayBubbleSettings> = listOf(
    TodayBubbleSettings(TodayBubbleId.TOMORROW, size = TodayBubbleSize.COMPACT, width = TodayBubbleWidth.HALF),
    TodayBubbleSettings(TodayBubbleId.NEXT_ALARM, size = TodayBubbleSize.COMPACT, width = TodayBubbleWidth.HALF),
    TodayBubbleSettings(TodayBubbleId.MONTH_STATUS, width = TodayBubbleWidth.HALF),
    TodayBubbleSettings(TodayBubbleId.PAYMENTS, width = TodayBubbleWidth.HALF),
    TodayBubbleSettings(TodayBubbleId.NOTES, background = TodayBubbleBackground.SOFT)
)

class TodayLayoutSettingsStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(load())

    val settingsFlow: Flow<TodayLayoutSettings> = _settingsFlow.asStateFlow()

    fun save(settings: TodayLayoutSettings) {
        val normalized = settings.normalized()
        prefs.edit {
            putString(KEY_BUBBLES_JSON, normalized.bubbles.toJson().toString())
        }
        _settingsFlow.value = normalized
    }

    private fun load(): TodayLayoutSettings {
        val raw = prefs.getString(KEY_BUBBLES_JSON, null) ?: return TodayLayoutSettings()
        return runCatching { parseTodayLayoutSettings(raw).normalized() }
            .getOrDefault(TodayLayoutSettings())
    }

    private fun List<TodayBubbleSettings>.toJson(): JSONArray {
        val array = JSONArray()
        forEach { bubble ->
            array.put(
                JSONObject()
                    .put("id", bubble.id.name)
                    .put("visible", bubble.visible)
                    .put("size", bubble.size.name)
                    .put("width", bubble.width.name)
                    .put("background", bubble.background.name)
            )
        }
        return array
    }

    companion object {
        const val PREFS_NAME = "today_layout_settings"
        private const val KEY_BUBBLES_JSON = "bubbles_json"
    }
}

/**
 * Parses a serialized Today bubbles JSON array without touching Android storage.
 *
 * All historical IDs (including the legacy fixed-top HERO/PULSE/NEXT_STEP/QUICK_ACTIONS)
 * remain parseable for backup/settings compatibility; normalization to the operational
 * hierarchy is applied separately by [TodayLayoutSettings.normalized].
 */
fun parseTodayLayoutSettings(raw: String): TodayLayoutSettings {
    val array = JSONArray(raw)
    val bubbles = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optEnum<TodayBubbleId>("id") ?: continue
            add(
                TodayBubbleSettings(
                    id = id,
                    visible = item.optBoolean("visible", true),
                    size = item.optEnum<TodayBubbleSize>("size") ?: TodayBubbleSize.NORMAL,
                    width = item.optEnum<TodayBubbleWidth>("width") ?: TodayBubbleWidth.FULL,
                    background = item.optEnum<TodayBubbleBackground>("background")
                        ?: TodayBubbleBackground.DEFAULT
                )
            )
        }
    }
    return TodayLayoutSettings(bubbles)
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String): T? {
    val raw = optString(key, "")
    return enumValues<T>().firstOrNull { it.name == raw }
}
