package com.vigilante.shiftsalaryplanner

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PayrollSettingsDialog(
    currentSettings: PayrollSettings,
    onDismiss: () -> Unit,
    onSave: (PayrollSettings) -> Unit
) {
    val context = LocalContext.current
    val sickLimitsPrefs = remember {
        context.getSharedPreferences(PREFS_SICK_LIMITS_CACHE, Context.MODE_PRIVATE)
    }
    var baseSalaryText by rememberSaveable { mutableStateOf(currentSettings.baseSalary.toPlainString()) }
    var extraSalaryText by rememberSaveable { mutableStateOf(currentSettings.extraSalary.toPlainString()) }
    var housingPaymentLabelText by rememberSaveable {
        mutableStateOf(displayHousingPaymentLabel(currentSettings.housingPaymentLabel))
    }
    var housingPaymentText by rememberSaveable { mutableStateOf(currentSettings.housingPayment.toPlainString()) }
    var payModeName by rememberSaveable { mutableStateOf(currentSettings.payMode.ifBlank { PayMode.HOURLY.name }) }
    var extraSalaryModeName by rememberSaveable { mutableStateOf(currentSettings.extraSalaryMode.ifBlank { ExtraSalaryMode.INCLUDED_IN_RATE.name }) }
    var normModeName by rememberSaveable { mutableStateOf(currentSettings.normMode.ifBlank { NormMode.MANUAL.name }) }
    var monthlyNormHoursText by rememberSaveable { mutableStateOf(currentSettings.monthlyNormHours.toPlainString()) }
    var workdayHoursText by rememberSaveable { mutableStateOf(currentSettings.workdayHours.toPlainString()) }
    var annualNormSourceModeName by rememberSaveable {
        mutableStateOf(currentSettings.annualNormSourceMode.ifBlank { AnnualNormSourceMode.WORKDAY_HOURS.name })
    }
    var annualNormHoursText by rememberSaveable { mutableStateOf(currentSettings.annualNormHours.toPlainString()) }
    var nightPercentText by rememberSaveable { mutableStateOf(currentSettings.nightPercent.toPlainString()) }
    var holidayRateMultiplierText by rememberSaveable { mutableStateOf(currentSettings.holidayRateMultiplier.toPlainString()) }
    var ndflPercentText by rememberSaveable { mutableStateOf(currentSettings.ndflPercent.toPlainString()) }
    var vacationAccruals12MonthsText by rememberSaveable { mutableStateOf(currentSettings.vacationAccruals12Months.toPlainString()) }
    var sickIncomeYear1Text by rememberSaveable { mutableStateOf(currentSettings.sickIncomeYear1.toPlainString()) }
    var sickIncomeYear2Text by rememberSaveable { mutableStateOf(currentSettings.sickIncomeYear2.toPlainString()) }
    var sickLimitYear1Text by rememberSaveable { mutableStateOf(currentSettings.sickLimitYear1.toPlainString()) }
    var sickLimitYear2Text by rememberSaveable { mutableStateOf(currentSettings.sickLimitYear2.toPlainString()) }
    var sickExcludedDaysText by rememberSaveable { mutableStateOf(currentSettings.sickExcludedDays.toString()) }
    var sickPayPercentText by rememberSaveable { mutableStateOf(currentSettings.sickPayPercent.toPlainString()) }
    var sickMaxDailyAmountText by rememberSaveable { mutableStateOf(currentSettings.sickMaxDailyAmount.toPlainString()) }
    var progressiveNdflEnabled by rememberSaveable { mutableStateOf(currentSettings.progressiveNdflEnabled) }
    var taxableIncomeYtdText by rememberSaveable { mutableStateOf(currentSettings.taxableIncomeYtdBeforeCurrentMonth.toPlainString()) }
    var advanceModeName by rememberSaveable { mutableStateOf(currentSettings.advanceMode.ifBlank { AdvanceMode.ACTUAL_EARNINGS.name }) }
    var advancePercentText by rememberSaveable { mutableStateOf(currentSettings.advancePercent.toPlainString()) }
    var advanceDayText by rememberSaveable { mutableStateOf(currentSettings.advanceDay.toString()) }
    var salaryDayText by rememberSaveable { mutableStateOf(currentSettings.salaryDay.toString()) }
    var movePaymentsToPreviousWorkday by rememberSaveable {
        mutableStateOf(currentSettings.movePaymentsToPreviousWorkday)
    }
    var housingPaymentTaxable by rememberSaveable {
        mutableStateOf(currentSettings.housingPaymentTaxable)
    }
    var housingPaymentWithAdvance by rememberSaveable {
        mutableStateOf(currentSettings.housingPaymentWithAdvance)
    }
    var applyShortDayReduction by rememberSaveable {
        mutableStateOf(currentSettings.applyShortDayReduction)
    }
    var overtimeEnabled by rememberSaveable { mutableStateOf(currentSettings.overtimeEnabled) }
    var overtimePeriodName by rememberSaveable {
        mutableStateOf(currentSettings.overtimePeriod.ifBlank { OvertimePeriod.YEAR.name })
    }
    var excludeWeekendHolidayFromOvertime by rememberSaveable {
        mutableStateOf(currentSettings.excludeWeekendHolidayFromOvertime)
    }
    var excludeRvdDoublePayFromOvertime by rememberSaveable {
        mutableStateOf(currentSettings.excludeRvdDoublePayFromOvertime)
    }
    var excludeRvdSingleWithDayOffFromOvertime by rememberSaveable {
        mutableStateOf(currentSettings.excludeRvdSingleWithDayOffFromOvertime)
    }
    var showLeaveBenefitsSettings by rememberSaveable { mutableStateOf(false) }
    var isSickLimitsLoading by rememberSaveable { mutableStateOf(false) }
    var sickLimitsMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val payMode = runCatching { PayMode.valueOf(payModeName) }.getOrElse { PayMode.HOURLY }
    val extraSalaryMode = runCatching { ExtraSalaryMode.valueOf(extraSalaryModeName) }.getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }
    val normMode = runCatching { NormMode.valueOf(normModeName) }.getOrElse { NormMode.MANUAL }
    val annualNormSourceMode = runCatching { AnnualNormSourceMode.valueOf(annualNormSourceModeName) }
        .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    val advanceMode = runCatching { AdvanceMode.valueOf(advanceModeName) }.getOrElse { AdvanceMode.ACTUAL_EARNINGS }
    val overtimePeriod = runCatching { OvertimePeriod.valueOf(overtimePeriodName) }.getOrElse { OvertimePeriod.YEAR }
    val dialogScope = rememberCoroutineScope()
    val benefitReferenceYear = remember { LocalDate.now().year }
    val sickYear1 = benefitReferenceYear - 2
    val sickYear2 = benefitReferenceYear - 1

    LaunchedEffect(sickYear1, sickYear2) {
        val cachedLimits = readCachedSickInsuranceBaseLimits(sickLimitsPrefs, sickYear1, sickYear2)
        if (cachedLimits.isNotEmpty()) {
            val currentLimitYear1 = parseDouble(sickLimitYear1Text, currentSettings.sickLimitYear1)
            val currentLimitYear2 = parseDouble(sickLimitYear2Text, currentSettings.sickLimitYear2)

            if (currentLimitYear1 <= 0.0) {
                cachedLimits[sickYear1]?.let { sickLimitYear1Text = formatWholeNumber(it) }
            }
            if (currentLimitYear2 <= 0.0) {
                cachedLimits[sickYear2]?.let { sickLimitYear2Text = formatWholeNumber(it) }
            }
        }
    }

    val autoSickCalculationPeriodDays = remember(benefitReferenceYear) {
        calculateDefaultSickCalculationPeriodDays(benefitReferenceYear)
    }
    val effectiveSickCalculationDays = autoSickCalculationPeriodDays
    val safeSickExcludedDays = parseInt(sickExcludedDaysText, currentSettings.sickExcludedDays).coerceAtLeast(0)
        .coerceAtMost((effectiveSickCalculationDays - 1).coerceAtLeast(0))
    val computedVacationAverageDaily = remember(vacationAccruals12MonthsText) {
        calculateVacationAverageDailyFromAccruals(
            parseDouble(vacationAccruals12MonthsText, currentSettings.vacationAccruals12Months)
        )
    }
    val computedSickAverageDaily = remember(
        sickIncomeYear1Text,
        sickIncomeYear2Text,
        sickLimitYear1Text,
        sickLimitYear2Text,
        sickExcludedDaysText
    ) {
        calculateSickAverageDailyFromInputs(
            incomeYear1 = parseDouble(sickIncomeYear1Text, currentSettings.sickIncomeYear1),
            incomeYear2 = parseDouble(sickIncomeYear2Text, currentSettings.sickIncomeYear2),
            limitYear1 = parseDouble(sickLimitYear1Text, currentSettings.sickLimitYear1),
            limitYear2 = parseDouble(sickLimitYear2Text, currentSettings.sickLimitYear2),
            calculationPeriodDays = effectiveSickCalculationDays,
            excludedDays = safeSickExcludedDays
        )
    }

    if (showLeaveBenefitsSettings) {
        LeaveBenefitsSettingsScreen(
            sickYear1 = sickYear1,
            sickYear2 = sickYear2,
            vacationAccruals12MonthsText = vacationAccruals12MonthsText,
            onVacationAccrualsChange = { vacationAccruals12MonthsText = it },
            computedVacationAverageDaily = computedVacationAverageDaily,
            sickIncomeYear1Text = sickIncomeYear1Text,
            onSickIncomeYear1Change = { sickIncomeYear1Text = it },
            sickIncomeYear2Text = sickIncomeYear2Text,
            onSickIncomeYear2Change = { sickIncomeYear2Text = it },
            sickLimitYear1Text = sickLimitYear1Text,
            onSickLimitYear1Change = { sickLimitYear1Text = it },
            sickLimitYear2Text = sickLimitYear2Text,
            onSickLimitYear2Change = { sickLimitYear2Text = it },
            autoSickCalculationPeriodDays = effectiveSickCalculationDays,
            sickExcludedDaysText = sickExcludedDaysText,
            onSickExcludedDaysChange = { sickExcludedDaysText = it.filter(Char::isDigit) },
            effectiveSickCalculationDays = (effectiveSickCalculationDays - safeSickExcludedDays).coerceAtLeast(1),
            sickPayPercentText = sickPayPercentText,
            onSickPayPercentChange = { sickPayPercentText = it },
            sickMaxDailyAmountText = sickMaxDailyAmountText,
            onSickMaxDailyAmountChange = { sickMaxDailyAmountText = it },
            computedSickAverageDaily = computedSickAverageDaily,
            isLoadingLimits = isSickLimitsLoading,
            limitsMessage = sickLimitsMessage,
            onFetchLimits = {
                dialogScope.launch {
                    isSickLimitsLoading = true
                    sickLimitsMessage = null
                    try {
                        val syncResult = checkAndFetchSickInsuranceBaseLimitsIfChanged(
                            prefs = sickLimitsPrefs,
                            year1 = sickYear1,
                            year2 = sickYear2,
                            forceNetworkCheck = true
                        )
                        syncResult.limits[sickYear1]?.let { sickLimitYear1Text = formatWholeNumber(it) }
                        syncResult.limits[sickYear2]?.let { sickLimitYear2Text = formatWholeNumber(it) }
                        sickLimitsMessage = syncResult.message
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val cachedLimits = readCachedSickInsuranceBaseLimits(sickLimitsPrefs, sickYear1, sickYear2)
                        if (cachedLimits.isNotEmpty()) {
                            cachedLimits[sickYear1]?.let { sickLimitYear1Text = formatWholeNumber(it) }
                            cachedLimits[sickYear2]?.let { sickLimitYear2Text = formatWholeNumber(it) }
                            sickLimitsMessage = "Сеть недоступна. Используются локально сохранённые лимиты"
                        } else {
                            sickLimitsMessage = "Не удалось загрузить лимиты: ${e.message ?: "ошибка"}"
                        }
                    } finally {
                        isSickLimitsLoading = false
                    }
                }
            },
            onSave = { showLeaveBenefitsSettings = false },
            onBack = { showLeaveBenefitsSettings = false }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Настройки расчёта",
                onBack = onDismiss
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                SettingsSectionCard(
                    title = "Оплата",
                    subtitle = "Режим и основные суммы"
                ) {
                    Text(
                        text = "Режим оплаты",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Почасовая",
                            subtitle = "Для сменного графика и почасовой оплаты",
                            selected = payMode == PayMode.HOURLY,
                            onClick = { payModeName = PayMode.HOURLY.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PayModeChoiceCard(
                            title = "Помесячная по окладу",
                            subtitle = "Для окладной схемы и графика 5/2",
                            selected = payMode == PayMode.MONTHLY_SALARY,
                            onClick = { payModeName = PayMode.MONTHLY_SALARY.name }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactDecimalField(
                        label = "Оклад",
                        value = baseSalaryText,
                        onValueChange = { baseSalaryText = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = "Надбавка",
                        value = extraSalaryText,
                        onValueChange = { extraSalaryText = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = housingPaymentLabelText,
                        onValueChange = { housingPaymentLabelText = it },
                        label = { Text("Название выплаты") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = displayHousingPaymentLabel(housingPaymentLabelText),
                        value = housingPaymentText,
                        onValueChange = { housingPaymentText = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Режим надбавки",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        ExtraSalaryModeChoiceCard(
                            title = "Включать в часовую ставку",
                            subtitle = "Надбавка участвует в расчёте часовой ставки и доплат",
                            selected = extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE,
                            onClick = { extraSalaryModeName = ExtraSalaryMode.INCLUDED_IN_RATE.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ExtraSalaryModeChoiceCard(
                            title = "Фиксированная месячная надбавка",
                            subtitle = "Надбавка начисляется отдельно и не увеличивает часовую ставку",
                            selected = extraSalaryMode == ExtraSalaryMode.FIXED_MONTHLY,
                            onClick = { extraSalaryModeName = ExtraSalaryMode.FIXED_MONTHLY.name }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Норма и коэффициенты",
                    subtitle = "Часы и параметры расчёта"
                ) {
                    Text(
                        text = "Режим нормы часов",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        NormModeChoiceCard(
                            title = "Ручная норма",
                            subtitle = "Норма часов вводится вручную",
                            selected = normMode == NormMode.MANUAL,
                            onClick = { normModeName = NormMode.MANUAL.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        NormModeChoiceCard(
                            title = "По производственному календарю",
                            subtitle = "Норма берётся из календаря рабочего времени",
                            selected = normMode == NormMode.PRODUCTION_CALENDAR,
                            onClick = { normModeName = NormMode.PRODUCTION_CALENDAR.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        NormModeChoiceCard(
                            title = "Среднегодовая",
                            subtitle = "Средняя месячная норма по году",
                            selected = normMode == NormMode.AVERAGE_ANNUAL,
                            onClick = { normModeName = NormMode.AVERAGE_ANNUAL.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        NormModeChoiceCard(
                            title = "Среднеквартальная",
                            subtitle = "Средняя норма по кварталу на основе рабочего дня",
                            selected = normMode == NormMode.AVERAGE_QUARTERLY,
                            onClick = { normModeName = NormMode.AVERAGE_QUARTERLY.name }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (normMode == NormMode.MANUAL) {
                        CompactDecimalField(
                            label = "Норма часов в месяце",
                            value = monthlyNormHoursText,
                            onValueChange = { monthlyNormHoursText = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (normMode == NormMode.AVERAGE_ANNUAL) {
                        Text(
                            text = "Источник среднегодовой нормы",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp)
                        ) {
                            AnnualNormSourceChoiceCard(
                                title = "По часам в рабочем дне",
                                subtitle = "Годовая норма считается по календарю и числу часов в дне",
                                selected = annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS,
                                onClick = { annualNormSourceModeName = AnnualNormSourceMode.WORKDAY_HOURS.name }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            AnnualNormSourceChoiceCard(
                                title = "По общему количеству часов в году",
                                subtitle = "Средняя месячная норма = часы за год / 12",
                                selected = annualNormSourceMode == AnnualNormSourceMode.YEAR_TOTAL_HOURS,
                                onClick = { annualNormSourceModeName = AnnualNormSourceMode.YEAR_TOTAL_HOURS.name }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (
                        normMode == NormMode.PRODUCTION_CALENDAR ||
                        normMode == NormMode.AVERAGE_QUARTERLY ||
                        (normMode == NormMode.AVERAGE_ANNUAL && annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS)
                    ) {
                        CompactDecimalField(
                            label = "Часов в рабочем дне",
                            value = workdayHoursText,
                            onValueChange = { workdayHoursText = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (normMode == NormMode.AVERAGE_ANNUAL && annualNormSourceMode == AnnualNormSourceMode.YEAR_TOTAL_HOURS) {
                        CompactDecimalField(
                            label = "Норма часов за год",
                            value = annualNormHoursText,
                            onValueChange = { annualNormHoursText = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    CompactSwitchRow(
                        title = "Учитывать сокращённые предпраздничные дни",
                        checked = applyShortDayReduction,
                        onCheckedChange = { applyShortDayReduction = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Ночные",
                            value = nightPercentText,
                            onValueChange = { nightPercentText = it },
                            modifier = Modifier.weight(1f)
                        )

                        CompactDecimalField(
                            label = "РВД/РВН",
                            value = holidayRateMultiplierText,
                            onValueChange = { holidayRateMultiplierText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = "НДФЛ",
                        value = ndflPercentText,
                        onValueChange = { ndflPercentText = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Отпуск и больничный",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsValueNavigationCard(
                        title = "Параметры отпуска и больничного",
                        subtitle = "Отдельный расчёт среднедневного заработка и лимитов ФНС",
                        value = "Отпуск: ${formatMoney(computedVacationAverageDaily)} • Больничный: ${formatMoney(computedSickAverageDaily)}",
                        onClick = { showLeaveBenefitsSettings = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactSwitchRow(
                        title = "Прогрессивный НДФЛ РФ",
                        checked = progressiveNdflEnabled,
                        onCheckedChange = { progressiveNdflEnabled = it }
                    )

                    if (progressiveNdflEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CompactDecimalField(
                            label = "Доход с начала года до текущего месяца",
                            value = taxableIncomeYtdText,
                            onValueChange = { taxableIncomeYtdText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Сверхурочка и выходные",
                    subtitle = "Период учёта и исключения из переработки"
                ) {
                    CompactSwitchRow(
                        title = "Считать сверхурочку",
                        checked = overtimeEnabled,
                        onCheckedChange = { overtimeEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Период учёта",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Месяц",
                            subtitle = "Переработка внутри текущего месяца",
                            selected = overtimePeriod == OvertimePeriod.MONTH,
                            onClick = { overtimePeriodName = OvertimePeriod.MONTH.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PayModeChoiceCard(
                            title = "Квартал",
                            subtitle = "Суммированный учёт по кварталу",
                            selected = overtimePeriod == OvertimePeriod.QUARTER,
                            onClick = { overtimePeriodName = OvertimePeriod.QUARTER.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PayModeChoiceCard(
                            title = "Полугодие",
                            subtitle = "Суммированный учёт по полугодию",
                            selected = overtimePeriod == OvertimePeriod.HALF_YEAR,
                            onClick = { overtimePeriodName = OvertimePeriod.HALF_YEAR.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PayModeChoiceCard(
                            title = "Год",
                            subtitle = "Суммированный учёт по году",
                            selected = overtimePeriod == OvertimePeriod.YEAR,
                            onClick = { overtimePeriodName = OvertimePeriod.YEAR.name }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactSwitchRow(
                        title = "Исключать выходные / праздничные",
                        checked = excludeWeekendHolidayFromOvertime,
                        onCheckedChange = { excludeWeekendHolidayFromOvertime = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactSwitchRow(
                        title = "Исключать РВД с двойной оплатой",
                        checked = excludeRvdDoublePayFromOvertime,
                        onCheckedChange = { excludeRvdDoublePayFromOvertime = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactSwitchRow(
                        title = "Исключать РВД с отгулом",
                        checked = excludeRvdSingleWithDayOffFromOvertime,
                        onCheckedChange = { excludeRvdSingleWithDayOffFromOvertime = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Даты выплат",
                    subtitle = "Числа месяца"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactIntField(
                            label = "Аванс",
                            value = advanceDayText,
                            onValueChange = { advanceDayText = it },
                            modifier = Modifier.weight(1f)
                        )

                        CompactIntField(
                            label = "Зарплата",
                            value = salaryDayText,
                            onValueChange = { salaryDayText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Режим аванса",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        AdvanceModeChoiceCard(
                            title = "По фактически начисленному",
                            subtitle = "Аванс считается по первой половине месяца",
                            selected = advanceMode == AdvanceMode.ACTUAL_EARNINGS,
                            onClick = { advanceModeName = AdvanceMode.ACTUAL_EARNINGS.name }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        AdvanceModeChoiceCard(
                            title = "Фиксированный процент",
                            subtitle = "Аванс как процент от месячной базы",
                            selected = advanceMode == AdvanceMode.FIXED_PERCENT,
                            onClick = { advanceModeName = AdvanceMode.FIXED_PERCENT.name }
                        )
                    }

                    if (advanceMode == AdvanceMode.FIXED_PERCENT) {
                        Spacer(modifier = Modifier.height(10.dp))

                        CompactDecimalField(
                            label = "Процент аванса",
                            value = advancePercentText,
                            onValueChange = { advancePercentText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompactSwitchRow(
                        title = "Сдвигать на предыдущий рабочий день",
                        checked = movePaymentsToPreviousWorkday,
                        onCheckedChange = { movePaymentsToPreviousWorkday = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Прочее",
                    subtitle = "Дополнительные параметры выплат"
                ) {
                    CompactSwitchRow(
                        title = "Выплату на квартиру учитывать в авансе",
                        checked = housingPaymentWithAdvance,
                        onCheckedChange = { housingPaymentWithAdvance = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactSwitchRow(
                        title = "Выплата на квартиру облагается НДФЛ",
                        checked = housingPaymentTaxable,
                        onCheckedChange = { housingPaymentTaxable = it }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onSave(
                            PayrollSettings(
                                baseSalary = parseDouble(baseSalaryText, currentSettings.baseSalary),
                                extraSalary = parseDouble(extraSalaryText, currentSettings.extraSalary),
                                housingPayment = parseDouble(housingPaymentText, currentSettings.housingPayment),
                                housingPaymentLabel = displayHousingPaymentLabel(housingPaymentLabelText),
                                housingPaymentTaxable = housingPaymentTaxable,
                                housingPaymentWithAdvance = housingPaymentWithAdvance,
                                monthlyNormHours = parseDouble(monthlyNormHoursText, currentSettings.monthlyNormHours),
                                normMode = normModeName,
                                workdayHours = parseDouble(workdayHoursText, currentSettings.workdayHours),
                                annualNormSourceMode = annualNormSourceModeName,
                                annualNormHours = parseDouble(annualNormHoursText, currentSettings.annualNormHours),
                                payMode = payModeName,
                                extraSalaryMode = extraSalaryModeName,
                                nightPercent = parseDouble(nightPercentText, currentSettings.nightPercent),
                                holidayRateMultiplier = parseDouble(
                                    holidayRateMultiplierText,
                                    currentSettings.holidayRateMultiplier
                                ),
                                ndflPercent = parseDouble(ndflPercentText, currentSettings.ndflPercent),
                                vacationAverageDaily = computedVacationAverageDaily,
                                vacationAccruals12Months = parseDouble(vacationAccruals12MonthsText, currentSettings.vacationAccruals12Months),
                                sickAverageDaily = computedSickAverageDaily,
                                sickIncomeYear1 = parseDouble(sickIncomeYear1Text, currentSettings.sickIncomeYear1),
                                sickIncomeYear2 = parseDouble(sickIncomeYear2Text, currentSettings.sickIncomeYear2),
                                sickLimitYear1 = parseDouble(sickLimitYear1Text, currentSettings.sickLimitYear1),
                                sickLimitYear2 = parseDouble(sickLimitYear2Text, currentSettings.sickLimitYear2),
                                sickCalculationPeriodDays = effectiveSickCalculationDays,
                                sickExcludedDays = safeSickExcludedDays,
                                sickPayPercent = parseDouble(sickPayPercentText, currentSettings.sickPayPercent),
                                sickMaxDailyAmount = parseDouble(sickMaxDailyAmountText, currentSettings.sickMaxDailyAmount),
                                progressiveNdflEnabled = progressiveNdflEnabled,
                                taxableIncomeYtdBeforeCurrentMonth = parseDouble(taxableIncomeYtdText, currentSettings.taxableIncomeYtdBeforeCurrentMonth),
                                advanceMode = advanceModeName,
                                advancePercent = parseDouble(advancePercentText, currentSettings.advancePercent),
                                advanceDay = parseInt(advanceDayText, currentSettings.advanceDay).coerceIn(1, 31),
                                salaryDay = parseInt(salaryDayText, currentSettings.salaryDay).coerceIn(1, 31),
                                movePaymentsToPreviousWorkday = movePaymentsToPreviousWorkday,
                                applyShortDayReduction = applyShortDayReduction,
                                overtimeEnabled = overtimeEnabled,
                                overtimePeriod = overtimePeriodName,
                                excludeWeekendHolidayFromOvertime = excludeWeekendHolidayFromOvertime,
                                excludeRvdDoublePayFromOvertime = excludeRvdDoublePayFromOvertime,
                                excludeRvdSingleWithDayOffFromOvertime = excludeRvdSingleWithDayOffFromOvertime
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
