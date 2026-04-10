package com.vigilante.shiftsalaryplanner

import android.Manifest
import androidx.compose.material3.Divider
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.DefaultShiftTemplates
import com.vigilante.shiftsalaryplanner.data.FederalHolidaySeed
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.excel.ExcelImportParseResult
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleParser
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculatePaymentDates
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetDraftFactory
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.widget.PREFS_WIDGET_SETTINGS
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProvider
import com.vigilante.shiftsalaryplanner.widget.clearWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import com.vigilante.shiftsalaryplanner.NewPayrollIntegration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftSalaryPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShiftSalaryApp()
                }
            }
        }
    }
}

@Composable
fun ShiftSalaryApp() {
    // В начале функции ShiftSalaryApp():
    var showNewPayrollDialog by remember { mutableStateOf(false) }
    var newPayrollGross by remember { mutableStateOf(0.0) }
    var newPayrollNet by remember { mutableStateOf(0.0) }
    var newPayrollAdvance by remember { mutableStateOf(0.0) }
    var newPayrollMain by remember { mutableStateOf(0.0) }
    var newPayrollError by remember { mutableStateOf<String?>(null) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var quickPickerOpen by rememberSaveable { mutableStateOf(false) }
    var activeBrushCode by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorSettings by remember { mutableStateOf(false) }
    var showPayrollSettings by rememberSaveable { mutableStateOf(false) }
    var showCurrentParameters by rememberSaveable { mutableStateOf(false) }
    var showAdditionalPaymentsScreen by rememberSaveable { mutableStateOf(false) }
    var showAdditionalPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var editingAdditionalPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeductionsScreen by rememberSaveable { mutableStateOf(false) }
    var showDeductionEditorScreen by rememberSaveable { mutableStateOf(false) }
    var editingDeductionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showShiftTemplateEditDialog by rememberSaveable { mutableStateOf(false) }
    var editingShiftTemplateCode by rememberSaveable { mutableStateOf<String?>(null) }
    var isSummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var isLegendExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedTabName by rememberSaveable { mutableStateOf(BottomTab.CALENDAR.name) }
    var showPatternListDialog by rememberSaveable { mutableStateOf(false) }
    var showPatternEditDialog by rememberSaveable { mutableStateOf(false) }
    var editingPatternId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPatternApplyDialog by rememberSaveable { mutableStateOf(false) }
    var applyingPatternId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPatternQuickPicker by rememberSaveable { mutableStateOf(false) }
    var activePatternId by rememberSaveable { mutableStateOf<String?>(null) }
    var patternRangeStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPatternRangeStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPatternRangeEndIso by rememberSaveable { mutableStateOf<String?>(null) }
    var showPatternPreviewDialog by rememberSaveable { mutableStateOf(false) }
    var templateModeName by rememberSaveable { mutableStateOf(TemplateMode.SHIFTS.name) }
    var isHolidaySyncing by rememberSaveable { mutableStateOf(false) }
    var holidaySyncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showManualHolidaysScreen by rememberSaveable { mutableStateOf(false) }
    var showManualHolidayDialog by rememberSaveable { mutableStateOf(false) }
    var editingManualHolidayDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showMonthlyReport by rememberSaveable { mutableStateOf(false) }
    var showBackupRestoreScreen by rememberSaveable { mutableStateOf(false) }
    var showWidgetSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showExcelImportScreen by rememberSaveable { mutableStateOf(false) }
    var excelImportStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExcelFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var backupRestoreStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBackupJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingBackupFileName by remember { mutableStateOf("ShiftSalaryPlanner_backup.json") }
    var pendingReportCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingReportCsvFileName by remember { mutableStateOf("report.csv") }
    var pendingExcelFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var excelImportPreview by remember { mutableStateOf<ExcelImportPreview?>(null) }
    var excelImportCandidates by remember { mutableStateOf<List<ExcelPersonCandidate>>(emptyList()) }

    val selectedTab = BottomTab.valueOf(selectedTabName)
    val templateMode = TemplateMode.valueOf(templateModeName)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val payrollSettingsStore = remember { PayrollSettingsStore(context) }
    val shiftAlarmStore = remember { ShiftAlarmStore(context) }
    val patternTemplatesStore = remember { PatternTemplatesStore(context) }
    val additionalPaymentsStore = remember { AdditionalPaymentsStore(context) }
    val deductionsStore = remember { DeductionsStore(context) }
    val db = remember { AppDatabase.getDatabase(context) }
        val shiftDayDao = remember { db.shiftDayDao() }
    val shiftTemplateDao = remember { db.shiftTemplateDao() }
    val holidayDao = remember { db.holidayDao() }
    val holidaySyncRepository = remember { HolidaySyncRepository(holidayDao) }
    val excelScheduleParser = remember { ExcelScheduleParser() }
    val excelScheduleImporter = remember(shiftTemplateDao, shiftDayDao) { ExcelScheduleImporter(shiftTemplateDao, shiftDayDao) }
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val reportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingReportCsvContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
            }
        }
        pendingReportCsvContent = null
    }

    val newPayroll = remember { NewPayrollIntegration(context, scope, db) }
    val excelImportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Не удалось прочитать Excel-файл")
            pendingExcelFileBytes = bytes
            pendingExcelFileName = uri.lastPathSegment ?: "tabel.xlsm"
            excelImportPreview = null
            excelImportCandidates = emptyList()
            excelImportStatusMessage = "Файл выбран: ${pendingExcelFileName}"
        }.onFailure { error ->
            pendingExcelFileBytes = null
            pendingExcelFileName = null
            excelImportPreview = null
            excelImportCandidates = emptyList()
            excelImportStatusMessage = "Не удалось открыть файл: ${error.message ?: "неизвестно"}"
        }
    }

    val savedDays by shiftDayDao.observeAll().collectAsState(initial = emptyList())
    val shiftTemplates by shiftTemplateDao.observeAll().collectAsState(initial = emptyList())
    val holidays by holidayDao.observeByScope("RU-FED").collectAsState(initial = emptyList())
    val additionalPayments by additionalPaymentsStore.paymentsFlow.collectAsState(initial = emptyList())
    val deductions by deductionsStore.deductionsFlow.collectAsState(initial = emptyList())
    val patternTemplates by patternTemplatesStore.patternsFlow.collectAsState(initial = emptyList())

    val payrollSettings by payrollSettingsStore.settingsFlow.collectAsState(
        initial = neutralInitialPayrollSettings()
    )
    val shiftAlarmSettings by shiftAlarmStore.settingsFlow.collectAsState(
        initial = ShiftAlarmSettings()
    )
    var shiftAlarmRescheduleResult by remember { mutableStateOf<ShiftAlarmRescheduleResult?>(null) }
    val shiftSpecialPrefs = remember {
        context.getSharedPreferences(PREFS_SHIFT_SPECIAL_RULES, Context.MODE_PRIVATE)
    }
    val migrationPrefs = remember {
        context.getSharedPreferences(PREFS_ONE_TIME_MIGRATIONS, Context.MODE_PRIVATE)
    }
    val payrollSettingsPrefs = remember {
        context.getSharedPreferences("payroll_settings", Context.MODE_PRIVATE)
    }
    val additionalPaymentsPrefs = remember {
        context.getSharedPreferences("additional_payments", Context.MODE_PRIVATE)
    }
    val patternTemplatesPrefs = remember {
        context.getSharedPreferences("pattern_templates", Context.MODE_PRIVATE)
    }
    val shiftAlarmSettingsPrefs = remember {
        context.getSharedPreferences("shift_alarm_settings", Context.MODE_PRIVATE)
    }
    val shiftColorsPrefs = remember {
        context.getSharedPreferences(PREFS_SHIFT_COLORS, Context.MODE_PRIVATE)
    }
    val shiftSpecialRules = remember { mutableStateMapOf<String, ShiftSpecialRule>() }
    val manualHolidayPrefs = remember {
        context.getSharedPreferences(PREFS_MANUAL_HOLIDAYS, Context.MODE_PRIVATE)
    }
    val calendarSyncPrefs = remember {
        context.getSharedPreferences(PREFS_CALENDAR_SYNC, Context.MODE_PRIVATE)
    }
    val widgetSettingsPrefs = remember {
        context.getSharedPreferences(PREFS_WIDGET_SETTINGS, Context.MODE_PRIVATE)
    }
    val manualHolidayRecords = remember { mutableStateListOf<ManualHolidayRecord>() }
    var widgetSettingsRefreshToken by remember { mutableStateOf(0) }

    LaunchedEffect(savedDays, shiftTemplates) {
        ShiftMonthWidgetProvider.requestUpdate(context)
    }
    Button(onClick = { newPayroll.calculateAndShow() }) {
        Text("🧪 Тест нового расчёта")
    }
    val editingAdditionalPayment = remember(editingAdditionalPaymentId, additionalPayments) {
        additionalPayments.firstOrNull { it.id == editingAdditionalPaymentId }
    }
    val editingDeduction = remember(editingDeductionId, deductions) {
        deductions.firstOrNull { it.id == editingDeductionId }
    }
    val editingShiftTemplate = remember(editingShiftTemplateCode, shiftTemplates) {
        shiftTemplates.firstOrNull { it.code == editingShiftTemplateCode }
    }
    val editingPattern = remember(editingPatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == editingPatternId }
    }
    val editingManualHoliday = remember(editingManualHolidayDate, manualHolidayRecords.toList()) {
        manualHolidayRecords.firstOrNull { it.date == editingManualHolidayDate }
    }
    val activePattern = remember(activePatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == activePatternId }
    }

    LaunchedEffect(currentMonth.year, holidays) {
        delay(700)

        if (isHolidaySyncing) return@LaunchedEffect

        val hasFederalYear = holidays.any { it.date.startsWith("${currentMonth.year}-") }

        isHolidaySyncing = true
        try {
            val result = checkAndSyncFederalCalendarIfChanged(
                holidaySyncRepository = holidaySyncRepository,
                prefs = calendarSyncPrefs,
                year = currentMonth.year,
                hasLocalYear = hasFederalYear,
                forceNetworkCheck = !hasFederalYear
            )
            holidaySyncMessage = result.message
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            holidaySyncMessage = if (hasFederalYear) {
                "Используется локальный календарь ${currentMonth.year}. Проверка не удалась: ${e.message ?: "неизвестно"}"
            } else {
                "Автозагрузка не удалась: ${e.message ?: "неизвестно"}"
            }
        } finally {
            isHolidaySyncing = false
        }
    }

    val patternRangeStartDate = remember(patternRangeStartIso) {
        patternRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingPatternRangeStartDate = remember(pendingPatternRangeStartIso) {
        pendingPatternRangeStartIso?.let { LocalDate.parse(it) }
    }

    val pendingPatternRangeEndDate = remember(pendingPatternRangeEndIso) {
        pendingPatternRangeEndIso?.let { LocalDate.parse(it) }
    }
    val applyingPattern = remember(applyingPatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == applyingPatternId }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existingTemplates = shiftTemplateDao.observeAll().first()
            if (existingTemplates.isEmpty()) {
                shiftTemplateDao.upsertAll(DefaultShiftTemplates.items())
            }
        }
    }
    LaunchedEffect(holidays) {
        if (holidays.isEmpty()) {
            holidayDao.upsertAll(FederalHolidaySeed.federal2026())
        }
    }

    LaunchedEffect(Unit) {
        manualHolidayRecords.clear()
        manualHolidayRecords.addAll(readManualHolidayRecords(manualHolidayPrefs))
    }

    LaunchedEffect(shiftTemplates) {
        if (shiftTemplates.isNotEmpty()) {
            shiftAlarmStore.synchronizeTemplates(shiftTemplates.alarmEligibleTemplates())
        }
    }

    val templateMap = remember(shiftTemplates) {
        shiftTemplates.associateBy { it.code }
    }

    LaunchedEffect(shiftTemplates) {
        shiftTemplates.forEach { template ->
            shiftSpecialRules[template.code] = readShiftSpecialRule(
                prefs = shiftSpecialPrefs,
                code = template.code,
                fallbackWeekendPaid = template.isWeekendPaid
            )
        }
    }

    val shiftSpecialRulesSnapshot = shiftSpecialRules.toMap()


    val editingShiftSpecialRule = remember(editingShiftTemplateCode, shiftSpecialRulesSnapshot, editingShiftTemplate) {
        editingShiftTemplate?.let { template ->
            shiftSpecialRulesSnapshot[template.code] ?: defaultShiftSpecialRule(template.isWeekendPaid)
        }
    }
    val editingShiftAlarmTemplateConfig = remember(editingShiftTemplateCode, shiftAlarmSettings, editingShiftTemplate) {
        editingShiftTemplate?.let { template ->
            shiftAlarmSettings.templateConfigs.firstOrNull { it.shiftCode == template.code }
        }
    }
    val holidayMap = remember(holidays) {
        holidays.associateBy { LocalDate.parse(it.date) }
    }
    val manualHolidayMap = remember(manualHolidayRecords.toList()) {
        manualHolidayRecords.associate { record ->
            LocalDate.parse(record.date) to record.toHolidayEntity()
        }
    }

    val resolvedHolidayMap = remember(currentMonth.year, holidayMap, manualHolidayMap) {
        buildMap {
            putAll(fixedFederalHolidayMap(currentMonth.year))
            putAll(holidayMap)
            putAll(manualHolidayMap)
        }
    }

    val extraDayOffDates = remember(resolvedHolidayMap) {
        resolvedHolidayMap.values
            .filter { it.isNonWorking }
            .map { LocalDate.parse(it.date) }
            .toSet()
    }

    val normMode = remember(payrollSettings.normMode) {
        runCatching { NormMode.valueOf(payrollSettings.normMode) }
            .getOrElse { NormMode.MANUAL }
    }

    val annualNormSourceMode = remember(payrollSettings.annualNormSourceMode) {
        runCatching { AnnualNormSourceMode.valueOf(payrollSettings.annualNormSourceMode) }
            .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    }

    remember(currentMonth, resolvedHolidayMap, payrollSettings.workdayHours) {
        calculateProductionCalendarMonthInfo(
            month = currentMonth,
            holidayMap = resolvedHolidayMap,
            workdayHours = payrollSettings.workdayHours
        )
    }

    val effectiveNormHours = remember(
        currentMonth,
        resolvedHolidayMap,
        payrollSettings,
        normMode,
        annualNormSourceMode
    ) {
        when (normMode) {
            NormMode.MANUAL -> payrollSettings.monthlyNormHours

            NormMode.PRODUCTION_CALENDAR -> {
                calculateProductionCalendarMonthInfo(
                    month = currentMonth,
                    holidayMap = resolvedHolidayMap,
                    workdayHours = payrollSettings.workdayHours
                ).normHours
            }

            NormMode.AVERAGE_ANNUAL -> {
                when (annualNormSourceMode) {
                    AnnualNormSourceMode.WORKDAY_HOURS -> {
                        calculateAverageAnnualNormHours(
                            year = currentMonth.year,
                            holidayMap = resolvedHolidayMap,
                            workdayHours = payrollSettings.workdayHours
                        )
                    }

                    AnnualNormSourceMode.YEAR_TOTAL_HOURS -> {
                        (payrollSettings.annualNormHours / 12.0).coerceAtLeast(0.0)
                    }
                }
            }

            NormMode.AVERAGE_QUARTERLY -> {
                calculateAverageQuarterNormHours(
                    month = currentMonth,
                    holidayMap = resolvedHolidayMap,
                    workdayHours = payrollSettings.workdayHours
                )
            }
        }
    }

    val benefitReferenceYear = remember { LocalDate.now().year }
    val defaultSickCalculationPeriodDays = remember(benefitReferenceYear) {
        calculateDefaultSickCalculationPeriodDays(benefitReferenceYear)
    }

    val effectivePayrollSettings = remember(payrollSettings, effectiveNormHours, defaultSickCalculationPeriodDays) {
        val computedVacationAverageDaily = if (payrollSettings.vacationAccruals12Months > 0.0) {
            calculateVacationAverageDailyFromAccruals(payrollSettings.vacationAccruals12Months)
        } else {
            payrollSettings.vacationAverageDaily
        }

        val resolvedSickCalculationPeriodDays = if (payrollSettings.sickCalculationPeriodDays > 0) {
            payrollSettings.sickCalculationPeriodDays
        } else {
            defaultSickCalculationPeriodDays
        }

        val hasDetailedSickInputs = payrollSettings.sickIncomeYear1 > 0.0 ||
                payrollSettings.sickIncomeYear2 > 0.0 ||
                payrollSettings.sickLimitYear1 > 0.0 ||
                payrollSettings.sickLimitYear2 > 0.0

        val computedSickAverageDaily = if (hasDetailedSickInputs) {
            calculateSickAverageDailyFromInputs(
                incomeYear1 = payrollSettings.sickIncomeYear1,
                incomeYear2 = payrollSettings.sickIncomeYear2,
                limitYear1 = payrollSettings.sickLimitYear1,
                limitYear2 = payrollSettings.sickLimitYear2,
                calculationPeriodDays = resolvedSickCalculationPeriodDays,
                excludedDays = payrollSettings.sickExcludedDays
            )
        } else {
            payrollSettings.sickAverageDaily
        }

        payrollSettings.copy(
            monthlyNormHours = effectiveNormHours,
            vacationAverageDaily = computedVacationAverageDaily,
            sickAverageDaily = computedSickAverageDaily,
            sickCalculationPeriodDays = resolvedSickCalculationPeriodDays
        )
    }

    val quickShiftTemplates = remember(shiftTemplates) {
        shiftTemplates
            .filter { it.active }
            .sortedBy { it.sortOrder }
    }

    val alarmEligibleTemplates = remember(shiftTemplates) {
        shiftTemplates.alarmEligibleTemplates()
    }

    val shiftCodesByDate = remember(savedDays) {
        savedDays.associate { LocalDate.parse(it.date) to it.shiftCode }
    }

    val summary = remember(
        shiftCodesByDate,
        currentMonth,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction
    ) {
        calculateSummary(
            shiftCodesByDate = shiftCodesByDate,
            month = currentMonth,
            templateMap = templateMap,
            holidayMap = resolvedHolidayMap,
            applyShortDayReduction = payrollSettings.applyShortDayReduction
        )
    }

    val monthEntries = remember(shiftCodesByDate, currentMonth) {
        shiftCodesByDate.filterKeys { YearMonth.from(it) == currentMonth }
    }

    val monthShifts = remember(
        monthEntries,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot
    ) {
        monthEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = payrollSettings.applyShortDayReduction,
                specialRule = shiftSpecialRulesSnapshot[code]
            )
        }
    }

    val firstHalfShifts = remember(
        monthEntries,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot
    ) {
        monthEntries
            .filterKeys { it.dayOfMonth <= 15 }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = payrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code]
                )
            }
    }

    val paymentResolution = remember(additionalPayments, currentMonth, monthShifts, firstHalfShifts) {
        resolveAdditionalPaymentsForMonth(
            configuredPayments = additionalPayments,
            month = currentMonth,
            shifts = monthShifts,
            firstHalfShifts = firstHalfShifts
        )
    }

    val payroll = remember(
        monthShifts,
        firstHalfShifts,
        effectivePayrollSettings,
        paymentResolution,
        deductions
    ) {
        PayrollCalculator.calculate(
            shifts = monthShifts,
            firstHalfShifts = firstHalfShifts,
            settings = effectivePayrollSettings,
            additionalPayments = paymentResolution.asPayrollPayments(),
            deductions = deductions
        )
    }

    val paymentDates = remember(currentMonth, effectivePayrollSettings, extraDayOffDates) {
        calculatePaymentDates(
            month = currentMonth,
            settings = effectivePayrollSettings,
            extraDayOffDates = extraDayOffDates
        )
    }

    val overtimePeriodInfo = remember(currentMonth, payrollSettings.overtimePeriod) {
        resolveOvertimePeriodInfo(currentMonth, payrollSettings.overtimePeriod)
    }

    val annualOvertime = remember(
        shiftCodesByDate,
        overtimePeriodInfo,
        effectivePayrollSettings,
        payrollSettings,
        normMode,
        annualNormSourceMode,
        templateMap,
        shiftSpecialRulesSnapshot,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction
    ) {
        val periodShifts = shiftCodesByDate
            .filterKeys { !it.isBefore(overtimePeriodInfo.startDate) && !it.isAfter(overtimePeriodInfo.endDate) }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = payrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code]
                )
            }

        val basePeriodNormHours = calculateNormHoursForPeriod(
            periodInfo = overtimePeriodInfo,
            payrollSettings = payrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = resolvedHolidayMap
        )

        val adjustedPeriodNormHours = calculateAdjustedNormHoursForPeriod(
            basePeriodNormHours = basePeriodNormHours,
            shifts = periodShifts,
            holidayMap = resolvedHolidayMap,
            workdayHours = payrollSettings.workdayHours,
            applyShortDayReduction = payrollSettings.applyShortDayReduction
        )

        PayrollCalculator.calculatePeriodOvertime(
            shifts = periodShifts,
            settings = effectivePayrollSettings,
            periodLabel = overtimePeriodInfo.label,
            periodStart = overtimePeriodInfo.startDate,
            periodEnd = overtimePeriodInfo.endDate,
            periodNormHours = adjustedPeriodNormHours
        )
    }

    val detailedShiftStats = remember(monthShifts, firstHalfShifts, paymentResolution, payroll, annualOvertime) {
        calculateDetailedShiftStats(
            shifts = monthShifts,
            firstHalfShifts = firstHalfShifts,
            paymentResolution = paymentResolution,
            payroll = payroll,
            annualOvertime = annualOvertime
        )
    }

    val resolvedAdditionalPaymentBreakdown = remember(paymentResolution.lines, payroll, effectivePayrollSettings) {
        calculateResolvedAdditionalPaymentBreakdown(
            resolvedPayments = paymentResolution.lines,
            payroll = payroll,
            payrollSettings = effectivePayrollSettings
        )
    }

    LaunchedEffect(savedDays, templateMap, shiftAlarmSettings) {
        shiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
            ShiftAlarmScheduler.reschedule(
                context = context,
                settings = shiftAlarmSettings,
                savedDays = savedDays,
                templateMap = templateMap
            )
        }
    }
    val payrollDetailedResult = remember(
        currentMonth,
        payroll,
        detailedShiftStats,
        effectivePayrollSettings,
        payrollSettings.housingPaymentLabel,
        payrollSettings.housingPaymentTaxable,
        resolvedAdditionalPaymentBreakdown
    ) {
        PayrollSheetDraftFactory.build(
            month = currentMonth,
            summary = payroll,
            detailedShiftStats = detailedShiftStats,
            payrollSettings = effectivePayrollSettings,
            housingPaymentLabel = payrollSettings.housingPaymentLabel,
            housingPaymentTaxable = payrollSettings.housingPaymentTaxable,
            resolvedAdditionalPaymentBreakdown = resolvedAdditionalPaymentBreakdown
        )
    }
    val shiftColors = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        val defaults = defaultShiftColors()
        defaults.forEach { (key, value) ->
            shiftColors[key] = shiftColorsPrefs.getInt(key, value)
        }
    }

    LaunchedEffect(shiftTemplates, savedDays, shiftAlarmSettings) {
        if (shiftTemplates.isEmpty()) return@LaunchedEffect
        if (migrationPrefs.getBoolean(KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1, false)) return@LaunchedEffect

        val usedShiftCodes = savedDays.map { it.shiftCode }.toSet()
        val alarmConfigsByCode = shiftAlarmSettings.templateConfigs.associateBy { it.shiftCode }
        val removableLegacyTemplates = shiftTemplates.filter { template ->
            template.matchesLegacyBuiltInWorkingTemplate() &&
                    template.code !in usedShiftCodes &&
                    !alarmConfigsByCode[template.code].isMeaningfullyCustomizedFor(template)
        }

        removableLegacyTemplates.forEach { template ->
            shiftTemplateDao.delete(template)
            shiftColorsPrefs.edit().remove(template.code).apply()
            shiftColors.remove(template.code)
            removeShiftSpecialRule(
                shiftSpecialRules = shiftSpecialRules,
                shiftSpecialPrefs = shiftSpecialPrefs,
                code = template.code
            )
            shiftAlarmStore.removeTemplateConfig(template.code)
        }

        if (activeBrushCode != null && removableLegacyTemplates.any { it.code == activeBrushCode }) {
            activeBrushCode = null
        }

        val storedPayrollSettings = readPayrollSettingsFromPrefs(payrollSettingsPrefs)
        if (storedPayrollSettings.matchesLikelyLegacyEmbeddedPayrollDefaults()) {
            payrollSettingsStore.save(
                storedPayrollSettings.copy(
                    baseSalary = 0.0,
                    extraSalary = 0.0
                )
            )
        }

        migrationPrefs.edit()
            .putBoolean(KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1, true)
            .apply()
    }

    val backupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingBackupJsonContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
                backupRestoreStatusMessage = "Резервная копия сохранена"
            }.onFailure { error ->
                backupRestoreStatusMessage = "Не удалось сохранить копию: ${error.message ?: "неизвестно"}"
            }
        }
        pendingBackupJsonContent = null
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching {
                restoreBackupFromUri(
                    context = context,
                    uri = uri,
                    existingShiftTemplates = shiftTemplates,
                    existingSavedDays = savedDays,
                    manualHolidayPrefs = manualHolidayPrefs,
                    shiftColorsPrefs = shiftColorsPrefs,
                    manualHolidayRecords = manualHolidayRecords,
                    shiftColors = shiftColors,
                    upsertShiftTemplate = { template -> shiftTemplateDao.upsert(template) },
                    deleteShiftTemplate = { template -> shiftTemplateDao.delete(template) },
                    upsertShiftDay = { day -> shiftDayDao.upsert(day) },
                    deleteShiftDayByDate = { date -> shiftDayDao.deleteByDate(date) },
                    onStatus = { message -> backupRestoreStatusMessage = message },
                    onAfterImport = { (context as? Activity)?.recreate() }
                )
            }.onFailure { error ->
                backupRestoreStatusMessage =
                    "Не удалось восстановить копию: ${error.message ?: "неизвестно"}"
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isLandscape) {
                AppBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTabName = it.name }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                AppNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTabName = it.name }
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(220)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(280)
                                ) + fadeOut(animationSpec = tween(180))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(280)
                        ) + fadeIn(animationSpec = tween(220)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(280)
                                ) + fadeOut(animationSpec = tween(180))
                    }
                },
                label = "tab_content",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    BottomTab.CALENDAR -> {
                        CalendarTab(
                            currentMonth = currentMonth,
                            onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                            onPickMonth = { pickedMonth ->
                                currentMonth = pickedMonth
                            },
                            shiftCodesByDate = shiftCodesByDate,
                            templateMap = templateMap,
                            shiftColors = shiftColors,
                            quickShiftTemplates = quickShiftTemplates,
                            quickPickerOpen = quickPickerOpen,
                            activeBrushCode = activeBrushCode,
                            holidayMap = resolvedHolidayMap,
                            isLegendExpanded = isLegendExpanded,
                            onToggleLegend = { isLegendExpanded = !isLegendExpanded },
                            onOpenColorSettings = { showColorSettings = true },
                            onToggleQuickPicker = { quickPickerOpen = !quickPickerOpen },
                            onCloseQuickPicker = { quickPickerOpen = false },
                            pendingPatternRangeStartDate = pendingPatternRangeStartDate,
                            pendingPatternRangeEndDate = pendingPatternRangeEndDate,
                            onOpenPatternPreview = {
                                if (pendingPatternRangeStartDate != null && pendingPatternRangeEndDate != null) {
                                    showPatternPreviewDialog = true
                                }
                            },
                            onSelectBrush = { code ->
                                activeBrushCode = code
                                quickPickerOpen = false
                            },
                            onClearBrush = {
                                activeBrushCode = BRUSH_CLEAR
                                quickPickerOpen = false
                            },
                            onDisableBrush = {
                                activeBrushCode = null
                                quickPickerOpen = false
                            },
                            onAddNewShift = {
                                editingShiftTemplateCode = null
                                showShiftTemplateEditDialog = true
                                quickPickerOpen = false
                            },
                            onOpenPatternEditor = {
                                showPatternQuickPicker = true
                                quickPickerOpen = false
                            },
                            onEraseDate = { date ->
                                scope.launch {
                                    shiftDayDao.deleteByDate(date.toString())
                                }
                            },
                            activePattern = activePattern,
                            patternRangeStartDate = patternRangeStartDate,
                            onCancelPatternMode = {
                                activePatternId = null
                                patternRangeStartIso = null
                            },
                            onDayClick = { date ->
                                if (YearMonth.from(date) != currentMonth) {
                                    currentMonth = YearMonth.from(date)
                                }
                                when {
                                    activePattern != null -> {
                                        val start = patternRangeStartIso?.let { LocalDate.parse(it) }

                                        if (start == null) {
                                            patternRangeStartIso = date.toString()
                                        } else {
                                            val rangeStart = minOf(start, date)
                                            val rangeEnd = maxOf(start, date)

                                            pendingPatternRangeStartIso = rangeStart.toString()
                                            pendingPatternRangeEndIso = rangeEnd.toString()
                                            showPatternPreviewDialog = true
                                            patternRangeStartIso = null
                                        }
                                    }

                                    activeBrushCode == null -> {
                                        selectedDate = date
                                    }

                                    activeBrushCode == BRUSH_CLEAR -> {
                                        scope.launch {
                                            shiftDayDao.deleteByDate(date.toString())
                                        }
                                    }

                                    else -> {
                                        scope.launch {
                                            shiftDayDao.upsert(
                                                ShiftDayEntity(
                                                    date = date.toString(),
                                                    shiftCode = activeBrushCode!!
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.PAYROLL -> {
                        PayrollTab(
                            currentMonth = currentMonth,
                            onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                            onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                            summary = summary,
                            payroll = payroll,
                            payrollDetailedResult = payrollDetailedResult,
                            annualOvertime = annualOvertime,
                            paymentDates = paymentDates,
                            housingPaymentLabel = payrollSettings.housingPaymentLabel,
                            detailedShiftStats = detailedShiftStats,
                            isSummaryExpanded = isSummaryExpanded,
                            onToggleSummary = { isSummaryExpanded = !isSummaryExpanded },
                            onOpenSettings = { showPayrollSettings = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.PAYMENTS -> {
                        PaymentsTab(
                            currentMonth = currentMonth,
                            onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                            onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                            payroll = payroll,
                            annualOvertime = annualOvertime,
                            paymentDates = paymentDates,
                            housingPaymentLabel = payrollSettings.housingPaymentLabel,
                            additionalPayments = additionalPayments,
                            resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                            detailedShiftStats = detailedShiftStats,
                            onAddPayment = {
                                editingAdditionalPaymentId = null
                                showAdditionalPaymentDialog = true
                            },
                            onEditPayment = { payment ->
                                editingAdditionalPaymentId = payment.id
                                showAdditionalPaymentDialog = true
                            },
                            onDeletePayment = { payment ->
                                scope.launch {
                                    additionalPaymentsStore.deleteById(payment.id)
                                }
                            },
                            onOpenMonthlyReport = {
                                showMonthlyReport = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    BottomTab.ALARMS -> {
                        ShiftAlarmsTab(
                            settings = shiftAlarmSettings,
                            shiftTemplates = alarmEligibleTemplates,
                            lastRescheduleResult = shiftAlarmRescheduleResult,
                            canScheduleExactAlarms = ShiftAlarmScheduler.canScheduleExactShiftAlarms(context),
                            notificationPermissionGranted = ShiftAlarmScheduler.hasNotificationPermission(context),
                            canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= 34) {
                                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
                            } else {
                                true
                            },
                            onOpenFullScreenIntentSettings = {
                                if (Build.VERSION.SDK_INT >= 34) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            },
                            onSave = { newSettings ->
                                scope.launch {
                                    shiftAlarmStore.save(newSettings)
                                    shiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
                                        ShiftAlarmScheduler.reschedule(
                                            context = context,
                                            settings = newSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap
                                        )
                                    }
                                }
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onOpenExactAlarmSettings = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            onRescheduleNow = {
                                scope.launch {
                                    shiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
                                        ShiftAlarmScheduler.reschedule(
                                            context = context,
                                            settings = shiftAlarmSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    BottomTab.SHIFTS -> {
                        TemplatesScreen(
                            mode = templateMode,
                            templates = shiftTemplates.sortedBy { it.sortOrder },
                            specialRules = shiftSpecialRulesSnapshot,
                            patterns = patternTemplates,
                            onModeChange = { templateModeName = it.name },
                            onBack = { selectedTabName = BottomTab.CALENDAR.name },
                            onAddShift = {
                                editingShiftTemplateCode = null
                                showShiftTemplateEditDialog = true
                            },
                            onEditShift = { template ->
                                editingShiftTemplateCode = template.code
                                showShiftTemplateEditDialog = true
                            },
                            onAddPattern = {
                                editingPatternId = null
                                showPatternEditDialog = true
                            },
                            onEditPattern = { pattern ->
                                editingPatternId = pattern.id
                                showPatternEditDialog = true
                            },
                            onApplyPattern = { pattern ->
                                applyingPatternId = pattern.id
                                showPatternApplyDialog = true
                            },
                            onDeletePattern = { pattern ->
                                scope.launch {
                                    patternTemplatesStore.deleteById(pattern.id)
                                }
                            }
                        )
                    }
                    BottomTab.SETTINGS -> {
                        SettingsTab(
                            payrollSettings = payrollSettings,
                            additionalPaymentsCount = additionalPayments.size,
                            deductionsCount = deductions.size,
                            onOpenDeductions = { showDeductionsScreen = true },
                            manualHolidayCount = manualHolidayRecords.size,
                            isHolidaySyncing = isHolidaySyncing,
                            holidaySyncMessage = holidaySyncMessage,
                            onOpenPayrollSettings = { showPayrollSettings = true },
                            onOpenColorSettings = { showColorSettings = true },
                            onOpenPayments = { showAdditionalPaymentsScreen = true },
                            onOpenCurrentParameters = { showCurrentParameters = true },
                            onOpenManualHolidays = { showManualHolidaysScreen = true },
                            onOpenBackupRestore = { showBackupRestoreScreen = true },
                            onOpenExcelImport = { showExcelImportScreen = true },
                            onOpenWidgetSettings = { showWidgetSettingsScreen = true },
                            onSyncProductionCalendar = {
                                lifecycleOwner.lifecycleScope.launch {
                                    isHolidaySyncing = true
                                    holidaySyncMessage = "Проверка календаря ${currentMonth.year}..."
                                    try {
                                        val hasFederalYear = holidays.any { it.date.startsWith("${currentMonth.year}-") }
                                        val result = checkAndSyncFederalCalendarIfChanged(
                                            holidaySyncRepository = holidaySyncRepository,
                                            prefs = calendarSyncPrefs,
                                            year = currentMonth.year,
                                            hasLocalYear = hasFederalYear,
                                            forceNetworkCheck = true
                                        )
                                        holidaySyncMessage = result.message
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        holidaySyncMessage = "Ошибка обновления: ${e.message ?: "неизвестно"}"
                                    } finally {
                                        isHolidaySyncing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    Button(onClick = { newPayroll.calculateAndShow() }) {
        Text("🧪 Тест нового расчёта")
    }
    AnimatedFullscreenOverlay(visible = showMonthlyReport) {
        MonthlyReportScreen(
            currentMonth = currentMonth,
            payrollSettings = effectivePayrollSettings,
            payroll = payroll,
            annualOvertime = annualOvertime,
            paymentDates = paymentDates,
            housingPaymentLabel = payrollSettings.housingPaymentLabel,
            additionalPayments = additionalPayments,
            resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
            detailedShiftStats = detailedShiftStats,
            onBack = { showMonthlyReport = false },
            onExportCsv = {
                pendingReportCsvContent = buildMonthlyReportCsv(
                    currentMonth = currentMonth,
                    payrollSettings = effectivePayrollSettings,
                    payroll = payroll,
                    annualOvertime = annualOvertime,
                    paymentDates = paymentDates,
                    housingPaymentLabel = payrollSettings.housingPaymentLabel,
                    additionalPayments = additionalPayments,
                    resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                    detailedShiftStats = detailedShiftStats
                )
                pendingReportCsvFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.csv"
                reportCsvLauncher.launch(pendingReportCsvFileName)
            }
        )
    }

    // Вместо: newPayroll.calculateAndShow()
// Используем:
    val payrollIntegration = remember { NewPayrollIntegration(context, scope, db) }

// Кнопка для теста нового расчёта (можно убрать после теста)
    Button(onClick = {
        payrollIntegration.calculateSimple(
            onResult = { gross, advance, main, net, error ->
                newPayrollGross = gross
                newPayrollAdvance = advance
                newPayrollMain = main
                newPayrollNet = net
                newPayrollError = error
                showNewPayrollDialog = true
            }
        )
    }) {
        Text("🧪 Тест нового расчёта")
    }

    selectedDate?.let { date ->
        ShiftPickerDialog(
            date = date,
            currentShiftCode = shiftCodesByDate[date],
            shiftTemplates = shiftTemplates.filter { it.active }.sortedBy { it.sortOrder },
            templateMap = templateMap,
            holidayMap = resolvedHolidayMap,
            onDismiss = { selectedDate = null },
            onSelectShiftCode = { code ->
                scope.launch {
                    shiftDayDao.upsert(
                        ShiftDayEntity(
                            date = date.toString(),
                            shiftCode = code
                        )
                    )
                }
                selectedDate = null
            },
            onClearShift = {
                scope.launch {
                    shiftDayDao.deleteByDate(date.toString())
                }
                selectedDate = null
            }
        )
    }

    if (showColorSettings) {
        ColorSettingsDialog(
            shiftTemplates = shiftTemplates.sortedBy { it.sortOrder },
            shiftColors = shiftColors,
            onDismiss = { showColorSettings = false },
            onColorSelected = { key, colorValue ->
                saveShiftColor(
                    shiftColors = shiftColors,
                    shiftColorsPrefs = shiftColorsPrefs,
                    context = context,
                    key = key,
                    colorValue = colorValue
                )
            },
            onResetDefaults = {
                resetShiftColors(
                    shiftColors = shiftColors,
                    shiftColorsPrefs = shiftColorsPrefs,
                    context = context
                )
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showPayrollSettings) {
        PayrollSettingsDialog(
            currentSettings = payrollSettings,
            onDismiss = { showPayrollSettings = false },
            onSave = { newSettings ->
                scope.launch {
                    payrollSettingsStore.save(newSettings)
                }
                showPayrollSettings = false
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showCurrentParameters) {
        CurrentParametersScreen(
            payrollSettings = effectivePayrollSettings,
            onBack = { showCurrentParameters = false }
        )
    }

    AnimatedFullscreenOverlay(visible = showManualHolidaysScreen) {
        ManualHolidaysScreen(
            records = manualHolidayRecords.sortedBy { it.date },
            onBack = { showManualHolidaysScreen = false },
            onAdd = {
                editingManualHolidayDate = null
                showManualHolidayDialog = true
            },
            onEdit = { record ->
                editingManualHolidayDate = record.date
                showManualHolidayDialog = true
            },
            onDelete = { record ->
                deleteManualHoliday(
                    manualHolidayRecords = manualHolidayRecords,
                    manualHolidayPrefs = manualHolidayPrefs,
                    date = record.date
                )
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showBackupRestoreScreen) {
        BackupRestoreScreen(
            shiftDaysCount = savedDays.size,
            shiftTemplatesCount = shiftTemplates.size,
            additionalPaymentsCount = additionalPayments.size,
            patternTemplatesCount = patternTemplates.size,
            manualHolidayCount = manualHolidayRecords.size,
            statusMessage = backupRestoreStatusMessage,
            onBack = { showBackupRestoreScreen = false },
            onExport = {
                pendingBackupJsonContent = buildBackupJsonForExport(
                    prefSnapshots = listOf(
                        PREF_NAME_PAYROLL_SETTINGS to payrollSettingsPrefs,
                        PREF_NAME_ADDITIONAL_PAYMENTS to additionalPaymentsPrefs,
                        PREF_NAME_PATTERN_TEMPLATES to patternTemplatesPrefs,
                        PREF_NAME_SHIFT_ALARM_SETTINGS to shiftAlarmSettingsPrefs,
                        PREF_NAME_SHIFT_COLORS to shiftColorsPrefs,
                        PREF_NAME_SHIFT_SPECIAL_RULES to shiftSpecialPrefs,
                        PREF_NAME_MANUAL_HOLIDAYS to manualHolidayPrefs
                    ),
                    shiftDays = savedDays,
                    shiftTemplates = shiftTemplates
                )
                pendingBackupFileName = "ShiftSalaryPlanner_backup_${LocalDate.now()}.json"
                backupJsonLauncher.launch(pendingBackupFileName)
            },
            onImport = {
                backupImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showExcelImportScreen) {
        ExcelImportScreen(
            fileName = pendingExcelFileName,
            preview = excelImportPreview,
            candidates = excelImportCandidates,
            statusMessage = excelImportStatusMessage,
            onBack = { showExcelImportScreen = false },
            onPickFile = {
                excelImportFileLauncher.launch(
                    arrayOf(
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel.sheet.macroEnabled.12",
                        "*/*"
                    )
                )
            },
            onAnalyze = { request, selectedFullName ->
                val bytes = pendingExcelFileBytes
                if (bytes == null) {
                    excelImportStatusMessage = "Сначала выбери Excel-файл"
                } else {
                    scope.launch {
                        runCatching {
                            excelScheduleParser.parse(
                                inputStream = bytes.inputStream(),
                                request = request.copy(selectedFullName = selectedFullName),
                                existingTemplates = shiftTemplates
                            )
                        }.onSuccess { result ->
                            when (result) {
                                is ExcelImportParseResult.CandidateSelectionRequired -> {
                                    excelImportCandidates = result.candidates
                                    excelImportPreview = null
                                    excelImportStatusMessage = "Найдено несколько сотрудников с этой фамилией. Выбери нужного."
                                }
                                is ExcelImportParseResult.Preview -> {
                                    excelImportCandidates = emptyList()
                                    excelImportPreview = result.preview
                                    excelImportStatusMessage = buildString {
                                        append("Готово к импорту: ")
                                        append(result.preview.importedDays.size)
                                        append(" дней • месяцев: ")
                                        append(result.preview.selectedMonths.joinToString())
                                        if (result.preview.templatesToCreate.isNotEmpty()) {
                                            append(" • новых шаблонов: ")
                                            append(result.preview.templatesToCreate.size)
                                        }
                                    }
                                }
                            }
                        }.onFailure { error ->
                            excelImportPreview = null
                            excelImportCandidates = emptyList()
                            excelImportStatusMessage = "Ошибка анализа: ${error.message ?: "неизвестно"}"
                        }
                    }
                }
            },
            onImport = { preview ->
                scope.launch {
                    runCatching {
                        preview.selectedMonths.sorted().forEach { month ->
                            val start = LocalDate.of(preview.year, month, 1)
                            val end = YearMonth.of(preview.year, month).atEndOfMonth()
                            excelScheduleImporter.clearPeriod(start, end)
                        }
                        excelScheduleImporter.import(preview)
                        preview.templatesToCreate.forEach { template ->
                            shiftColors[template.code] = parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                        }
                    }.onSuccess {
                        excelImportStatusMessage = "Импорт завершён: ${preview.importedDays.size} дней"
                        excelImportPreview = null
                        excelImportCandidates = emptyList()
                    }.onFailure { error ->
                        excelImportStatusMessage = "Ошибка импорта: ${error.message ?: "неизвестно"}"
                    }
                }
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showWidgetSettingsScreen) {
        WidgetSettingsScreen(
            prefs = widgetSettingsPrefs,
            refreshToken = widgetSettingsRefreshToken,
            shiftTemplates = shiftTemplates.sortedBy { it.sortOrder },
            shiftColors = shiftColors,
            onBack = { showWidgetSettingsScreen = false },
            onSaveThemeMode = { themeMode ->
                writeWidgetThemeMode(widgetSettingsPrefs, themeMode)
                widgetSettingsRefreshToken++
                ShiftMonthWidgetProvider.requestUpdate(context)
            },
            onSaveShiftOverride = { shiftCode, override ->
                writeWidgetShiftOverride(widgetSettingsPrefs, shiftCode, override)
                widgetSettingsRefreshToken++
                ShiftMonthWidgetProvider.requestUpdate(context)
            },
            onResetShiftOverride = { shiftCode ->
                clearWidgetShiftOverride(widgetSettingsPrefs, shiftCode)
                widgetSettingsRefreshToken++
                ShiftMonthWidgetProvider.requestUpdate(context)
            }
        )
    }

    if (showManualHolidayDialog) {
        ManualHolidayDialog(
            currentRecord = editingManualHoliday,
            onDismiss = {
                showManualHolidayDialog = false
                editingManualHolidayDate = null
            },
            onSave = { record ->
                if (editingManualHolidayDate != null && editingManualHolidayDate != record.date) {
                    deleteManualHoliday(
                        manualHolidayRecords = manualHolidayRecords,
                        manualHolidayPrefs = manualHolidayPrefs,
                        date = editingManualHolidayDate!!
                    )
                }
                saveManualHoliday(
                    manualHolidayRecords = manualHolidayRecords,
                    manualHolidayPrefs = manualHolidayPrefs,
                    record = record
                )
                showManualHolidayDialog = false
                editingManualHolidayDate = null
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showAdditionalPaymentsScreen) {
        AdditionalPaymentsManagementScreen(
            payments = additionalPayments,
            onBack = { showAdditionalPaymentsScreen = false },
            onAddPayment = {
                editingAdditionalPaymentId = null
                showAdditionalPaymentDialog = true
            },
            onEditPayment = { payment ->
                editingAdditionalPaymentId = payment.id
                showAdditionalPaymentDialog = true
            },
            onDeletePayment = { payment ->
                scope.launch {
                    additionalPaymentsStore.deleteById(payment.id)
                }
            }
        )
    }
    AnimatedFullscreenOverlay(visible = showDeductionsScreen) {
        DeductionsManagementScreen(
            deductions = deductions,
            onBack = { showDeductionsScreen = false },
            onAddDeduction = {
                editingDeductionId = null
                showDeductionEditorScreen = true
            },
            onEditDeduction = { deduction ->
                editingDeductionId = deduction.id
                showDeductionEditorScreen = true
            },
            onDeleteDeduction = { deduction ->
                scope.launch {
                    deductionsStore.deleteById(deduction.id)
                }
            },
            onToggleActive = { deduction, active ->
                scope.launch {
                    deductionsStore.setActive(deduction.id, active)
                }
            }
        )
    }
    if (showAdditionalPaymentDialog) {
        AdditionalPaymentDialog(
            currentPayment = editingAdditionalPayment,
            currentMonth = currentMonth,
            onDismiss = {
                showAdditionalPaymentDialog = false
                editingAdditionalPaymentId = null
            },
            onSave = { payment ->
                scope.launch {
                    additionalPaymentsStore.addOrUpdate(payment)
                }
                showAdditionalPaymentDialog = false
                editingAdditionalPaymentId = null
            }
        )
    }
    AnimatedFullscreenOverlay(visible = showDeductionEditorScreen) {
        DeductionEditorScreen(
            currentDeduction = editingDeduction,
            onBack = {
                showDeductionEditorScreen = false
                editingDeductionId = null
            },
            onSave = { deduction ->
                deductionsStore.addOrUpdate(deduction)
                showDeductionEditorScreen = false
                editingDeductionId = null
            }
        )
    }
    AnimatedFullscreenOverlay(visible = showShiftTemplateEditDialog) {
        ShiftTemplateEditorScreen(
            currentTemplate = editingShiftTemplate,
            currentSpecialRule = editingShiftSpecialRule,
            currentAlarmTemplateConfig = editingShiftAlarmTemplateConfig,
            onBack = {
                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
            },
            onSave = { template, alarmTemplateConfig ->
                val oldTemplate = editingShiftTemplate
                val oldCode = oldTemplate?.code

                scope.launch {
                    shiftTemplateDao.upsert(template)

                    if (oldTemplate != null && oldCode != null && oldCode != template.code) {
                        savedDays
                            .filter { it.shiftCode == oldCode }
                            .forEach { day ->
                                shiftDayDao.upsert(day.copy(shiftCode = template.code))
                            }

                        shiftTemplateDao.delete(oldTemplate)

                        shiftColorsPrefs.edit().remove(oldCode).apply()
                        shiftColors.remove(oldCode)
                        removeShiftSpecialRule(
                            shiftSpecialRules = shiftSpecialRules,
                            shiftSpecialPrefs = shiftSpecialPrefs,
                            code = oldCode
                        )
                        shiftAlarmStore.removeTemplateConfig(oldCode)
                    }

                    saveShiftColor(
                        shiftColors = shiftColors,
                        shiftColorsPrefs = shiftColorsPrefs,
                        context = context,
                        key = template.code,
                        colorValue = parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                    )
                    shiftAlarmStore.upsertTemplateConfig(alarmTemplateConfig.copy(shiftCode = template.code))
                }

                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
            },
            onSaveSpecialRule = { code, rule ->
                saveShiftSpecialRule(
                    shiftSpecialRules = shiftSpecialRules,
                    shiftSpecialPrefs = shiftSpecialPrefs,
                    code = code,
                    rule = rule
                )
            },
            onDelete = { template ->
                scope.launch {
                    shiftTemplateDao.delete(template)

                    savedDays
                        .filter { it.shiftCode == template.code }
                        .forEach { day ->
                            shiftDayDao.deleteByDate(day.date)
                        }

                    shiftColorsPrefs.edit().remove(template.code).apply()
                    shiftColors.remove(template.code)
                    removeShiftSpecialRule(
                        shiftSpecialRules = shiftSpecialRules,
                        shiftSpecialPrefs = shiftSpecialPrefs,
                        code = template.code
                    )
                    shiftAlarmStore.removeTemplateConfig(template.code)
                }

                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
            }
        )
    }
    if (showPatternListDialog) {
        PatternListDialog(
            patterns = patternTemplates,
            onDismiss = { showPatternListDialog = false },
            onAddNew = {
                editingPatternId = null
                showPatternEditDialog = true
            },
            onEdit = { pattern ->
                editingPatternId = pattern.id
                showPatternEditDialog = true
            },
            onApply = { pattern ->
                applyingPatternId = pattern.id
                showPatternApplyDialog = true
            },
            onDelete = { pattern ->
                scope.launch {
                    patternTemplatesStore.deleteById(pattern.id)
                }
            }
        )
    }
    if (showPatternApplyDialog && applyingPattern != null) {
        PatternApplyDialog(
            currentPattern = applyingPattern,
            currentMonth = currentMonth,
            onDismiss = {
                showPatternApplyDialog = false
                applyingPatternId = null
            },
            onApply = { cycleStartDate ->
                scope.launch {
                    applyPatternToMonth(
                        shiftDayDao = shiftDayDao,
                        pattern = applyingPattern,
                        cycleStartDate = cycleStartDate,
                        month = currentMonth,
                        validShiftCodes = shiftTemplates.map { it.code }.toSet()
                    )
                }
                showPatternApplyDialog = false
                applyingPatternId = null
                selectedTabName = BottomTab.CALENDAR.name
            }
        )
    }
    if (showPatternEditDialog) {
        PatternEditDialog(
            currentPattern = editingPattern,
            shiftTemplates = shiftTemplates.filter { it.active }.sortedBy { it.sortOrder },
            onDismiss = {
                showPatternEditDialog = false
                editingPatternId = null
            },
            onSave = { pattern ->
                scope.launch {
                    patternTemplatesStore.addOrUpdate(pattern)
                }
                showPatternEditDialog = false
                editingPatternId = null
            }
        )
    }
    if (showPatternQuickPicker) {
        PatternQuickPickerDialog(
            patterns = patternTemplates,
            onDismiss = { showPatternQuickPicker = false },
            onSelect = { pattern ->
                activePatternId = pattern.id
                patternRangeStartIso = null
                activeBrushCode = null
                showPatternQuickPicker = false
                selectedTabName = BottomTab.CALENDAR.name
            },
            onOpenManager = {
                showPatternQuickPicker = false
                showPatternListDialog = true
            }
        )
    }
    if (
        showPatternPreviewDialog &&
        activePattern != null &&
        pendingPatternRangeStartDate != null &&
        pendingPatternRangeEndDate != null
    ) {
        PatternApplyPreviewDialog(
            currentPattern = activePattern,
            rangeStart = pendingPatternRangeStartDate,
            rangeEnd = pendingPatternRangeEndDate,
            onDismiss = {
                showPatternPreviewDialog = false
                pendingPatternRangeStartIso = null
                pendingPatternRangeEndIso = null
            },
            onApply = { phaseOffset ->
                scope.launch {
                    applyPatternToRange(
                        shiftDayDao = shiftDayDao,
                        pattern = activePattern,
                        rangeStart = pendingPatternRangeStartDate,
                        rangeEnd = pendingPatternRangeEndDate,
                        validShiftCodes = shiftTemplates.map { it.code }.toSet(),
                        phaseOffset = phaseOffset
                    )
                }

                showPatternPreviewDialog = false
                pendingPatternRangeStartIso = null
                pendingPatternRangeEndIso = null
                activePatternId = null
                selectedTabName = BottomTab.CALENDAR.name
            }
        )

    }
    // Диалог результата нового расчёта
    if (showNewPayrollDialog) {
        AlertDialog(
            onDismissRequest = { showNewPayrollDialog = false },
            title = {
                Text(
                    if (newPayrollError != null) "❌ Ошибка" else "📊 Расчёт зарплаты",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (newPayrollError != null) {
                        Text(
                            text = newPayrollError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("💰 Аванс: ${String.format("%,.2f", newPayrollAdvance)} ₽")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("💵 Основная: ${String.format("%,.2f", newPayrollMain)} ₽")
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "📈 Всего к выплате: ${String.format("%,.2f", newPayrollNet)} ₽",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewPayrollDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun SettingsValueNavigationCard(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "›", style = MaterialTheme.typography.titleLarge)
    }
}


@Composable
fun ShiftTemplateAlarmConfigCard(
    template: ShiftTemplateEntity,
    config: ShiftTemplateAlarmConfig,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onConfigChange: (ShiftTemplateAlarmConfig) -> Unit,
    onAddAlarm: () -> Unit,
    onEditAlarm: (ShiftAlarmConfig) -> Unit,
    onDeleteAlarm: (ShiftAlarmConfig) -> Unit
) {
    val activeAlarmCount = config.alarms.count { it.enabled }
    val chipColor = Color(parseColorHex(template.colorHex, 0xFF42A5F5.toInt()))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleExpanded() }
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconGlyph(template.iconKey, template.code),
                        color = readableContentColor(chipColor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shiftAlarmTemplateLabel(template),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AlarmCountChip(text = if (config.enabled) "Вкл" else "Выкл")
                        AlarmCountChip(text = "Всего ${config.alarms.size}")
                        AlarmCountChip(text = "Активных $activeAlarmCount")
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Switch(
                        modifier = Modifier.scale(0.82f),
                        checked = config.enabled,
                        onCheckedChange = { checked ->
                            val updated = if (checked && config.alarms.isEmpty()) {
                                defaultShiftTemplateAlarmConfig(template).copy(
                                    shiftCode = config.shiftCode,
                                    enabled = true,
                                    startHour = config.startHour,
                                    startMinute = config.startMinute,
                                    endHour = config.endHour,
                                    endMinute = config.endMinute
                                )
                            } else {
                                config.copy(enabled = checked)
                            }
                            onConfigChange(updated)
                        }
                    )

                    Text(
                        text = if (expanded) "▾" else "▸",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (config.alarms.isEmpty()) {
                                "Будильников пока нет"
                            } else {
                                "Будильников: ${config.alarms.size}"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onAddAlarm,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("+ Будильник")
                    }
                }

                if (config.alarms.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Добавь один или несколько будильников для этой смены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    config.alarms
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                        .forEach { alarm ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ShiftTemplateAlarmItemCard(
                                alarm = alarm,
                                onToggleEnabled = { checked ->
                                    onConfigChange(
                                        config.copy(
                                            alarms = config.alarms.map {
                                                if (it.id == alarm.id) it.copy(enabled = checked) else it
                                            }
                                        )
                                    )
                                },
                                onEdit = { onEditAlarm(alarm) },
                                onDelete = { onDeleteAlarm(alarm) }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun AlarmCountChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ShiftTemplateAlarmItemCard(
    alarm: ShiftAlarmConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = appPanelColor(),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlarmTimeBadge(
                text = formatClockHm(alarm.triggerHour, alarm.triggerMinute)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.title.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlarmMetaChip(text = "${alarm.volumePercent.coerceIn(0, 100)}%")
                    AlarmMetaChip(text = shiftAlarmSoundSummary(alarm))
                }
            }

            Switch(
                modifier = Modifier.scale(0.76f),
                checked = alarm.enabled,
                onCheckedChange = onToggleEnabled
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("✏️")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("🗑️")
                }
            }
        }
    }
}

@Composable
private fun AlarmTimeBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AlarmMetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ShiftTemplateAlarmEditDialog(
    template: ShiftTemplateEntity,
    currentAlarm: ShiftAlarmConfig?,
    onDismiss: () -> Unit,
    onSave: (ShiftAlarmConfig) -> Unit
) {
    val context = LocalContext.current
    val defaultAlarmClock = remember(currentAlarm?.id, template.code) {
        if (currentAlarm != null) {
            currentAlarm.triggerHour to currentAlarm.triggerMinute
        } else {
            resolveAlarmClockFromShiftStart(
                startHour = 8,
                startMinute = 0,
                minutesBefore = if (template.nightHours > 0.0) 90 else 60
            )
        }
    }
    var titleText by remember(currentAlarm?.id) {
        mutableStateOf(
            currentAlarm?.title ?: defaultShiftAlarmTitle(
                shiftAlarmTemplateLabel(template),
                defaultAlarmClock.first,
                defaultAlarmClock.second
            )
        )
    }
    var triggerHour by remember(currentAlarm?.id) {
        mutableStateOf((currentAlarm?.triggerHour ?: defaultAlarmClock.first).coerceIn(0, 23))
    }
    var triggerMinute by remember(currentAlarm?.id) {
        mutableStateOf((currentAlarm?.triggerMinute ?: defaultAlarmClock.second).coerceIn(0, 59))
    }
    var volumePercent by remember(currentAlarm?.id) {
        mutableStateOf((currentAlarm?.volumePercent ?: 100).coerceIn(0, 100))
    }
    var soundUriText by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.soundUri ?: "")
    }
    var soundLabelText by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.soundLabel ?: "")
    }
    var enabled by remember(currentAlarm?.id) {
        mutableStateOf(currentAlarm?.enabled ?: true)
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            soundUriText = uri.toString()
            soundLabelText = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast(':')
                ?.ifBlank { "Свой файл" }
                ?: "Свой файл"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (currentAlarm == null) "Новый будильник" else "Редактировать будильник"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = shiftAlarmTemplateLabel(template),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Время срабатывания",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShiftAlarmWheelTimePicker(
                    hour = triggerHour,
                    minute = triggerMinute,
                    onHourChange = { triggerHour = it },
                    onMinuteChange = { triggerMinute = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выбрано: ${formatClockHm(triggerHour, triggerMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Громкость: ${volumePercent.coerceIn(0, 100)}% от системной громкости будильников",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { volumePercent = it.toInt().coerceIn(0, 100) },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Мелодия",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (soundUriText.isBlank()) {
                        "Системная мелодия будильника"
                    } else {
                        soundLabelText.ifBlank { "Свой файл" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            soundUriText = ""
                            soundLabelText = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Системная")
                    }
                    OutlinedButton(
                        onClick = {
                            soundPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать файл")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                CompactSwitchRow(
                    title = "Активен",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ShiftAlarmConfig(
                            id = currentAlarm?.id ?: UUID.randomUUID().toString(),
                            title = titleText.trim(),
                            triggerHour = triggerHour.coerceIn(0, 23),
                            triggerMinute = triggerMinute.coerceIn(0, 59),
                            volumePercent = volumePercent.coerceIn(0, 100),
                            soundUri = soundUriText.ifBlank { null },
                            soundLabel = soundLabelText.trim(),
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun LeaveBenefitsSettingsScreen(
    sickYear1: Int,
    sickYear2: Int,
    vacationAccruals12MonthsText: String,
    onVacationAccrualsChange: (String) -> Unit,
    computedVacationAverageDaily: Double,
    sickIncomeYear1Text: String,
    onSickIncomeYear1Change: (String) -> Unit,
    sickIncomeYear2Text: String,
    onSickIncomeYear2Change: (String) -> Unit,
    sickLimitYear1Text: String,
    onSickLimitYear1Change: (String) -> Unit,
    sickLimitYear2Text: String,
    onSickLimitYear2Change: (String) -> Unit,
    autoSickCalculationPeriodDays: Int,
    sickExcludedDaysText: String,
    onSickExcludedDaysChange: (String) -> Unit,
    effectiveSickCalculationDays: Int,
    sickPayPercentText: String,
    onSickPayPercentChange: (String) -> Unit,
    sickMaxDailyAmountText: String,
    onSickMaxDailyAmountChange: (String) -> Unit,
    computedSickAverageDaily: Double,
    isLoadingLimits: Boolean,
    limitsMessage: String?,
    onFetchLimits: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            FixedScreenHeader(
                title = "Отпуск и больничный",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Отпуск",
                    subtitle = "Упрощённый расчёт по сумме начислений за 12 месяцев"
                ) {
                    CompactDecimalField(
                        label = "Сумма начислений за последние 12 месяцев",
                        value = vacationAccruals12MonthsText,
                        onValueChange = onVacationAccrualsChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Средний дневной заработок",
                        value = formatMoney(computedVacationAverageDaily)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onSave) {
                            Text("Сохранить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Больничный",
                    subtitle = "Доход за два предыдущих года, лимиты ФНС и расчётный период"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onFetchLimits, enabled = !isLoadingLimits) {
                            Text(if (isLoadingLimits) "Загрузка..." else "Подтянуть лимиты")
                        }
                    }

                    if (!limitsMessage.isNullOrBlank()) {
                        Text(
                            text = limitsMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    CompactDecimalField(
                        label = "Доход за ${sickYear1} год",
                        value = sickIncomeYear1Text,
                        onValueChange = onSickIncomeYear1Change,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = "Доход за ${sickYear2} год",
                        value = sickIncomeYear2Text,
                        onValueChange = onSickIncomeYear2Change,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Лимит ${sickYear1}",
                            value = sickLimitYear1Text,
                            onValueChange = onSickLimitYear1Change,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "Лимит ${sickYear2}",
                            value = sickLimitYear2Text,
                            onValueChange = onSickLimitYear2Change,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Дней в расчётном периоде",
                        value = autoSickCalculationPeriodDays.toString()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactIntField(
                        label = "Исключаемые дни",
                        value = sickExcludedDaysText,
                        onValueChange = onSickExcludedDaysChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentInfoRow(
                        label = "Дней к применению",
                        value = effectiveSickCalculationDays.toString()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "Коэф. больничного",
                            value = sickPayPercentText,
                            onValueChange = onSickPayPercentChange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "Макс. в день",
                            value = sickMaxDailyAmountText,
                            onValueChange = onSickMaxDailyAmountChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Средний дневной заработок",
                        value = formatMoney(computedSickAverageDaily)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ShiftTemplatesDialog(
    templates: List<ShiftTemplateEntity>,
    onDismiss: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (ShiftTemplateEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактор смен") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onAddNew) {
                        Text("Добавить смену")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (templates.isEmpty()) {
                    Text("Шаблонов смен пока нет.")
                } else {
                    templates.forEach { template ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onEdit(template) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${template.code} — ${template.title}",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Всего часов: ${formatDouble(template.totalHours)}")
                            Text("Обед: ${formatDouble(template.breakHours)}")
                            Text("Оплачиваемые: ${formatDouble(template.paidHours())}")
                            Text("Ночные: ${formatDouble(template.nightHours)}")
                            Text(
                                buildString {
                                    append(if (template.active) "Активна" else "Неактивна")
                                    append(" • ")
                                    append(if (template.isWeekendPaid) "Выходная/праздничная" else "Обычная")
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onEdit(template) }) {
                                    Text("Изменить")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {}
    )
}

data class OvertimePeriodInfo(
    val label: String,
    val startMonth: YearMonth,
    val endMonth: YearMonth
) {
    val startDate: LocalDate get() = startMonth.atDay(1)
    val endDate: LocalDate get() = endMonth.atEndOfMonth()
}
fun formatMonthYearTitle(month: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru-RU"))
    return month.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("ru-RU")) else it.toString()
    }
}

fun resolveOvertimePeriodInfo(
    currentMonth: YearMonth,
    overtimePeriodName: String
): OvertimePeriodInfo {
    return when (runCatching { OvertimePeriod.valueOf(overtimePeriodName) }.getOrElse { OvertimePeriod.YEAR }) {
        OvertimePeriod.MONTH -> OvertimePeriodInfo(
            label = formatMonthYearTitle(currentMonth),
            startMonth = currentMonth,
            endMonth = currentMonth
        )

        OvertimePeriod.QUARTER -> {
            val startMonthNumber = ((currentMonth.monthValue - 1) / 3) * 3 + 1
            val startMonth = YearMonth.of(currentMonth.year, startMonthNumber)
            val endMonth = startMonth.plusMonths(2)
            val quarterNumber = ((currentMonth.monthValue - 1) / 3) + 1
            OvertimePeriodInfo(
                label = "${quarterNumber} квартал ${currentMonth.year}",
                startMonth = startMonth,
                endMonth = endMonth
            )
        }

        OvertimePeriod.HALF_YEAR -> {
            val firstHalf = currentMonth.monthValue <= 6
            val startMonth = if (firstHalf) YearMonth.of(currentMonth.year, 1) else YearMonth.of(currentMonth.year, 7)
            val endMonth = if (firstHalf) YearMonth.of(currentMonth.year, 6) else YearMonth.of(currentMonth.year, 12)
            OvertimePeriodInfo(
                label = if (firstHalf) "1 полугодие ${currentMonth.year}" else "2 полугодие ${currentMonth.year}",
                startMonth = startMonth,
                endMonth = endMonth
            )
        }

        OvertimePeriod.YEAR -> OvertimePeriodInfo(
            label = "${currentMonth.year} год",
            startMonth = YearMonth.of(currentMonth.year, 1),
            endMonth = YearMonth.of(currentMonth.year, 12)
        )
    }
}

fun calculateNormHoursForMonth(
    month: YearMonth,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    return when (normMode) {
        NormMode.MANUAL -> payrollSettings.monthlyNormHours.coerceAtLeast(0.0)
        NormMode.PRODUCTION_CALENDAR -> calculateProductionCalendarMonthInfo(
            month = month,
            holidayMap = holidayMap,
            workdayHours = payrollSettings.workdayHours
        ).normHours
        NormMode.AVERAGE_ANNUAL -> when (annualNormSourceMode) {
            AnnualNormSourceMode.WORKDAY_HOURS -> calculateAverageAnnualNormHours(
                year = month.year,
                holidayMap = holidayMap,
                workdayHours = payrollSettings.workdayHours
            )
            AnnualNormSourceMode.YEAR_TOTAL_HOURS -> (payrollSettings.annualNormHours / 12.0).coerceAtLeast(0.0)
        }
        NormMode.AVERAGE_QUARTERLY -> calculateAverageQuarterNormHours(
            month = month,
            holidayMap = holidayMap,
            workdayHours = payrollSettings.workdayHours
        )
    }
}

fun calculateNormHoursForPeriod(
    periodInfo: OvertimePeriodInfo,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    var month = periodInfo.startMonth
    var total = 0.0
    while (!month.isAfter(periodInfo.endMonth)) {
        total += calculateNormHoursForMonth(
            month = month,
            payrollSettings = payrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = holidayMap
        )
        month = month.plusMonths(1)
    }
    return total
}

fun calculateExpectedWorkHoursForDate(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double,
    applyShortDayReduction: Boolean
): Double {
    if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return 0.0
    val holiday = holidayMap[date]
    if (holiday?.isNonWorking == true) return 0.0
    if (holiday?.kind == HolidayKinds.SHORT_DAY) {
        return if (applyShortDayReduction) {
            (workdayHours - 1.0).coerceAtLeast(0.0)
        } else {
            workdayHours.coerceAtLeast(0.0)
        }
    }
    return workdayHours.coerceAtLeast(0.0)
}

fun calculateAdjustedNormHoursForPeriod(
    basePeriodNormHours: Double,
    shifts: List<WorkShiftItem>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double,
    applyShortDayReduction: Boolean
): Double {
    val excludedNormHours = shifts
        .filter { (it.isVacation || it.isSickLeave) && it.date != null }
        .sumOf { shift ->
            calculateExpectedWorkHoursForDate(
                date = shift.date!!,
                holidayMap = holidayMap,
                workdayHours = workdayHours,
                applyShortDayReduction = applyShortDayReduction
            )
        }
    return (basePeriodNormHours - excludedNormHours).coerceAtLeast(0.0)
}

fun shiftTemplateSubtitle(template: ShiftTemplateEntity): String {
    fun fixed2(value: Double): String = String.format(Locale.US, "%.2f", value)

    return buildString {
        append("Оплач. ")
        append(fixed2(template.paidHours()))
        append(" ч")
        if (template.breakHours > 0.0) {
            append(" • Обед ")
            append(fixed2(template.breakHours))
            append(" ч")
        }
        if (template.nightHours > 0.0) {
            append(" • Ночь ")
            append(fixed2(template.nightHours))
            append(" ч")
        }
    }
}
data class MonthSummary(
    val workedDays: Int,
    val workedHours: Double,
    val nightHours: Double
)

fun calculateSummary(
    shiftCodesByDate: Map<LocalDate, String>,
    month: YearMonth,
    templateMap: Map<String, ShiftTemplateEntity>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    applyShortDayReduction: Boolean
): MonthSummary {
    val monthShiftItems = shiftCodesByDate
        .filterKeys { YearMonth.from(it) == month }
        .mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = holidayMap,
                applyShortDayReduction = applyShortDayReduction
            )
        }

    return MonthSummary(
        workedDays = monthShiftItems.count { it.paidHours > 0.0 },
        workedHours = monthShiftItems.sumOf { it.paidHours },
        nightHours = monthShiftItems.sumOf { it.nightHours }
    )
}

fun ShiftTemplateEntity.paidHours(): Double {
    return max(0.0, totalHours - breakHours)
}

fun ShiftTemplateEntity.toWorkShiftItem(
    specialRule: ShiftSpecialRule? = null
): WorkShiftItem {
    val paid = paidHours()
    val normalizedCode = code.trim().uppercase()
    val normalizedTitle = title.trim().uppercase()
    val isVacation = iconKey == "OT" || normalizedCode in setOf("ОТ", "ОТП", "ОТПУСК") || "ОТПУ" in normalizedTitle
    val isSickLeave = iconKey == "SICK" || normalizedCode in setOf("Б", "БЛ", "БОЛ", "БОЛЬН") || "БОЛЬН" in normalizedTitle
    val resolvedSpecialDayType = resolveSpecialDayType(specialRule, isWeekendPaid)
    val resolvedSpecialDayCompensation = resolveSpecialDayCompensation(specialRule, isWeekendPaid)
    val legacyWeekendPaid = legacyWeekendPaidFlag(resolvedSpecialDayType, resolvedSpecialDayCompensation)

    return WorkShiftItem(
        paidHours = if (isVacation || isSickLeave) 0.0 else paid,
        nightHours = if (isVacation || isSickLeave) 0.0 else nightHours.coerceAtMost(paid),
        isWeekendPaid = if (isVacation || isSickLeave) false else legacyWeekendPaid,
        specialDayType = if (isVacation || isSickLeave) SpecialDayType.NONE.name else resolvedSpecialDayType.name,
        specialDayCompensation = if (isVacation || isSickLeave) SpecialDayCompensation.NONE.name else resolvedSpecialDayCompensation.name,
        isVacation = isVacation,
        isSickLeave = isSickLeave
    )
}

fun ShiftTemplateEntity.toWorkShiftItemForDate(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>,
    applyShortDayReduction: Boolean,
    specialRule: ShiftSpecialRule? = null
): WorkShiftItem {
    val normalizedCode = code.trim().uppercase()
    val normalizedTitle = title.trim().uppercase()
    val isVacation = iconKey == "OT" || normalizedCode in setOf("ОТ", "ОТП", "ОТПУСК") || "ОТПУ" in normalizedTitle
    val isSickLeave = iconKey == "SICK" || normalizedCode in setOf("Б", "БЛ", "БОЛ", "БОЛЬН") || "БОЛЬН" in normalizedTitle
    val resolvedSpecialDayType = resolveSpecialDayType(specialRule, isWeekendPaid)
    val resolvedSpecialDayCompensation = resolveSpecialDayCompensation(specialRule, isWeekendPaid)
    val legacyWeekendPaid = legacyWeekendPaidFlag(resolvedSpecialDayType, resolvedSpecialDayCompensation)

    if (isVacation || isSickLeave) {
        return WorkShiftItem(
            paidHours = 0.0,
            nightHours = 0.0,
            isWeekendPaid = false,
            date = date,
            specialDayType = SpecialDayType.NONE.name,
            specialDayCompensation = SpecialDayCompensation.NONE.name,
            isVacation = isVacation,
            isSickLeave = isSickLeave
        )
    }

    val basePaid = paidHours()
    val isShortDay = holidayMap[date]?.kind == HolidayKinds.SHORT_DAY

    val reductionHours = if (
        applyShortDayReduction &&
        isShortDay &&
        resolvedSpecialDayType == SpecialDayType.NONE &&
        basePaid > 0.0
    ) {
        1.0
    } else {
        0.0
    }

    val adjustedPaidHours = (basePaid - reductionHours).coerceAtLeast(0.0)
    val adjustedNightHours = nightHours.coerceAtMost(adjustedPaidHours)

    return WorkShiftItem(
        paidHours = adjustedPaidHours,
        nightHours = adjustedNightHours,
        isWeekendPaid = legacyWeekendPaid,
        date = date,
        specialDayType = resolvedSpecialDayType.name,
        specialDayCompensation = resolvedSpecialDayCompensation.name,
        isVacation = false,
        isSickLeave = false
    )
}

data class ProductionCalendarMonthInfo(
    val normHours: Double,
    val workingDays: Int,
    val shortDays: Int
)

fun calculateProductionCalendarMonthInfo(
    month: YearMonth,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): ProductionCalendarMonthInfo {
    var normHours = 0.0
    var workingDays = 0
    var shortDays = 0

    var date = month.atDay(1)
    val endDate = month.atEndOfMonth()

    while (!date.isAfter(endDate)) {
        val holiday = holidayMap[date]
        val isWeekend = isWeekendDay(date)

        when {
            isWeekend -> Unit
            holiday?.kind == HolidayKinds.SHORT_DAY -> {
                workingDays += 1
                shortDays += 1
                normHours += (workdayHours - 1.0).coerceAtLeast(0.0)
            }
            holiday?.isNonWorking == true -> Unit
            else -> {
                workingDays += 1
                normHours += workdayHours
            }
        }

        date = date.plusDays(1)
    }

    return ProductionCalendarMonthInfo(
        normHours = normHours,
        workingDays = workingDays,
        shortDays = shortDays
    )
}

fun calculateAverageAnnualNormHours(
    year: Int,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): Double {
    val total = (1..12).sumOf { monthNumber ->
        calculateProductionCalendarMonthInfo(
            month = YearMonth.of(year, monthNumber),
            holidayMap = holidayMap,
            workdayHours = workdayHours
        ).normHours
    }

    return total / 12.0
}

fun calculateAverageQuarterNormHours(
    month: YearMonth,
    holidayMap: Map<LocalDate, HolidayEntity>,
    workdayHours: Double
): Double {
    val quarterStartMonth = ((month.monthValue - 1) / 3) * 3 + 1

    val total = (quarterStartMonth until quarterStartMonth + 3).sumOf { monthNumber ->
        calculateProductionCalendarMonthInfo(
            month = YearMonth.of(month.year, monthNumber),
            holidayMap = holidayMap,
            workdayHours = workdayHours
        ).normHours
    }

    return total / 3.0
}

fun calculateAnnualNormHoursForYear(
    year: Int,
    payrollSettings: PayrollSettings,
    normMode: NormMode,
    annualNormSourceMode: AnnualNormSourceMode,
    holidayMap: Map<LocalDate, HolidayEntity>
): Double {
    return when (normMode) {
        NormMode.MANUAL -> payrollSettings.monthlyNormHours.coerceAtLeast(0.0) * 12.0
        NormMode.PRODUCTION_CALENDAR,
        NormMode.AVERAGE_QUARTERLY -> {
            (1..12).sumOf { monthNumber ->
                calculateProductionCalendarMonthInfo(
                    month = YearMonth.of(year, monthNumber),
                    holidayMap = holidayMap,
                    workdayHours = payrollSettings.workdayHours
                ).normHours
            }
        }
        NormMode.AVERAGE_ANNUAL -> {
            when (annualNormSourceMode) {
                AnnualNormSourceMode.WORKDAY_HOURS ->
                    calculateAverageAnnualNormHours(
                        year = year,
                        holidayMap = holidayMap,
                        workdayHours = payrollSettings.workdayHours
                    ) * 12.0
                AnnualNormSourceMode.YEAR_TOTAL_HOURS -> payrollSettings.annualNormHours.coerceAtLeast(0.0)
            }
        }
    }
}

fun Double.toPlainString(): String {
    return if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        this.toString()
    }
}

fun displayHousingPaymentLabel(rawLabel: String): String {
    return rawLabel.trim().ifBlank { "Выплата на квартиру" }
}

fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

fun formatMoney(value: Double): String {
    return String.format(Locale.US, "%.2f ₽", value)
}

private val fallbackSickInsuranceBaseLimits = mapOf(
    2023 to 1_917_000.0,
    2024 to 2_225_000.0,
    2025 to 2_759_000.0,
    2026 to 2_979_000.0
)

fun formatWholeNumber(value: Double): String {
    return value.toLong().toString()
}

suspend fun fetchSickInsuranceBaseLimitsFromInternet(
    vararg years: Int
): Map<Int, Double> = withContext(Dispatchers.IO) {
    val requestedYears = years.distinct().filter { it >= 2023 }
    if (requestedYears.isEmpty()) return@withContext emptyMap()

    val result = mutableMapOf<Int, Double>()
    val urls = listOf(
        "https://www.nalog.gov.ru/rn77/taxation/insprem/",
        "https://www.nalog.gov.ru/rn77/ip/prem_employ/"
    )

    urls.forEach { url ->
        if (requestedYears.all { result.containsKey(it) }) return@forEach
        runCatching {
            downloadUrlText(url)
        }.getOrNull()?.let { html ->
            val normalized = html
                .replace("&nbsp;", " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")

            requestedYears.forEach { year ->
                if (!result.containsKey(year)) {
                    extractInsuranceBaseLimitFromText(normalized, year)?.let { limit ->
                        result[year] = limit
                    }
                }
            }
        }
    }

    requestedYears.forEach { year ->
        if (!result.containsKey(year)) {
            fallbackSickInsuranceBaseLimits[year]?.let { limit ->
                result[year] = limit
            }
        }
    }

    if (result.isEmpty()) {
        throw IllegalStateException("Не удалось определить лимиты")
    }

    result
}

private fun extractInsuranceBaseLimitFromText(
    text: String,
    year: Int
): Double? {
    val yearWindow = Regex("""\b$year\b.{0,260}""")
        .find(text)
        ?.value
        ?: return null

    return Regex("""\d[\d\s]{5,15}\d""")
        .findAll(yearWindow)
        .mapNotNull { match ->
            match.value.filter(Char::isDigit).toLongOrNull()
        }
        .firstOrNull { value ->
            value in 1_000_000L..9_999_999L
        }
        ?.toDouble()
}

private fun downloadUrlText(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 15_000
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Mobile Safari/537.36"
        )
    }

    return try {
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}


data class SickLimitsSyncResult(
    val updated: Boolean,
    val changedOnServer: Boolean,
    val limits: Map<Int, Double>,
    val message: String
)

private fun sickLimitsValueKey(year: Int): String = "sick_limit_${year}_value"
private fun sickLimitsFingerprintKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_fingerprint"
}
private fun sickLimitsLastCheckKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_last_check_at"
}
private fun sickLimitsSuccessKey(year1: Int, year2: Int): String {
    val (first, second) = listOf(year1, year2).sorted()
    return "sick_limits_${first}_${second}_success_at"
}

fun readCachedSickInsuranceBaseLimits(
    prefs: android.content.SharedPreferences,
    vararg years: Int
): Map<Int, Double> {
    return buildMap {
        years.distinct().forEach { year ->
            val value = prefs.getFloat(sickLimitsValueKey(year), Float.NaN)
            if (!value.isNaN()) {
                put(year, value.toDouble())
            }
        }
    }
}

suspend fun checkAndFetchSickInsuranceBaseLimitsIfChanged(
    prefs: android.content.SharedPreferences,
    year1: Int,
    year2: Int,
    forceNetworkCheck: Boolean
): SickLimitsSyncResult {
    val cachedLimits = readCachedSickInsuranceBaseLimits(prefs, year1, year2)
    val hasCachedLimits = cachedLimits.containsKey(year1) && cachedLimits.containsKey(year2)
    val now = System.currentTimeMillis()
    val lastCheckKey = sickLimitsLastCheckKey(year1, year2)
    val successKey = sickLimitsSuccessKey(year1, year2)
    val fingerprintKey = sickLimitsFingerprintKey(year1, year2)
    val lastCheckAt = prefs.getLong(lastCheckKey, 0L)
    val lastSuccessAt = prefs.getLong(successKey, 0L)
    val savedFingerprint = prefs.getString(fingerprintKey, null)

    if (hasCachedLimits && !forceNetworkCheck && lastCheckAt > 0L && now - lastCheckAt < SICK_LIMITS_AUTO_CHECK_INTERVAL_MS) {
        return SickLimitsSyncResult(
            updated = false,
            changedOnServer = false,
            limits = cachedLimits,
            message = if (lastSuccessAt > 0L) {
                "Используются локальные лимиты. Последняя проверка: ${formatCalendarSyncMoment(lastCheckAt)}"
            } else {
                "Используются локально сохранённые лимиты"
            }
        )
    }

    val latestLimits = fetchSickInsuranceBaseLimitsFromInternet(year1, year2)
    val latestFingerprint = sha256(
        latestLimits.toSortedMap().entries.joinToString(separator = "|") { (year, value) ->
            "$year=${formatWholeNumber(value)}"
        }
    )

    if (hasCachedLimits && savedFingerprint != null && latestFingerprint == savedFingerprint) {
        prefs.edit()
            .putLong(lastCheckKey, now)
            .apply()

        return SickLimitsSyncResult(
            updated = false,
            changedOnServer = false,
            limits = cachedLimits,
            message = "Изменений по лимитам не найдено. Проверено: ${formatCalendarSyncMoment(now)}"
        )
    }

    prefs.edit().apply {
        latestLimits.forEach { (year, value) ->
            putFloat(sickLimitsValueKey(year), value.toFloat())
        }
        putLong(lastCheckKey, now)
        putLong(successKey, now)
        putString(fingerprintKey, latestFingerprint)
    }.apply()

    return SickLimitsSyncResult(
        updated = true,
        changedOnServer = hasCachedLimits,
        limits = latestLimits,
        message = when {
            !hasCachedLimits -> "Лимиты ФНС загружены и сохранены локально"
            savedFingerprint == null -> "Лимиты ФНС проверены и обновлены"
            else -> "Найдены изменения по лимитам. Локальные данные обновлены"
        }
    )
}


@Composable
fun HolidayInfoCard(
    holiday: HolidayEntity
) {
    val accentColor = when (holiday.kind) {
        HolidayKinds.SHORT_DAY -> Color(0xFFEF6C00)
        HolidayKinds.TRANSFERRED_DAY_OFF -> Color(0xFFD32F2F)
        else -> Color(0xFFD32F2F)
    }

    val typeLabel = when (holiday.kind) {
        HolidayKinds.SHORT_DAY -> "Сокращённый день"
        HolidayKinds.TRANSFERRED_DAY_OFF -> "Перенесённый выходной"
        else -> "Праздничный день"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(accentColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = holiday.title.ifBlank { typeLabel },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
fun isDateInRange(
    date: LocalDate,
    start: LocalDate?,
    end: LocalDate?
): Boolean {
    if (start == null || end == null) return false
    return !date.isBefore(start) && !date.isAfter(end)
}
fun trimTrailingBlankSteps(steps: List<String>, minSize: Int = 35): List<String> {
    val lastUsedIndex = steps.indexOfLast { it.isNotBlank() }
    val trimmed = if (lastUsedIndex == -1) {
        emptyList()
    } else {
        steps.take(lastUsedIndex + 1)
    }

    return if (trimmed.size >= minSize) {
        trimmed
    } else {
        trimmed + List(minSize - trimmed.size) { "" }
    }
}

fun shiftStepsLeft(steps: List<String>): List<String> {
    if (steps.isEmpty()) return steps
    return steps.drop(1) + steps.first()
}

fun shiftStepsRight(steps: List<String>): List<String> {
    if (steps.isEmpty()) return steps
    return listOf(steps.last()) + steps.dropLast(1)
}