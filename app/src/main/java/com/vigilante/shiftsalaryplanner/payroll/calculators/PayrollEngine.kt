package com.vigilante.shiftsalaryplanner.payroll.calculators

import com.vigilante.shiftsalaryplanner.payroll.models.*
import java.time.YearMonth
import java.time.temporal.IsoFields

/**
 * Главный движок расчёта зарплаты
 * Объединяет все калькуляторы и производит полный расчёт месяца
 * Заменяет хаос в MainActivity на чёткую структуру
 */
class PayrollEngine(
    private val accrualSettings: List<AccrualSettings>
) {

    /**
     * Полный результат расчёта месяца
     */
    data class MonthlyPayrollResult(
        // Аванс (1-15)
        val advance: AdvanceCalculator.AdvanceResult,

        // Основная зарплата (16-31 + все начисления)
        val mainSalary: MainSalaryResult,

        // Итоги
        val totalGross: Double,           // Всего начислено
        val totalTaxable: Double,         // Облагаемая база
        val totalNdfl: Double,            // НДФЛ (аванс + основная)
        val totalNet: Double,             // На руки

        // Детализация по кодам начислений (для "Зарплатной ведомости")
        val accrualDetails: List<AccrualDetail>,

        // Справочно
        val shiftsTotal: Int,
        val hoursTotal: Double,
        val vacationDays: Int,
        val sickDays: Int
    )

    data class MainSalaryResult(
        val gross: Double,
        val taxable: Double,
        val ndfl: Double,
        val net: Double,
        val details: List<AdvanceCalculator.AdvanceDetail>
    )

    data class AccrualDetail(
        val code: String,
        val name: String,
        val amount: Double,
        val taxable: Boolean,
        val withAdvance: Boolean,  // В авансе или в основной
        val category: String       // Группировка в ведомости
    )

    /**
     * Основной метод расчёта за месяц
     *
     * @param allShifts Все смены месяца
     * @param targetMonth Целевой месяц (например, YearMonth.of(2026, 3) для марта)
     * @param allShiftsInQuarter Все смены квартала (для премии, опционально)
     */
    fun calculateMonth(
        allShifts: List<ShiftData>,
        targetMonth: YearMonth,
        allShiftsInQuarter: List<ShiftData> = emptyList()
    ): MonthlyPayrollResult {

        // 1. РАСЧЁТ АВАНСА (1-15 числа)
        val advanceSettings = accrualSettings.filter { it.withAdvance }
        val advance = AdvanceCalculator.calculateAdvance(
            allShifts = allShifts,
            accrualSettings = advanceSettings,
            targetMonth = targetMonth
        )

        // 2. РАСЧЁТ ОСНОВНОЙ ЗАРПЛАТЫ (все смены месяца)
        val mainDetails = mutableListOf<AdvanceCalculator.AdvanceDetail>()
        var mainGross = 0.0
        var mainTaxable = 0.0

        // Статистика
        val workedShifts = allShifts.filter { !it.isVacation && !it.isSick && !it.isDayOff }
        val vacationDays = allShifts.count { it.isVacation }
        val sickDays = allShifts.count { it.isSick }

        // 2.1 Основной оклад (0010)
        accrualSettings.find { it.code == AccrualCode.BASE_SALARY_0010 && it.isEnabled }?.let { settings ->
            val amount = BaseSalaryCalculator.calculate(allShifts, settings)
            if (amount > 0) {
                mainDetails.add(AdvanceCalculator.AdvanceDetail(
                    accrualName = settings.displayName,
                    accrualCode = settings.code.code,
                    amount = amount,
                    isTaxable = settings.taxable,
                    calculationDescription = "Основной оклад"
                ))
                mainGross += amount
                if (settings.taxable) mainTaxable += amount
            }
        }

        // 2.2 Интегральная надбавка (1010)
        accrualSettings.find { it.code == AccrualCode.INTERSTIM_1010 && it.isEnabled }?.let { settings ->
            val result = InterstimCalculator.calculate(allShifts, settings)
            if (result.gross > 0) {
                mainDetails.add(AdvanceCalculator.AdvanceDetail(
                    accrualName = settings.displayName,
                    accrualCode = settings.code.code,
                    amount = result.gross,
                    isTaxable = settings.taxable,
                    calculationDescription = "Интегральная: ${result.hoursWorked}ч"
                ))
                mainGross += result.gross
                if (settings.taxable) mainTaxable += result.gross
            }
        }

        // 2.3 Индексирующая выплата (1024)
        accrualSettings.find { it.code == AccrualCode.INDEXATION_1024 && it.isEnabled }?.let { settings ->
            val result = InterstimCalculator.calculate(allShifts, settings)
            if (result.gross > 0) {
                mainDetails.add(AdvanceCalculator.AdvanceDetail(
                    accrualName = settings.displayName,
                    accrualCode = settings.code.code,
                    amount = result.gross,
                    isTaxable = settings.taxable,
                    calculationDescription = "Индексация: ${result.hoursWorked}ч"
                ))
                mainGross += result.gross
                if (settings.taxable) mainTaxable += result.gross
            }
        }

        // 2.4 Ночные и праздники (0212, 0233)
        val nightSettings = accrualSettings.find { it.code == AccrualCode.NIGHT_EXTRA_0212 }
        val holidaySettings = accrualSettings.find { it.code == AccrualCode.HOLIDAY_EXTRA_0233 }
        val nhResult = NightAndHolidayCalculator.calculate(allShifts, nightSettings, holidaySettings)

        if (nhResult.nightAmount > 0 && nightSettings != null) {
            mainDetails.add(AdvanceCalculator.AdvanceDetail(
                accrualName = nightSettings.displayName,
                accrualCode = nightSettings.code.code,
                amount = nhResult.nightAmount,
                isTaxable = nightSettings.taxable,
                calculationDescription = "Ночные часы: ${nhResult.nightHours}ч"
            ))
            mainGross += nhResult.nightAmount
            if (nightSettings.taxable) mainTaxable += nhResult.nightAmount
        }

        if (nhResult.holidayAmount > 0 && holidaySettings != null) {
            mainDetails.add(AdvanceCalculator.AdvanceDetail(
                accrualName = holidaySettings.displayName,
                accrualCode = holidaySettings.code.code,
                amount = nhResult.holidayAmount,
                isTaxable = holidaySettings.taxable,
                calculationDescription = "Праздничные часы: ${nhResult.holidayHours}ч"
            ))
            mainGross += nhResult.holidayAmount
            if (holidaySettings.taxable) mainTaxable += nhResult.holidayAmount
        }

        // 2.5 Квартальная премия (1273)
        val premiumSettings = accrualSettings.find {
            it.code == AccrualCode.PREMIUM_1273 && it.isEnabled
        }
        if (premiumSettings != null && allShiftsInQuarter.isNotEmpty()) {
            val premiumResult = PremiumCalculator.calculate(
                currentMonth = targetMonth,
                allShiftsInQuarter = allShiftsInQuarter,
                settings = premiumSettings
            )

            if (premiumResult.isPaymentMonth && premiumResult.gross > 0) {
                mainDetails.add(AdvanceCalculator.AdvanceDetail(
                    accrualName = premiumSettings.displayName,
                    accrualCode = premiumSettings.code.code,
                    amount = premiumResult.gross,
                    isTaxable = premiumSettings.taxable,
                    calculationDescription = premiumResult.calculationDescription
                ))
                mainGross += premiumResult.gross
                if (premiumSettings.taxable) mainTaxable += premiumResult.gross
            }
        }

        // 2.6 Компенсация жилья (4433) - фиксированная, обычно в основную
        accrualSettings.find { it.code == AccrualCode.HOUSING_4433 && it.isEnabled }?.let { settings ->
            val amount = settings.baseAmount // Фиксированная
            mainDetails.add(AdvanceCalculator.AdvanceDetail(
                accrualName = settings.displayName,
                accrualCode = settings.code.code,
                amount = amount,
                isTaxable = settings.taxable,
                calculationDescription = "Компенсация аренды жилья"
            ))
            mainGross += amount
            if (settings.taxable) mainTaxable += amount
        }

        // 3. РАСЧЁТ НДФЛ ДЛЯ ОСНОВНОЙ ЗАРПЛАТЫ
        // Важно: НДФЛ считается отдельно для аванса и отдельно для основной!
        val mainNdfl = mainTaxable * 0.13 // Упрощённо 13%, позже добавим прогрессивный
        val mainNet = mainGross - mainNdfl

        val mainSalary = MainSalaryResult(
            gross = mainGross,
            taxable = mainTaxable,
            ndfl = mainNdfl,
            net = mainNet,
            details = mainDetails
        )

        // 4. ИТОГОВЫЕ СУММЫ
        val totalGross = advance.gross + mainSalary.gross
        val totalNdfl = advance.ndfl + mainSalary.ndfl
        val totalNet = advance.net + mainSalary.net

        // 5. СОЗДАНИЕ ДЕТАЛИЗАЦИИ ДЛЯ "ЗАРПЛАТНОЙ ВЕДОМОСТИ"
        val accrualDetails = buildAccrualDetails(advance, mainSalary)

        return MonthlyPayrollResult(
            advance = advance,
            mainSalary = mainSalary,
            totalGross = totalGross,
            totalTaxable = advance.taxable + mainSalary.taxable,
            totalNdfl = totalNdfl,
            totalNet = totalNet,
            accrualDetails = accrualDetails,
            shiftsTotal = workedShifts.size,
            hoursTotal = workedShifts.sumOf { it.hours },
            vacationDays = vacationDays,
            sickDays = sickDays
        )
    }

    /**
     * Создаёт детализацию для отображения в "Зарплатной ведомости"
     * Группирует по категориям: Начисления → Удержания → К выплате
     */
    private fun buildAccrualDetails(
        advance: AdvanceCalculator.AdvanceResult,
        mainSalary: MainSalaryResult
    ): List<AccrualDetail> {
        val details = mutableListOf<AccrualDetail>()

        // НАЧИСЛЕНИЯ (группируем по кодам)
        // Авансовые начисления (уже выплачены как аванс)
        advance.details.forEach { adv ->
            details.add(AccrualDetail(
                code = adv.accrualCode,
                name = adv.accrualName,
                amount = adv.amount,
                taxable = adv.isTaxable,
                withAdvance = true,
                category = "Начисления (в авансе)"
            ))
        }

        // Основные начисления
        mainSalary.details.forEach { main ->
            details.add(AccrualDetail(
                code = main.accrualCode,
                name = main.accrualName,
                amount = main.amount,
                taxable = main.isTaxable,
                withAdvance = false,
                category = "Начисления (основные)"
            ))
        }

        // УДЕРЖАНИЯ
        details.add(AccrualDetail(
            code = "НДФЛ",
            name = "НДФЛ общий (13%)",
            amount = advance.ndfl + mainSalary.ndfl,
            taxable = false,
            withAdvance = false,
            category = "Удержания"
        ))

        // К ВЫПЛАТЕ
        details.add(AccrualDetail(
            code = "ИТОГО",
            name = "Сумма к выплате",
            amount = mainSalary.net, // Только основная, аванс уже выплачен
            taxable = false,
            withAdvance = false,
            category = "К выплате"
        ))

        return details
    }

    /**
     * Упрощённый расчёт только для предпросмотра (без квартальных данных)
     */
    fun calculateMonthPreview(
        allShifts: List<ShiftData>,
        targetMonth: YearMonth
    ): MonthlyPayrollResult {
        return calculateMonth(allShifts, targetMonth, emptyList())
    }
}