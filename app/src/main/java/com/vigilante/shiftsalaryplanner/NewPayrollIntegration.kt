package com.vigilante.shiftsalaryplanner

import android.util.Log
import android.widget.Toast
import com.vigilante.shiftsalaryplanner.payroll.calculators.*
import com.vigilante.shiftsalaryplanner.payroll.datastore.DefaultAccrualConfig
import com.vigilante.shiftsalaryplanner.payroll.datastore.PayrollSettingsRepository
import com.vigilante.shiftsalaryplanner.payroll.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Интеграция новой системы расчёта зарплаты
 */
class NewPayrollIntegration(
    private val context: android.content.Context,
    private val coroutineScope: CoroutineScope,
    private val database: com.vigilante.shiftsalaryplanner.data.AppDatabase
) {
    private val repository = PayrollSettingsRepository(context)
    private val TAG = "NewPayrollIntegration"

    fun calculateAndShow(currentDate: LocalDate = LocalDate.now()) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Начало расчёта...")

                // Проверяем/инициализируем настройки если их нет
                var settings = try {
                    repository.getSettings()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка чтения настроек, используем дефолтные", e)
                    DefaultAccrualConfig.getDefaultSettings()
                }

                Log.d(TAG, "Настроек загружено: ${settings.size}")

                // Получаем ВСЕ смены
                val allShifts = database.shiftDayDao().observeAll().first()
                Log.d(TAG, "Всего смен в базе: ${allShifts.size}")

                // Фильтруем нужный месяц
                val yearMonth = YearMonth.of(currentDate.year, currentDate.month)
                val monthShifts = allShifts.filter { shift ->
                    try {
                        val shiftDate = LocalDate.parse(shift.date)
                        YearMonth.from(shiftDate) == yearMonth
                    } catch (e: Exception) {
                        false
                    }
                }
                Log.d(TAG, "Смен в месяце: ${monthShifts.size}")

                // Конвертируем
                val shiftDataList = monthShifts.map { shift ->
                    val isVacation = shift.shiftCode.uppercase() in setOf("ОТ", "OT", "ОТПУСК", "VACATION")
                    val isSick = shift.shiftCode.uppercase() in setOf("Б", "B", "БЛ", "БОЛЬНИЧНЫЙ", "SICK")
                    val isDayOff = shift.shiftCode.uppercase() in setOf("ВЫХ", "В", "ВЫХОДНОЙ", "DAYOFF")

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

                Log.d(TAG, "Запускаем движок расчёта...")

                // Создаём движок и считаем
                val engine = PayrollEngine(settings)
                val result = engine.calculateMonthPreview(shiftDataList, yearMonth)

                Log.d(TAG, "Расчёт завершён: аванс=${result.advance.net}, основная=${result.mainSalary.net}")

                // Формируем сообщение
                val message = buildString {
                    appendLine("📊 Новый расчёт ${currentDate.monthValue}/${currentDate.year}")
                    appendLine()
                    appendLine("💰 Аванс: ${String.format("%,.2f", result.advance.net)} ₽")
                    appendLine("💵 Основная: ${String.format("%,.2f", result.mainSalary.net)} ₽")
                    appendLine("📈 Всего: ${String.format("%,.2f", result.totalNet)} ₽")
                }

                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e(TAG, "Критическая ошибка", e)
                e.printStackTrace()
                val errorMsg = "❌ Ошибка: ${e.javaClass.simpleName}: ${e.message?.take(50)}"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}