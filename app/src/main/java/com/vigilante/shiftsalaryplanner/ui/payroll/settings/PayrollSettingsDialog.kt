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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.vigilante.shiftsalaryplanner.payroll.NightHoursBaseMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import com.vigilante.shiftsalaryplanner.settings.Workplace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class PayrollSettingsSection {
    PAYMENT,
    NORMS,
    OVERTIME,
    DATES,
    OTHER
}

@Composable
fun PayrollSettingsDialog(
    currentSettings: PayrollSettings,
    workplaces: List<Workplace>,
    selectedWorkplaceId: String,
    onChangeWorkplace: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (PayrollSettings) -> Unit
) {
    val context = LocalContext.current
    val currencySymbol = currentCurrencySymbol()
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
    var nightPercentText by rememberSaveable {
        mutableStateOf(
            ratioToPercentUiValue(
                ratio = currentSettings.nightPercent,
                coefficientUpperBound = 3.0
            ).toPlainString()
        )
    }
    var nightHoursBaseModeName by rememberSaveable {
        mutableStateOf(currentSettings.nightHoursBaseMode.ifBlank { NightHoursBaseMode.FOLLOW_HOURLY_RATE.name })
    }
    var holidayRateMultiplierText by rememberSaveable { mutableStateOf(currentSettings.holidayRateMultiplier.toPlainString()) }
    var ndflPercentText by rememberSaveable {
        mutableStateOf(
            ratioToPercentUiValue(
                ratio = currentSettings.ndflPercent,
                coefficientUpperBound = 1.0
            ).toPlainString()
        )
    }
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
    var expandedSectionName by rememberSaveable { mutableStateOf(PayrollSettingsSection.PAYMENT.name) }
    var showLeaveBenefitsSettings by rememberSaveable { mutableStateOf(false) }
    var isSickLimitsLoading by rememberSaveable { mutableStateOf(false) }
    var sickLimitsMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val payMode = runCatching { PayMode.valueOf(payModeName) }.getOrElse { PayMode.HOURLY }
    val extraSalaryMode = runCatching { ExtraSalaryMode.valueOf(extraSalaryModeName) }.getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }
    val normMode = runCatching { NormMode.valueOf(normModeName) }.getOrElse { NormMode.MANUAL }
    val annualNormSourceMode = runCatching { AnnualNormSourceMode.valueOf(annualNormSourceModeName) }
        .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    val nightHoursBaseMode = runCatching { NightHoursBaseMode.valueOf(nightHoursBaseModeName) }
        .getOrElse { NightHoursBaseMode.FOLLOW_HOURLY_RATE }
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
    val safeSickExcludedDays = parseInt(sickExcludedDaysText, currentSettings.sickExcludedDays).coerceAtLeast(0)
        .coerceAtMost((autoSickCalculationPeriodDays - 1).coerceAtLeast(0))
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
            calculationPeriodDays = autoSickCalculationPeriodDays,
            excludedDays = safeSickExcludedDays
        )
    }
    val expandedSection = runCatching { PayrollSettingsSection.valueOf(expandedSectionName) }.getOrNull()
    val toggleSection: (PayrollSettingsSection) -> Unit = { section ->
        expandedSectionName = if (expandedSection == section) "" else section.name
    }
    val paymentSummary = "${payModeLabel(payModeName)} • ${formatMoney(parseDouble(baseSalaryText, currentSettings.baseSalary))}"
    val normsSummary = "${normModeLabel(normModeName)} • ${nightHoursBaseModeLabel(nightHoursBaseModeName)} • НДФЛ ${
        formatDouble(
            parseDouble(
                ndflPercentText,
                ratioToPercentUiValue(
                    ratio = currentSettings.ndflPercent,
                    coefficientUpperBound = 1.0
                )
            )
        )
    }%"
    val overtimeSummary = if (overtimeEnabled) {
        "${overtimePeriodLabel(overtimePeriodName)} • исключения: " +
            listOf(
                excludeWeekendHolidayFromOvertime,
                excludeRvdDoublePayFromOvertime,
                excludeRvdSingleWithDayOffFromOvertime
            ).count { it }
    } else {
        "Отключено"
    }
    val datesSummary =
        "Аванс ${parseInt(advanceDayText, currentSettings.advanceDay).coerceIn(1, 31)} • " +
            "Зарплата ${parseInt(salaryDayText, currentSettings.salaryDay).coerceIn(1, 31)}"
    val otherSummary = buildString {
        append(if (housingPaymentWithAdvance) "Квартира в авансе" else "Квартира только в зарплате")
        append(" • ")
        append(if (housingPaymentTaxable) "Облагается НДФЛ" else "Без НДФЛ")
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
            autoSickCalculationPeriodDays = autoSickCalculationPeriodDays,
            sickExcludedDaysText = sickExcludedDaysText,
            onSickExcludedDaysChange = { sickExcludedDaysText = it.filter(Char::isDigit) },
            effectiveSickCalculationDays = (autoSickCalculationPeriodDays - safeSickExcludedDays).coerceAtLeast(1),
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
                            sickLimitsMessage = "Лимиты обновить не удалось, использованы сохранённые значения"
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
                CalendarWorkplaceSwitcher(
                    workplaces = workplaces,
                    activeWorkplaceId = selectedWorkplaceId,
                    onSwitchWorkplace = onChangeWorkplace
                )

                Spacer(modifier = Modifier.height(10.dp))

                CollapsibleSettingsSectionCard(
                    title = "Оплата",
                    subtitle = "Режим и основные суммы",
                    summary = paymentSummary,
                    expanded = expandedSection == PayrollSettingsSection.PAYMENT,
                    onToggle = { toggleSection(PayrollSettingsSection.PAYMENT) }
                ) {
                    Text(
                        text = "Режим оплаты",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PayModeChoiceCard(
                            title = "Почасовая",
                            subtitle = "Для сменного графика и почасовой оплаты",
                            selected = payMode == PayMode.HOURLY,
                            onClick = { payModeName = PayMode.HOURLY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )

                        PayModeChoiceCard(
                            title = "Оклад",
                            subtitle = "Для окладной схемы и графика 5/2",
                            selected = payMode == PayMode.MONTHLY_SALARY,
                            onClick = { payModeName = PayMode.MONTHLY_SALARY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (payMode == PayMode.HOURLY) {
                            "Сменный график и почасовая оплата"
                        } else {
                            "Окладная схема и график 5/2"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Оклад, $currencySymbol",
                            value = baseSalaryText,
                            onValueChange = { baseSalaryText = it },
                            modifier = Modifier.weight(1f)
                        )

                        CompactDecimalField(
                            label = "Надбавка, $currencySymbol",
                            value = extraSalaryText,
                            onValueChange = { extraSalaryText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "База для доплаты за ночные",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = "Как ставка",
                                subtitle = "База ночных берётся из текущей часовой ставки",
                                selected = nightHoursBaseMode == NightHoursBaseMode.FOLLOW_HOURLY_RATE,
                                onClick = { nightHoursBaseModeName = NightHoursBaseMode.FOLLOW_HOURLY_RATE.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = "Только оклад",
                                subtitle = "Ночные считаются от базового оклада",
                                selected = nightHoursBaseMode == NightHoursBaseMode.BASE_ONLY,
                                onClick = { nightHoursBaseModeName = NightHoursBaseMode.BASE_ONLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = "Оклад + надбавка",
                                subtitle = "Ночные считаются от оклада вместе с надбавкой",
                                selected = nightHoursBaseMode == NightHoursBaseMode.BASE_PLUS_EXTRA,
                                onClick = { nightHoursBaseModeName = NightHoursBaseMode.BASE_PLUS_EXTRA.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = "Оклад + надб. + ручные",
                                subtitle = "Добавляет ручные надбавки с флагом «В цене смены»",
                                selected = nightHoursBaseMode == NightHoursBaseMode.BASE_PLUS_EXTRA_PLUS_MANUAL,
                                onClick = { nightHoursBaseModeName = NightHoursBaseMode.BASE_PLUS_EXTRA_PLUS_MANUAL.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (nightHoursBaseMode) {
                            NightHoursBaseMode.FOLLOW_HOURLY_RATE -> "Ночные считаются от текущей часовой ставки"
                            NightHoursBaseMode.BASE_ONLY -> "Ночные считаются только от оклада"
                            NightHoursBaseMode.BASE_PLUS_EXTRA -> "Ночные считаются от оклада и надбавки"
                            NightHoursBaseMode.BASE_PLUS_EXTRA_PLUS_MANUAL -> "Ночные считаются от оклада, надбавки и ручных доплат в цене смены"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactTextField(
                            label = "Название выплаты",
                            value = housingPaymentLabelText,
                            onValueChange = { housingPaymentLabelText = it },
                            modifier = Modifier.weight(1.25f)
                        )

                        CompactDecimalField(
                            label = "Сумма, $currencySymbol",
                            value = housingPaymentText,
                            onValueChange = { housingPaymentText = it },
                            modifier = Modifier.weight(0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Режим надбавки",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExtraSalaryModeChoiceCard(
                            title = "В ставку",
                            subtitle = "Надбавка участвует в расчёте часовой ставки и доплат",
                            selected = extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE,
                            onClick = { extraSalaryModeName = ExtraSalaryMode.INCLUDED_IN_RATE.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )

                        ExtraSalaryModeChoiceCard(
                            title = "Фикс, мес",
                            subtitle = "Надбавка начисляется отдельно и не увеличивает часовую ставку",
                            selected = extraSalaryMode == ExtraSalaryMode.FIXED_MONTHLY,
                            onClick = { extraSalaryModeName = ExtraSalaryMode.FIXED_MONTHLY.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE) {
                            "Надбавка участвует в расчёте часовой ставки и доплат"
                        } else {
                            "Надбавка начисляется отдельно и не увеличивает часовую ставку"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                CollapsibleSettingsSectionCard(
                    title = "Норма и коэффициенты",
                    subtitle = "Часы и параметры расчёта",
                    summary = normsSummary,
                    expanded = expandedSection == PayrollSettingsSection.NORMS,
                    onToggle = { toggleSection(PayrollSettingsSection.NORMS) }
                ) {
                    Text(
                        text = "Режим нормы часов",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NormModeChoiceCard(
                                title = "Ручная",
                                subtitle = "Норма часов вводится вручную",
                                selected = normMode == NormMode.MANUAL,
                                onClick = { normModeName = NormMode.MANUAL.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            NormModeChoiceCard(
                                title = "Календарь",
                                subtitle = "Норма берётся из календаря рабочего времени",
                                selected = normMode == NormMode.PRODUCTION_CALENDAR,
                                onClick = { normModeName = NormMode.PRODUCTION_CALENDAR.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NormModeChoiceCard(
                                title = "Ср. год",
                                subtitle = "Средняя месячная норма по году",
                                selected = normMode == NormMode.AVERAGE_ANNUAL,
                                onClick = { normModeName = NormMode.AVERAGE_ANNUAL.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            NormModeChoiceCard(
                                title = "Ср. квартал",
                                subtitle = "Средняя норма по кварталу на основе рабочего дня",
                                selected = normMode == NormMode.AVERAGE_QUARTERLY,
                                onClick = { normModeName = NormMode.AVERAGE_QUARTERLY.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (normMode) {
                            NormMode.MANUAL -> "Норма часов вводится вручную"
                            NormMode.PRODUCTION_CALENDAR -> "Норма берётся из производственного календаря"
                            NormMode.AVERAGE_ANNUAL -> "Средняя месячная норма по году"
                            NormMode.AVERAGE_QUARTERLY -> "Средняя норма по кварталу"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (normMode == NormMode.MANUAL) {
                        CompactDecimalField(
                            label = "Норма часов в месяце",
                            value = monthlyNormHoursText,
                            onValueChange = { monthlyNormHoursText = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (normMode == NormMode.AVERAGE_ANNUAL) {
                        Text(
                            text = "Источник среднегодовой нормы",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnnualNormSourceChoiceCard(
                                title = "По дню",
                                subtitle = "Годовая норма считается по календарю и числу часов в дне",
                                selected = annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS,
                                onClick = { annualNormSourceModeName = AnnualNormSourceMode.WORKDAY_HOURS.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )

                            AnnualNormSourceChoiceCard(
                                title = "По году",
                                subtitle = "Средняя месячная норма = часы за год / 12",
                                selected = annualNormSourceMode == AnnualNormSourceMode.YEAR_TOTAL_HOURS,
                                onClick = { annualNormSourceModeName = AnnualNormSourceMode.YEAR_TOTAL_HOURS.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS) {
                                "Норма считается по календарю и числу часов в рабочем дне"
                            } else {
                                "Средняя месячная норма = часы за год / 12"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))
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

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (normMode == NormMode.AVERAGE_ANNUAL && annualNormSourceMode == AnnualNormSourceMode.YEAR_TOTAL_HOURS) {
                        CompactDecimalField(
                            label = "Норма часов за год",
                            value = annualNormHoursText,
                            onValueChange = { annualNormHoursText = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    CompactSwitchRow(
                        title = "Учитывать сокращённые предпраздничные дни",
                        checked = applyShortDayReduction,
                        onCheckedChange = { applyShortDayReduction = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Ночные, %",
                            value = nightPercentText,
                            onValueChange = { nightPercentText = it },
                            modifier = Modifier.weight(1f)
                        )

                        CompactDecimalField(
                            label = "НДФЛ, %",
                            value = ndflPercentText,
                            onValueChange = { ndflPercentText = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CompactDecimalField(
                        label = "РВД/РВН, x",
                        value = holidayRateMultiplierText,
                        onValueChange = { holidayRateMultiplierText = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Отпуск и больничный",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsValueNavigationCard(
                        title = "Параметры отпуска и больничного",
                        subtitle = "Отдельный расчёт среднедневного заработка и лимитов ФНС",
                        value = "Отпуск: ${formatMoney(computedVacationAverageDaily)} • Больничный: ${formatMoney(computedSickAverageDaily)}",
                        onClick = { showLeaveBenefitsSettings = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactSwitchRow(
                        title = "Прогрессивный НДФЛ РФ",
                        checked = progressiveNdflEnabled,
                        onCheckedChange = { progressiveNdflEnabled = it }
                    )

                    if (progressiveNdflEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        CompactDecimalField(
                            label = "Доход с начала года до текущего месяца",
                            value = taxableIncomeYtdText,
                            onValueChange = { taxableIncomeYtdText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                CollapsibleSettingsSectionCard(
                    title = "Сверхурочка и выходные",
                    subtitle = "Период учёта и исключения из переработки",
                    summary = overtimeSummary,
                    expanded = expandedSection == PayrollSettingsSection.OVERTIME,
                    onToggle = { toggleSection(PayrollSettingsSection.OVERTIME) }
                ) {
                    CompactSwitchRow(
                        title = "Считать сверхурочку",
                        checked = overtimeEnabled,
                        onCheckedChange = { overtimeEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Период учёта",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = "Месяц",
                                subtitle = "Переработка внутри текущего месяца",
                                selected = overtimePeriod == OvertimePeriod.MONTH,
                                onClick = { overtimePeriodName = OvertimePeriod.MONTH.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = "Квартал",
                                subtitle = "Суммированный учёт по кварталу",
                                selected = overtimePeriod == OvertimePeriod.QUARTER,
                                onClick = { overtimePeriodName = OvertimePeriod.QUARTER.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayModeChoiceCard(
                                title = "Полугодие",
                                subtitle = "Суммированный учёт по полугодию",
                                selected = overtimePeriod == OvertimePeriod.HALF_YEAR,
                                onClick = { overtimePeriodName = OvertimePeriod.HALF_YEAR.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                            PayModeChoiceCard(
                                title = "Год",
                                subtitle = "Суммированный учёт по году",
                                selected = overtimePeriod == OvertimePeriod.YEAR,
                                onClick = { overtimePeriodName = OvertimePeriod.YEAR.name },
                                modifier = Modifier.weight(1f),
                                showSubtitle = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (overtimePeriod) {
                            OvertimePeriod.MONTH -> "Переработка внутри текущего месяца"
                            OvertimePeriod.QUARTER -> "Суммированный учёт по кварталу"
                            OvertimePeriod.HALF_YEAR -> "Суммированный учёт по полугодию"
                            OvertimePeriod.YEAR -> "Суммированный учёт по году"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactSwitchRow(
                        title = "Исключать выходные / праздничные",
                        checked = excludeWeekendHolidayFromOvertime,
                        onCheckedChange = { excludeWeekendHolidayFromOvertime = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CompactSwitchRow(
                        title = "Исключать РВД с двойной оплатой",
                        checked = excludeRvdDoublePayFromOvertime,
                        onCheckedChange = { excludeRvdDoublePayFromOvertime = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CompactSwitchRow(
                        title = "Исключать РВД с отгулом",
                        checked = excludeRvdSingleWithDayOffFromOvertime,
                        onCheckedChange = { excludeRvdSingleWithDayOffFromOvertime = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                CollapsibleSettingsSectionCard(
                    title = "Даты выплат",
                    subtitle = "Числа месяца",
                    summary = datesSummary,
                    expanded = expandedSection == PayrollSettingsSection.DATES,
                    onToggle = { toggleSection(PayrollSettingsSection.DATES) }
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Режим аванса",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdvanceModeChoiceCard(
                            title = "По факту",
                            subtitle = "Аванс считается по первой половине месяца",
                            selected = advanceMode == AdvanceMode.ACTUAL_EARNINGS,
                            onClick = { advanceModeName = AdvanceMode.ACTUAL_EARNINGS.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )

                        AdvanceModeChoiceCard(
                            title = "Фикс %",
                            subtitle = "Аванс как процент от месячной базы",
                            selected = advanceMode == AdvanceMode.FIXED_PERCENT,
                            onClick = { advanceModeName = AdvanceMode.FIXED_PERCENT.name },
                            modifier = Modifier.weight(1f),
                            showSubtitle = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (advanceMode == AdvanceMode.ACTUAL_EARNINGS) {
                            "Аванс считается по фактически начисленному за первую половину месяца"
                        } else {
                            "Аванс рассчитывается как фиксированный процент от месячной базы"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )

                    if (advanceMode == AdvanceMode.FIXED_PERCENT) {
                        Spacer(modifier = Modifier.height(6.dp))

                        CompactDecimalField(
                            label = "Процент аванса",
                            value = advancePercentText,
                            onValueChange = { advancePercentText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactSwitchRow(
                        title = "Сдвигать на предыдущий рабочий день",
                        checked = movePaymentsToPreviousWorkday,
                        onCheckedChange = { movePaymentsToPreviousWorkday = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                CollapsibleSettingsSectionCard(
                    title = "Прочее",
                    subtitle = "Дополнительные параметры выплат",
                    summary = otherSummary,
                    expanded = expandedSection == PayrollSettingsSection.OTHER,
                    onToggle = { toggleSection(PayrollSettingsSection.OTHER) }
                ) {
                    CompactSwitchRow(
                        title = "Выплату на квартиру учитывать в авансе",
                        checked = housingPaymentWithAdvance,
                        onCheckedChange = { housingPaymentWithAdvance = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CompactSwitchRow(
                        title = "Выплата на квартиру облагается НДФЛ",
                        checked = housingPaymentTaxable,
                        onCheckedChange = { housingPaymentTaxable = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                nightPercent = parsePercentUiToRatio(
                                    text = nightPercentText,
                                    fallbackRatio = currentSettings.nightPercent,
                                    coefficientUpperBound = 3.0
                                ),
                                nightHoursBaseMode = nightHoursBaseModeName,
                                holidayRateMultiplier = parseDouble(
                                    holidayRateMultiplierText,
                                    currentSettings.holidayRateMultiplier
                                ),
                                ndflPercent = parsePercentUiToRatio(
                                    text = ndflPercentText,
                                    fallbackRatio = currentSettings.ndflPercent,
                                    coefficientUpperBound = 1.0
                                ),
                                vacationAverageDaily = computedVacationAverageDaily,
                                vacationAccruals12Months = parseDouble(vacationAccruals12MonthsText, currentSettings.vacationAccruals12Months),
                                sickAverageDaily = computedSickAverageDaily,
                                sickIncomeYear1 = parseDouble(sickIncomeYear1Text, currentSettings.sickIncomeYear1),
                                sickIncomeYear2 = parseDouble(sickIncomeYear2Text, currentSettings.sickIncomeYear2),
                                sickLimitYear1 = parseDouble(sickLimitYear1Text, currentSettings.sickLimitYear1),
                                sickLimitYear2 = parseDouble(sickLimitYear2Text, currentSettings.sickLimitYear2),
                                sickCalculationPeriodDays = autoSickCalculationPeriodDays,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Сохранить изменения",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

