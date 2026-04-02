package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class DeductionsStore(context: Context) {
    private val prefs = context.getSharedPreferences("payroll_deductions", Context.MODE_PRIVATE)
    private val keyDeductionsJson = "deductions_json"
    private val _deductionsFlow = MutableStateFlow(loadFromPrefs())

    val deductionsFlow: Flow<List<PayrollDeduction>> = _deductionsFlow.asStateFlow()

    private fun loadFromPrefs(): List<PayrollDeduction> {
        return try {
            val raw = prefs.getString(keyDeductionsJson, "[]") ?: "[]"
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        PayrollDeduction(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            type = obj.optString("type", "OTHER"),
                            mode = obj.optString("mode", "FIXED"),
                            value = obj.optDouble("value", 0.0),
                            active = obj.optBoolean("active", true),
                            applyToAdvance = obj.optBoolean("applyToAdvance", false),
                            applyToSalary = obj.optBoolean("applyToSalary", true),
                            priority = obj.optInt("priority", 0),
                            note = obj.optString("note", ""),
                            preserveMinimumIncome = obj.optBoolean("preserveMinimumIncome", false),
                            maxPercentLimit = obj.optDouble("maxPercentLimit", 50.0)
                        )
                    )
                }
            }.sortedWith(compareBy<PayrollDeduction> { it.priority }.thenBy { it.title.lowercase() })
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(items: List<PayrollDeduction>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("type", item.type)
                put("mode", item.mode)
                put("value", item.value)
                put("active", item.active)
                put("applyToAdvance", item.applyToAdvance)
                put("applyToSalary", item.applyToSalary)
                put("priority", item.priority)
                put("note", item.note)
                put("preserveMinimumIncome", item.preserveMinimumIncome)
                put("maxPercentLimit", item.maxPercentLimit)
            }
            array.put(obj)
        }

        prefs.edit {
            putString(keyDeductionsJson, array.toString())
        }
    }

    fun addOrUpdate(item: PayrollDeduction) {
        val current = loadFromPrefs().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        val saved = current
            .sortedWith(compareBy<PayrollDeduction> { it.priority }.thenBy { it.title.lowercase() })
        saveToPrefs(saved)
        _deductionsFlow.value = saved
    }

    fun deleteById(id: String) {
        val current = loadFromPrefs().filterNot { it.id == id }
        saveToPrefs(current)
        _deductionsFlow.value = current
    }

    @Suppress("unused")
    fun replaceAll(items: List<PayrollDeduction>) {
        val saved = items.sortedWith(compareBy<PayrollDeduction> { it.priority }.thenBy { it.title.lowercase() })
        saveToPrefs(saved)
        _deductionsFlow.value = saved
    }

    fun setActive(id: String, active: Boolean) {
        val current = loadFromPrefs().map { item ->
            if (item.id == id) item.copy(active = active) else item
        }
        saveToPrefs(current)
        _deductionsFlow.value = current
    }
}
