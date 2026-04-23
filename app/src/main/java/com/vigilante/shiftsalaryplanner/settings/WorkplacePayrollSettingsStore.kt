package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.payroll.NightHoursBaseMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

const val PREF_NAME_WORKPLACE_PAYROLL_SETTINGS = "workplace_payroll_settings"

data class WorkplacePayrollSettingsState(
    val settingsByWorkplaceId: Map<String, PayrollSettings>
)

class WorkplacePayrollSettingsStore(context: Context) {

    private val prefs = context.profileSharedPreferences(PREF_NAME_WORKPLACE_PAYROLL_SETTINGS)
    private val legacyPrefs = context.profileSharedPreferences("workplace_payroll_salaries")
    private val _stateFlow = MutableStateFlow(load())
    val stateFlow: Flow<WorkplacePayrollSettingsState> = _stateFlow.asStateFlow()

    fun save(state: WorkplacePayrollSettingsState) {
        val normalized = state.settingsByWorkplaceId
            .filterKeys { it.isNotBlank() && it != WORKPLACE_MAIN_ID }

        prefs.edit {
            putString(KEY_SETTINGS_JSON, serialize(normalized))
        }
        _stateFlow.value = WorkplacePayrollSettingsState(normalized)
    }

    private fun load(): WorkplacePayrollSettingsState {
        val parsed = parse(prefs.getString(KEY_SETTINGS_JSON, null))
        if (parsed.isNotEmpty()) {
            return WorkplacePayrollSettingsState(settingsByWorkplaceId = parsed)
        }

        val migrated = parseLegacy(legacyPrefs.getString(KEY_LEGACY_SALARIES_JSON, null))
        if (migrated.isNotEmpty()) {
            prefs.edit {
                putString(KEY_SETTINGS_JSON, serialize(migrated))
            }
            return WorkplacePayrollSettingsState(settingsByWorkplaceId = migrated)
        }

        return WorkplacePayrollSettingsState(settingsByWorkplaceId = parsed)
    }

    private fun parse(raw: String?): Map<String, PayrollSettings> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val workplaceId = keys.next().trim()
                    if (workplaceId.isBlank() || workplaceId == WORKPLACE_MAIN_ID) continue
                    val item = root.optJSONObject(workplaceId) ?: continue
                    put(workplaceId, parsePayrollSettings(item))
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun serialize(settingsByWorkplaceId: Map<String, PayrollSettings>): String {
        val root = JSONObject()
        settingsByWorkplaceId.toSortedMap().forEach { (workplaceId, settings) ->
            root.put(workplaceId, serializePayrollSettings(settings))
        }
        return root.toString()
    }

    private fun parseLegacy(raw: String?): Map<String, PayrollSettings> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val workplaceId = keys.next().trim()
                    if (workplaceId.isBlank() || workplaceId == WORKPLACE_MAIN_ID) continue
                    val item = root.optJSONObject(workplaceId) ?: continue
                    put(
                        workplaceId,
                        PayrollSettings(
                            baseSalary = item.optDouble("baseSalary", 0.0).coerceAtLeast(0.0),
                            extraSalary = item.optDouble("extraSalary", 0.0).coerceAtLeast(0.0)
                        )
                    )
                }
            }
        }.getOrElse { emptyMap() }
    }

    private fun parsePayrollSettings(source: JSONObject): PayrollSettings {
        return PayrollSettings(
            baseSalary = source.optDouble("baseSalary", 0.0),
            extraSalary = source.optDouble("extraSalary", 0.0),
            housingPayment = source.optDouble("housingPayment", 0.0),
            housingPaymentLabel = source.optString("housingPaymentLabel", "Выплата на квартиру"),
            housingPaymentTaxable = source.optBoolean("housingPaymentTaxable", true),
            housingPaymentWithAdvance = source.optBoolean("housingPaymentWithAdvance", false),
            monthlyNormHours = source.optDouble("monthlyNormHours", 165.0),
            workdayHours = source.optDouble("workdayHours", 8.0),
            annualNormSourceMode = source.optString("annualNormSourceMode", "WORKDAY_HOURS"),
            annualNormHours = source.optDouble("annualNormHours", 1970.0),
            normMode = source.optString("normMode", "MANUAL"),
            payMode = source.optString("payMode", "HOURLY"),
            extraSalaryMode = source.optString("extraSalaryMode", "INCLUDED_IN_RATE"),
            advanceMode = source.optString("advanceMode", "ACTUAL_EARNINGS"),
            advancePercent = source.optDouble("advancePercent", 50.0),
            applyShortDayReduction = source.optBoolean("applyShortDayReduction", true),
            nightPercent = source.optDouble("nightPercent", 0.4),
            nightHoursBaseMode = source.optString(
                "nightHoursBaseMode",
                NightHoursBaseMode.FOLLOW_HOURLY_RATE.name
            ),
            holidayRateMultiplier = source.optDouble("holidayRateMultiplier", 2.0),
            ndflPercent = source.optDouble("ndflPercent", 0.13),
            vacationAverageDaily = source.optDouble("vacationAverageDaily", 0.0),
            vacationAccruals12Months = source.optDouble("vacationAccruals12Months", 0.0),
            sickAverageDaily = source.optDouble("sickAverageDaily", 0.0),
            sickIncomeYear1 = source.optDouble("sickIncomeYear1", 0.0),
            sickIncomeYear2 = source.optDouble("sickIncomeYear2", 0.0),
            sickLimitYear1 = source.optDouble("sickLimitYear1", 0.0),
            sickLimitYear2 = source.optDouble("sickLimitYear2", 0.0),
            sickCalculationPeriodDays = source.optInt("sickCalculationPeriodDays", 730),
            sickExcludedDays = source.optInt("sickExcludedDays", 0),
            sickPayPercent = source.optDouble("sickPayPercent", 1.0),
            sickMaxDailyAmount = source.optDouble("sickMaxDailyAmount", 6827.40),
            progressiveNdflEnabled = source.optBoolean("progressiveNdflEnabled", false),
            taxableIncomeYtdBeforeCurrentMonth = source.optDouble("taxableIncomeYtdBeforeCurrentMonth", 0.0),
            advanceDay = source.optInt("advanceDay", 20),
            salaryDay = source.optInt("salaryDay", 5),
            movePaymentsToPreviousWorkday = source.optBoolean("movePaymentsToPreviousWorkday", true),
            overtimeEnabled = source.optBoolean("overtimeEnabled", true),
            overtimePeriod = source.optString("overtimePeriod", "YEAR"),
            excludeWeekendHolidayFromOvertime = source.optBoolean("excludeWeekendHolidayFromOvertime", true),
            excludeRvdDoublePayFromOvertime = source.optBoolean("excludeRvdDoublePayFromOvertime", true),
            excludeRvdSingleWithDayOffFromOvertime = source.optBoolean("excludeRvdSingleWithDayOffFromOvertime", false)
        )
    }

    private fun serializePayrollSettings(settings: PayrollSettings): JSONObject {
        return JSONObject().apply {
            put("baseSalary", settings.baseSalary)
            put("extraSalary", settings.extraSalary)
            put("housingPayment", settings.housingPayment)
            put("housingPaymentLabel", settings.housingPaymentLabel)
            put("housingPaymentTaxable", settings.housingPaymentTaxable)
            put("housingPaymentWithAdvance", settings.housingPaymentWithAdvance)
            put("monthlyNormHours", settings.monthlyNormHours)
            put("workdayHours", settings.workdayHours)
            put("annualNormSourceMode", settings.annualNormSourceMode)
            put("annualNormHours", settings.annualNormHours)
            put("normMode", settings.normMode)
            put("payMode", settings.payMode)
            put("extraSalaryMode", settings.extraSalaryMode)
            put("advanceMode", settings.advanceMode)
            put("advancePercent", settings.advancePercent)
            put("applyShortDayReduction", settings.applyShortDayReduction)
            put("nightPercent", settings.nightPercent)
            put("nightHoursBaseMode", settings.nightHoursBaseMode)
            put("holidayRateMultiplier", settings.holidayRateMultiplier)
            put("ndflPercent", settings.ndflPercent)
            put("vacationAverageDaily", settings.vacationAverageDaily)
            put("vacationAccruals12Months", settings.vacationAccruals12Months)
            put("sickAverageDaily", settings.sickAverageDaily)
            put("sickIncomeYear1", settings.sickIncomeYear1)
            put("sickIncomeYear2", settings.sickIncomeYear2)
            put("sickLimitYear1", settings.sickLimitYear1)
            put("sickLimitYear2", settings.sickLimitYear2)
            put("sickCalculationPeriodDays", settings.sickCalculationPeriodDays)
            put("sickExcludedDays", settings.sickExcludedDays)
            put("sickPayPercent", settings.sickPayPercent)
            put("sickMaxDailyAmount", settings.sickMaxDailyAmount)
            put("progressiveNdflEnabled", settings.progressiveNdflEnabled)
            put("taxableIncomeYtdBeforeCurrentMonth", settings.taxableIncomeYtdBeforeCurrentMonth)
            put("advanceDay", settings.advanceDay)
            put("salaryDay", settings.salaryDay)
            put("movePaymentsToPreviousWorkday", settings.movePaymentsToPreviousWorkday)
            put("overtimeEnabled", settings.overtimeEnabled)
            put("overtimePeriod", settings.overtimePeriod)
            put("excludeWeekendHolidayFromOvertime", settings.excludeWeekendHolidayFromOvertime)
            put("excludeRvdDoublePayFromOvertime", settings.excludeRvdDoublePayFromOvertime)
            put("excludeRvdSingleWithDayOffFromOvertime", settings.excludeRvdSingleWithDayOffFromOvertime)
        }
    }

    private companion object {
        private const val KEY_SETTINGS_JSON = "settings_json"
        private const val KEY_LEGACY_SALARIES_JSON = "salaries_json"
    }
}
