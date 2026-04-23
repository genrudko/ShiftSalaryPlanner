package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.PaymentDistribution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AdditionalPaymentsStore(context: Context) {
    private val prefs = context.profileSharedPreferences("additional_payments")
    private val keyPaymentsJson = "payments_json"
    private val _paymentsFlow = MutableStateFlow(loadFromPrefs())

    val paymentsFlow: Flow<List<AdditionalPayment>> = _paymentsFlow.asStateFlow()

    private fun loadFromPrefs(): List<AdditionalPayment> {
        return try {
            val raw = prefs.getString(keyPaymentsJson, "[]") ?: "[]"
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val withAdvance = obj.optBoolean("withAdvance", false)
                    add(
                        AdditionalPayment(
                            id = obj.optString("id"),
                            workplaceId = obj.optString("workplaceId", "work_main"),
                            name = obj.optString("name"),
                            amount = obj.optDouble("amount", 0.0),
                            taxable = obj.optBoolean("taxable", true),
                            withAdvance = withAdvance,
                            active = obj.optBoolean("active", true),
                            type = obj.optString("type", "MONTHLY"),
                            premiumPeriod = obj.optString("premiumPeriod", "MONTHLY"),
                            targetMonth = obj.optString("targetMonth", ""),
                            delayMonths = obj.optInt("delayMonths", 0),
                            includeInShiftCost = obj.optBoolean("includeInShiftCost", true),
                            distribution = obj.optString(
                                "distribution",
                                if (withAdvance) PaymentDistribution.ADVANCE.name else PaymentDistribution.SALARY.name
                            )
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(items: List<AdditionalPayment>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("workplaceId", item.workplaceId)
                put("name", item.name)
                put("amount", item.amount)
                put("taxable", item.taxable)
                put("withAdvance", item.withAdvance)
                put("active", item.active)
                put("type", item.type)
                put("premiumPeriod", item.premiumPeriod)
                put("targetMonth", item.targetMonth)
                put("delayMonths", item.delayMonths)
                put("includeInShiftCost", item.includeInShiftCost)
                put("distribution", item.distribution)
            }
            array.put(obj)
        }

        prefs.edit {
            putString(keyPaymentsJson, array.toString())
        }
    }

    fun addOrUpdate(item: AdditionalPayment) {
        val current = loadFromPrefs().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        saveToPrefs(current)
        _paymentsFlow.value = loadFromPrefs()
    }

    fun deleteById(id: String) {
        val current = loadFromPrefs().filterNot { it.id == id }
        saveToPrefs(current)
        _paymentsFlow.value = loadFromPrefs()
    }
}
