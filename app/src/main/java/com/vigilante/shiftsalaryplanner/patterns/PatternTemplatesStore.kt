package com.vigilante.shiftsalaryplanner.patterns

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PatternTemplatesStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("pattern_templates", Context.MODE_PRIVATE)
    private val keyPatternsJson = "patterns_json"

    private val _patternsFlow = MutableStateFlow(loadFromPrefs())
    val patternsFlow: Flow<List<PatternTemplate>> = _patternsFlow.asStateFlow()

    private fun loadFromPrefs(): List<PatternTemplate> {
        return try {
            val raw = prefs.getString(keyPatternsJson, "[]") ?: "[]"
            val array = JSONArray(raw)

            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val stepsArray = obj.optJSONArray("steps") ?: JSONArray()

                    val steps = buildList {
                        for (j in 0 until stepsArray.length()) {
                            add(stepsArray.optString(j, ""))
                        }
                    }

                    add(
                        PatternTemplate(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            steps = steps
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(items: List<PatternTemplate>) {
        val array = JSONArray()

        items.forEach { item ->
            val stepsArray = JSONArray()
            item.normalizedSteps().forEach { step ->
                stepsArray.put(step)
            }

            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("steps", stepsArray)
            }

            array.put(obj)
        }

        prefs.edit()
            .putString(keyPatternsJson, array.toString())
            .apply()
    }

    suspend fun addOrUpdate(item: PatternTemplate) {
        val current = loadFromPrefs().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }

        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }

        saveToPrefs(current)
        _patternsFlow.value = loadFromPrefs()
    }

    suspend fun deleteById(id: String) {
        val current = loadFromPrefs().filterNot { it.id == id }
        saveToPrefs(current)
        _patternsFlow.value = loadFromPrefs()
    }
}