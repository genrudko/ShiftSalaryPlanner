package com.vigilante.shiftsalaryplanner.payroll

import android.util.Log
import androidx.core.content.edit
import com.vigilante.shiftsalaryplanner.formatMoney
import com.vigilante.shiftsalaryplanner.payroll.calculators.*
import com.vigilante.shiftsalaryplanner.payroll.datastore.DefaultAccrualConfig
import com.vigilante.shiftsalaryplanner.payroll.datastore.PayrollSettingsRepository
import com.vigilante.shiftsalaryplanner.payroll.models.*
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields

class NewPayrollIntegration(
    private val context: android.content.Context,
    private val coroutineScope: CoroutineScope,
    private val database: com.vigilante.shiftsalaryplanner.data.AppDatabase
) {
    private val repository = PayrollSettingsRepository(context)
    private val TAG = "NewPayrollIntegration"

    // Простой метод без сложных generic-типов
    fun calculateSimple(
        currentDate: LocalDate = LocalDate.now(),
        onResult: (gross: Double, advanceNet: Double, mainNet: Double, totalNet: Double, error: String?) -> Unit
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Начало расчёта...")

                var settings = try {
                    repository.getSettings()
                } catch (e: Exception) {
                    DefaultAccrualConfig.getDefaultSettings()
                }

                val allShifts = database.shiftDayDao().observeAll().first()
                val yearMonth = YearMonth.of(currentDate.year, currentDate.month)

                val monthShifts = allShifts.filter { shift ->
                    try {
                        val shiftDate = LocalDate.parse(shift.date)
                        YearMonth.from(shiftDate) == yearMonth
                    } catch (e: Exception) {
                        false
                    }
                }

                // Квартальные смены для премии
                val currentQuarter = yearMonth.get(IsoFields.QUARTER_OF_YEAR)
                val quarterShifts = allShifts.filter { shift ->
                    try {
                        val shiftDate = LocalDate.parse(shift.date)
                        val shiftQuarter = YearMonth.from(shiftDate).get(IsoFields.QUARTER_OF_YEAR)
                        shiftQuarter == currentQuarter && shiftDate.year == yearMonth.year
                    } catch (e: Exception) {
                        false
                    }
                }

                val shiftDataList = monthShifts.map { shift ->
                    val isVacation = shift.shiftCode.uppercase() in setOf("ОТ", "OT", "ОТПУСК")
                    val isSick = shift.shiftCode.uppercase() in setOf("Б", "B", "БЛ")
                    val isDayOff = shift.shiftCode.uppercase() in setOf("ВЫХ", "В", "DAYOFF")

                    ShiftData(
                        date = LocalDate.parse(shift.date),
                        shiftCode = shift.shiftCode,
                        hours = 11.0,
                        nightHours = 0.0,
                        isHoliday = false,
                        isVacation = isVacation,
                        isSick = isSick,
                        isDayOff = isDayOff
                    )
                }

                val quarterShiftData = quarterShifts.map { shift ->
                    ShiftData(
                        date = LocalDate.parse(shift.date),
                        shiftCode = shift.shiftCode,
                        hours = 11.0,
                        nightHours = 0.0,
                        isHoliday = false,
                        isVacation = shift.shiftCode.uppercase() in setOf("ОТ", "OT"),
                        isSick = shift.shiftCode.uppercase() in setOf("Б", "B"),
                        isDayOff = shift.shiftCode.uppercase() in setOf("ВЫХ", "В")
                    )
                }

                val engine = PayrollEngine(settings)
                val result = engine.calculateMonthPreview(shiftDataList, yearMonth)

// Убираем yearToDate логику пока нет полей в PayrollEngine
// saveYearToDate(yearMonth.year, result.yearToDateTaxableAfter)

                onResult(
                    result.totalGross,
                    result.advance.net,
                    result.mainSalary.net,
                    result.totalNet,
                    null
                )

                // Вызываем callback с простыми типами
                onResult(
                    result.totalGross,
                    result.advance.net,
                    result.mainSalary.net,
                    result.totalNet,
                    null
                )

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                onResult(0.0, 0.0, 0.0, 0.0, e.message)
            }
        }
    }

    private fun loadYearToDate(year: Int): Double {
        val prefs = context.profileSharedPreferences("payroll_ytd")
        return prefs.getFloat("ytd_$year", 0.0f).toDouble()
    }

    private fun saveYearToDate(year: Int, value: Double) {
        val prefs = context.profileSharedPreferences("payroll_ytd")
        prefs.edit { putFloat("ytd_$year", value.toFloat()) }
    }

    // Старый метод для совместимости
    fun calculateAndShow(currentDate: LocalDate = LocalDate.now()) {
        calculateSimple(currentDate) { gross, advance, main, net, error ->
            if (error != null) {
                android.widget.Toast.makeText(context, "Ошибка: $error", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Аванс: ${formatMoney(advance)}\nОсновная: ${formatMoney(main)}\nВсего: ${formatMoney(net)}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
