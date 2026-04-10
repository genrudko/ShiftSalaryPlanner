package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.derivedStateOf
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
import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

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

    val excelImportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("РќРµ СѓРґР°Р»РѕСЃСЊ РїСЂРѕС‡РёС‚Р°С‚СЊ Excel-С„Р°Р№Р»")
            pendingExcelFileBytes = bytes
            pendingExcelFileName = uri.lastPathSegment ?: "tabel.xlsm"
            excelImportPreview = null
            excelImportCandidates = emptyList()
            excelImportStatusMessage = "Р¤Р°Р№Р» РІС‹Р±СЂР°РЅ: ${pendingExcelFileName}"
        }.onFailure { error ->
            pendingExcelFileBytes = null
            pendingExcelFileName = null
            excelImportPreview = null
            excelImportCandidates = emptyList()
            excelImportStatusMessage = "РќРµ СѓРґР°Р»РѕСЃСЊ РѕС‚РєСЂС‹С‚СЊ С„Р°Р№Р»: ${error.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
                "РСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ Р»РѕРєР°Р»СЊРЅС‹Р№ РєР°Р»РµРЅРґР°СЂСЊ ${currentMonth.year}. РџСЂРѕРІРµСЂРєР° РЅРµ СѓРґР°Р»Р°СЃСЊ: ${e.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
            } else {
                "РђРІС‚РѕР·Р°РіСЂСѓР·РєР° РЅРµ СѓРґР°Р»Р°СЃСЊ: ${e.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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

    val shiftSpecialRulesSnapshot by remember {
        derivedStateOf { shiftSpecialRules.toMap() }
    }

    val manualHolidayRecordsSnapshot by remember {
        derivedStateOf { manualHolidayRecords.toList() }
    }

    val editingManualHoliday = remember(editingManualHolidayDate, manualHolidayRecordsSnapshot) {
        manualHolidayRecordsSnapshot.firstOrNull { it.date == editingManualHolidayDate }
    }

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
    val manualHolidayMap = remember(manualHolidayRecordsSnapshot) {
        manualHolidayRecordsSnapshot.associate { record ->
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
    val shiftTemplateTimingByCode = remember(shiftAlarmSettings.templateConfigs) {
        shiftAlarmSettings.templateConfigs.associateBy { it.shiftCode }
    }

    val summary = remember(
        shiftCodesByDate,
        currentMonth,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftTemplateTimingByCode
    ) {
        calculateSummary(
            shiftCodesByDate = shiftCodesByDate,
            month = currentMonth,
            templateMap = templateMap,
            holidayMap = resolvedHolidayMap,
            applyShortDayReduction = payrollSettings.applyShortDayReduction,
            shiftTimingsByCode = shiftTemplateTimingByCode
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
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        monthEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = payrollSettings.applyShortDayReduction,
                specialRule = shiftSpecialRulesSnapshot[code],
                shiftTiming = shiftTemplateTimingByCode[code]
            )
        }
    }

    val firstHalfShifts = remember(
        monthEntries,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        monthEntries
            .filterKeys { it.dayOfMonth <= 15 }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = payrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code]
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
        payrollSettings.applyShortDayReduction,
        shiftTemplateTimingByCode
    ) {
        val periodShifts = shiftCodesByDate
            .filterKeys { !it.isBefore(overtimePeriodInfo.startDate) && !it.isAfter(overtimePeriodInfo.endDate) }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = payrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code]
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
                backupRestoreStatusMessage = "Р РµР·РµСЂРІРЅР°СЏ РєРѕРїРёСЏ СЃРѕС…СЂР°РЅРµРЅР°"
            }.onFailure { error ->
                backupRestoreStatusMessage = "РќРµ СѓРґР°Р»РѕСЃСЊ СЃРѕС…СЂР°РЅРёС‚СЊ РєРѕРїРёСЋ: ${error.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
                    "РќРµ СѓРґР°Р»РѕСЃСЊ РІРѕСЃСЃС‚Р°РЅРѕРІРёС‚СЊ РєРѕРїРёСЋ: ${error.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
                            animationSpec = tween(260)
                        ) + fadeIn(animationSpec = tween(240)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(220)
                        ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(240)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                            targetScale = 0.99f,
                            animationSpec = tween(180)
                        )
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(260)
                        ) + fadeIn(animationSpec = tween(240)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(220)
                        ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(240)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                            targetScale = 0.99f,
                            animationSpec = tween(180)
                        )
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
                            state = PayrollTabState(
                                currentMonth = currentMonth,
                                summary = summary,
                                payroll = payroll,
                                payrollDetailedResult = payrollDetailedResult,
                                annualOvertime = annualOvertime,
                                paymentDates = paymentDates,
                                housingPaymentLabel = payrollSettings.housingPaymentLabel,
                                detailedShiftStats = detailedShiftStats,
                                isSummaryExpanded = isSummaryExpanded
                            ),
                            actions = PayrollTabActions(
                                onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                                onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                                onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                                onToggleSummary = { isSummaryExpanded = !isSummaryExpanded },
                                onOpenSettings = { showPayrollSettings = true }
                            ),
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
                            state = ShiftAlarmsTabState(
                                settings = shiftAlarmSettings,
                                shiftTemplates = alarmEligibleTemplates,
                                lastRescheduleResult = shiftAlarmRescheduleResult,
                                canScheduleExactAlarms = ShiftAlarmScheduler.canScheduleExactShiftAlarms(context),
                                notificationPermissionGranted = ShiftAlarmScheduler.hasNotificationPermission(context)
                            ),
                            actions = ShiftAlarmsTabActions(
                                onSave = { newSettings ->
                                    scope.launch {
                                        shiftAlarmStore.save(newSettings)
                                        shiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
                                            ShiftAlarmScheduler.reschedule(
                                                context = context,
                                                settings = newSettings,
                                                savedDays = savedDays,
                                                templateMap = templateMap,
                                                mirrorToSystemClockApp = true,
                                                allowSystemClockUiFallback = false
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
                                onOpenSystemClock = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }.onFailure {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_DATE_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    }
                                },
                                onRescheduleNow = {
                                    scope.launch {
                                        shiftAlarmRescheduleResult = withContext(Dispatchers.IO) {
                                            ShiftAlarmScheduler.reschedule(
                                                context = context,
                                                settings = shiftAlarmSettings,
                                                savedDays = savedDays,
                                                templateMap = templateMap,
                                                mirrorToSystemClockApp = true,
                                                allowSystemClockUiFallback = false
                                            )
                                        }
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    BottomTab.SHIFTS -> {
                        TemplatesScreen(
                            state = TemplatesScreenState(
                                mode = templateMode,
                                templates = shiftTemplates.sortedBy { it.sortOrder },
                                specialRules = shiftSpecialRulesSnapshot,
                                patterns = patternTemplates
                            ),
                            actions = TemplatesScreenActions(
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
                                    holidaySyncMessage = "РџСЂРѕРІРµСЂРєР° РєР°Р»РµРЅРґР°СЂСЏ ${currentMonth.year}..."
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
                                        holidaySyncMessage = "РћС€РёР±РєР° РѕР±РЅРѕРІР»РµРЅРёСЏ: ${e.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
                    excelImportStatusMessage = "РЎРЅР°С‡Р°Р»Р° РІС‹Р±РµСЂРё Excel-С„Р°Р№Р»"
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
                                    excelImportStatusMessage = "РќР°Р№РґРµРЅРѕ РЅРµСЃРєРѕР»СЊРєРѕ СЃРѕС‚СЂСѓРґРЅРёРєРѕРІ СЃ СЌС‚РѕР№ С„Р°РјРёР»РёРµР№. Р’С‹Р±РµСЂРё РЅСѓР¶РЅРѕРіРѕ."
                                }
                                is ExcelImportParseResult.Preview -> {
                                    excelImportCandidates = emptyList()
                                    excelImportPreview = result.preview
                                    excelImportStatusMessage = buildString {
                                        append("Р“РѕС‚РѕРІРѕ Рє РёРјРїРѕСЂС‚Сѓ: ")
                                        append(result.preview.importedDays.size)
                                        append(" РґРЅРµР№ вЂў РјРµСЃСЏС†РµРІ: ")
                                        append(result.preview.selectedMonths.joinToString())
                                        if (result.preview.templatesToCreate.isNotEmpty()) {
                                            append(" вЂў РЅРѕРІС‹С… С€Р°Р±Р»РѕРЅРѕРІ: ")
                                            append(result.preview.templatesToCreate.size)
                                        }
                                    }
                                }
                            }
                        }.onFailure { error ->
                            excelImportPreview = null
                            excelImportCandidates = emptyList()
                            excelImportStatusMessage = "РћС€РёР±РєР° Р°РЅР°Р»РёР·Р°: ${error.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
                        excelImportStatusMessage = "РРјРїРѕСЂС‚ Р·Р°РІРµСЂС€С‘РЅ: ${preview.importedDays.size} РґРЅРµР№"
                        excelImportPreview = null
                        excelImportCandidates = emptyList()
                    }.onFailure { error ->
                        excelImportStatusMessage = "РћС€РёР±РєР° РёРјРїРѕСЂС‚Р°: ${error.message ?: "РЅРµРёР·РІРµСЃС‚РЅРѕ"}"
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
        Text(text = "вЂє", style = MaterialTheme.typography.titleLarge)
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
                        AlarmCountChip(text = if (config.enabled) "Р’РєР»" else "Р’С‹РєР»")
                        AlarmCountChip(text = "Р’СЃРµРіРѕ ${config.alarms.size}")
                        AlarmCountChip(text = "РђРєС‚РёРІРЅС‹С… $activeAlarmCount")
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
                        text = if (expanded) "в–ѕ" else "в–ё",
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
                                "Р‘СѓРґРёР»СЊРЅРёРєРѕРІ РїРѕРєР° РЅРµС‚"
                            } else {
                                "Р‘СѓРґРёР»СЊРЅРёРєРѕРІ: ${config.alarms.size}"
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
                        Text("+ Р‘СѓРґРёР»СЊРЅРёРє")
                    }
                }

                if (config.alarms.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Р”РѕР±Р°РІСЊ РѕРґРёРЅ РёР»Рё РЅРµСЃРєРѕР»СЊРєРѕ Р±СѓРґРёР»СЊРЅРёРєРѕРІ РґР»СЏ СЌС‚РѕР№ СЃРјРµРЅС‹.",
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
                    text = alarm.title.ifBlank { "Р‘РµР· РЅР°Р·РІР°РЅРёСЏ" },
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("РР·Рј.")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("РЈРґР°Р».")
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
                ?.ifBlank { "РЎРІРѕР№ С„Р°Р№Р»" }
                ?: "РЎРІРѕР№ С„Р°Р№Р»"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (currentAlarm == null) "РќРѕРІС‹Р№ Р±СѓРґРёР»СЊРЅРёРє" else "Р РµРґР°РєС‚РёСЂРѕРІР°С‚СЊ Р±СѓРґРёР»СЊРЅРёРє"
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
                    label = { Text("РќР°Р·РІР°РЅРёРµ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Р’СЂРµРјСЏ СЃСЂР°Р±Р°С‚С‹РІР°РЅРёСЏ",
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
                    text = "Р’С‹Р±СЂР°РЅРѕ: ${formatClockHm(triggerHour, triggerMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Р“СЂРѕРјРєРѕСЃС‚СЊ: ${volumePercent.coerceIn(0, 100)}% РѕС‚ СЃРёСЃС‚РµРјРЅРѕР№ РіСЂРѕРјРєРѕСЃС‚Рё Р±СѓРґРёР»СЊРЅРёРєРѕРІ",
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
                    text = "РњРµР»РѕРґРёСЏ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (soundUriText.isBlank()) {
                        "РЎРёСЃС‚РµРјРЅР°СЏ РјРµР»РѕРґРёСЏ Р±СѓРґРёР»СЊРЅРёРєР°"
                    } else {
                        soundLabelText.ifBlank { "РЎРІРѕР№ С„Р°Р№Р»" }
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
                        Text("РЎРёСЃС‚РµРјРЅР°СЏ")
                    }
                    OutlinedButton(
                        onClick = {
                            soundPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Р’С‹Р±СЂР°С‚СЊ С„Р°Р№Р»")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                CompactSwitchRow(
                    title = "РђРєС‚РёРІРµРЅ",
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
                Text("РЎРѕС…СЂР°РЅРёС‚СЊ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("РћС‚РјРµРЅР°")
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
                title = "РћС‚РїСѓСЃРє Рё Р±РѕР»СЊРЅРёС‡РЅС‹Р№",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "РћС‚РїСѓСЃРє",
                    subtitle = "РЈРїСЂРѕС‰С‘РЅРЅС‹Р№ СЂР°СЃС‡С‘С‚ РїРѕ СЃСѓРјРјРµ РЅР°С‡РёСЃР»РµРЅРёР№ Р·Р° 12 РјРµСЃСЏС†РµРІ"
                ) {
                    CompactDecimalField(
                        label = "РЎСѓРјРјР° РЅР°С‡РёСЃР»РµРЅРёР№ Р·Р° РїРѕСЃР»РµРґРЅРёРµ 12 РјРµСЃСЏС†РµРІ",
                        value = vacationAccruals12MonthsText,
                        onValueChange = onVacationAccrualsChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "РЎСЂРµРґРЅРёР№ РґРЅРµРІРЅРѕР№ Р·Р°СЂР°Р±РѕС‚РѕРє",
                        value = formatMoney(computedVacationAverageDaily)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onSave) {
                            Text("РЎРѕС…СЂР°РЅРёС‚СЊ")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Р‘РѕР»СЊРЅРёС‡РЅС‹Р№",
                    subtitle = "Р”РѕС…РѕРґ Р·Р° РґРІР° РїСЂРµРґС‹РґСѓС‰РёС… РіРѕРґР°, Р»РёРјРёС‚С‹ Р¤РќРЎ Рё СЂР°СЃС‡С‘С‚РЅС‹Р№ РїРµСЂРёРѕРґ"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onFetchLimits, enabled = !isLoadingLimits) {
                            Text(if (isLoadingLimits) "Р—Р°РіСЂСѓР·РєР°..." else "РџРѕРґС‚СЏРЅСѓС‚СЊ Р»РёРјРёС‚С‹")
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
                        label = "Р”РѕС…РѕРґ Р·Р° ${sickYear1} РіРѕРґ",
                        value = sickIncomeYear1Text,
                        onValueChange = onSickIncomeYear1Change,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CompactDecimalField(
                        label = "Р”РѕС…РѕРґ Р·Р° ${sickYear2} РіРѕРґ",
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
                            label = "Р›РёРјРёС‚ ${sickYear1}",
                            value = sickLimitYear1Text,
                            onValueChange = onSickLimitYear1Change,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "Р›РёРјРёС‚ ${sickYear2}",
                            value = sickLimitYear2Text,
                            onValueChange = onSickLimitYear2Change,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "Р”РЅРµР№ РІ СЂР°СЃС‡С‘С‚РЅРѕРј РїРµСЂРёРѕРґРµ",
                        value = autoSickCalculationPeriodDays.toString()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CompactIntField(
                        label = "РСЃРєР»СЋС‡Р°РµРјС‹Рµ РґРЅРё",
                        value = sickExcludedDaysText,
                        onValueChange = onSickExcludedDaysChange,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentInfoRow(
                        label = "Р”РЅРµР№ Рє РїСЂРёРјРµРЅРµРЅРёСЋ",
                        value = effectiveSickCalculationDays.toString()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactDecimalField(
                            label = "РљРѕСЌС„. Р±РѕР»СЊРЅРёС‡РЅРѕРіРѕ",
                            value = sickPayPercentText,
                            onValueChange = onSickPayPercentChange,
                            modifier = Modifier.weight(1f)
                        )
                        CompactDecimalField(
                            label = "РњР°РєСЃ. РІ РґРµРЅСЊ",
                            value = sickMaxDailyAmountText,
                            onValueChange = onSickMaxDailyAmountChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PaymentInfoRow(
                        label = "РЎСЂРµРґРЅРёР№ РґРЅРµРІРЅРѕР№ Р·Р°СЂР°Р±РѕС‚РѕРє",
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
        title = { Text("Р РµРґР°РєС‚РѕСЂ СЃРјРµРЅ") },
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
                        Text("Р”РѕР±Р°РІРёС‚СЊ СЃРјРµРЅСѓ")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (templates.isEmpty()) {
                    Text("РЁР°Р±Р»РѕРЅРѕРІ СЃРјРµРЅ РїРѕРєР° РЅРµС‚.")
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
                                text = "${template.code} вЂ” ${template.title}",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Р’СЃРµРіРѕ С‡Р°СЃРѕРІ: ${formatDouble(template.totalHours)}")
                            Text("РћР±РµРґ: ${formatDouble(template.breakHours)}")
                            Text("РћРїР»Р°С‡РёРІР°РµРјС‹Рµ: ${formatDouble(template.paidHours())}")
                            Text("РќРѕС‡РЅС‹Рµ: ${formatDouble(template.nightHours)}")
                            Text(
                                buildString {
                                    append(if (template.active) "РђРєС‚РёРІРЅР°" else "РќРµР°РєС‚РёРІРЅР°")
                                    append(" вЂў ")
                                    append(if (template.isWeekendPaid) "Р’С‹С…РѕРґРЅР°СЏ/РїСЂР°Р·РґРЅРёС‡РЅР°СЏ" else "РћР±С‹С‡РЅР°СЏ")
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onEdit(template) }) {
                                    Text("РР·РјРµРЅРёС‚СЊ")
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
                Text("Р—Р°РєСЂС‹С‚СЊ")
            }
        },
        dismissButton = {}
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
        HolidayKinds.SHORT_DAY -> "РЎРѕРєСЂР°С‰С‘РЅРЅС‹Р№ РґРµРЅСЊ"
        HolidayKinds.TRANSFERRED_DAY_OFF -> "РџРµСЂРµРЅРµСЃС‘РЅРЅС‹Р№ РІС‹С…РѕРґРЅРѕР№"
        else -> "РџСЂР°Р·РґРЅРёС‡РЅС‹Р№ РґРµРЅСЊ"
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
