@file:Suppress("DEPRECATION")

package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import com.vigilante.shiftsalaryplanner.payroll.PayrollDeduction
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.widget.PREFS_WIDGET_SETTINGS
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProvider
import com.vigilante.shiftsalaryplanner.widget.WidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.WidgetThemeMode
import com.vigilante.shiftsalaryplanner.widget.clearWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetLongLabel
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetMetaLabel
import com.vigilante.shiftsalaryplanner.widget.defaultWidgetShortLabel
import com.vigilante.shiftsalaryplanner.widget.readWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.readWidgetThemeMode
import com.vigilante.shiftsalaryplanner.widget.writeWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetThemeMode
import com.vigilante.shiftsalaryplanner.excel.EmptyDayImportMode
import com.vigilante.shiftsalaryplanner.excel.ExcelImportParseResult
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelImportRequest
import com.vigilante.shiftsalaryplanner.excel.ExcelImportScopeType
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleParser
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBars
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.DefaultShiftTemplates
import com.vigilante.shiftsalaryplanner.data.FederalHolidaySeed
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.HolidayKinds
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import com.vigilante.shiftsalaryplanner.data.ShiftDayDao
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPayment
import com.vigilante.shiftsalaryplanner.payroll.AdditionalPaymentType
import com.vigilante.shiftsalaryplanner.payroll.PaymentDistribution
import com.vigilante.shiftsalaryplanner.payroll.PremiumPeriod
import com.vigilante.shiftsalaryplanner.payroll.AdvanceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.AnnualOvertimeResult
import com.vigilante.shiftsalaryplanner.payroll.ExtraSalaryMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.OvertimePeriod
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PaymentDates
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.PayrollResult
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayCompensation
import com.vigilante.shiftsalaryplanner.payroll.SpecialDayType
import com.vigilante.shiftsalaryplanner.payroll.WorkShiftItem
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculatePaymentDates
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

private const val PREFS_SHIFT_COLORS = "shift_colors"
private const val PREFS_SHIFT_SPECIAL_RULES = "shift_special_rules"
private const val PREFS_ONE_TIME_MIGRATIONS = "one_time_migrations"
private const val KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1 = "legacy_defaults_cleanup_v1"
private const val LEGACY_EMBEDDED_BASE_SALARY = 102050.0
private const val LEGACY_EMBEDDED_EXTRA_SALARY = 49733.0

private fun neutralInitialPayrollSettings(): PayrollSettings = PayrollSettings(
    baseSalary = 0.0,
    extraSalary = 0.0
)
fun Double.nearlyEquals(other: Double, epsilon: Double = 0.0001): Boolean =
    abs(this - other) <= epsilon

private fun readPayrollSettingsFromPrefs(prefs: SharedPreferences): PayrollSettings {
    return PayrollSettings(
        baseSalary = prefs.getFloat("base_salary", 0f).toDouble(),
        extraSalary = prefs.getFloat("extra_salary", 0f).toDouble(),
        housingPayment = prefs.getFloat("housing_payment", 0f).toDouble(),
        housingPaymentLabel = prefs.getString("housing_payment_label", "Выплата на квартиру") ?: "Выплата на квартиру",
        housingPaymentTaxable = prefs.getBoolean("housing_payment_taxable", true),
        housingPaymentWithAdvance = prefs.getBoolean("housing_payment_with_advance", false),
        monthlyNormHours = prefs.getFloat("monthly_norm_hours", 165f).toDouble(),
        workdayHours = prefs.getFloat("workday_hours", 8f).toDouble(),
        annualNormSourceMode = prefs.getString("annual_norm_source_mode", "WORKDAY_HOURS") ?: "WORKDAY_HOURS",
        annualNormHours = prefs.getFloat("annual_norm_hours", 1970f).toDouble(),
        normMode = prefs.getString("norm_mode", "MANUAL") ?: "MANUAL",
        payMode = prefs.getString("pay_mode", "HOURLY") ?: "HOURLY",
        extraSalaryMode = prefs.getString("extra_salary_mode", "INCLUDED_IN_RATE") ?: "INCLUDED_IN_RATE",
        advanceMode = prefs.getString("advance_mode", "ACTUAL_EARNINGS") ?: "ACTUAL_EARNINGS",
        advancePercent = prefs.getFloat("advance_percent", 50f).toDouble(),
        applyShortDayReduction = prefs.getBoolean("apply_short_day_reduction", true),
        nightPercent = prefs.getFloat("night_percent", 0.4f).toDouble(),
        holidayRateMultiplier = prefs.getFloat("holiday_rate_multiplier", 2f).toDouble(),
        ndflPercent = prefs.getFloat("ndfl_percent", 0.13f).toDouble(),
        vacationAverageDaily = prefs.getFloat("vacation_average_daily", 0f).toDouble(),
        vacationAccruals12Months = prefs.getFloat("vacation_accruals_12_months", 0f).toDouble(),
        sickAverageDaily = prefs.getFloat("sick_average_daily", 0f).toDouble(),
        sickIncomeYear1 = prefs.getFloat("sick_income_year1", 0f).toDouble(),
        sickIncomeYear2 = prefs.getFloat("sick_income_year2", 0f).toDouble(),
        sickLimitYear1 = prefs.getFloat("sick_limit_year1", 0f).toDouble(),
        sickLimitYear2 = prefs.getFloat("sick_limit_year2", 0f).toDouble(),
        sickCalculationPeriodDays = prefs.getInt("sick_calculation_period_days", 730),
        sickExcludedDays = prefs.getInt("sick_excluded_days", 0),
        sickPayPercent = prefs.getFloat("sick_pay_percent", 1f).toDouble(),
        sickMaxDailyAmount = prefs.getFloat("sick_max_daily_amount", 6827.40f).toDouble(),
        progressiveNdflEnabled = prefs.getBoolean("progressive_ndfl_enabled", false),
        taxableIncomeYtdBeforeCurrentMonth = prefs.getFloat("taxable_income_ytd_before_current_month", 0f).toDouble(),
        advanceDay = prefs.getInt("advance_day", 20),
        salaryDay = prefs.getInt("salary_day", 5),
        movePaymentsToPreviousWorkday = prefs.getBoolean("move_payments_to_previous_workday", true),
        overtimeEnabled = prefs.getBoolean("overtime_enabled", true),
        overtimePeriod = prefs.getString("overtime_period", "YEAR") ?: "YEAR",
        excludeWeekendHolidayFromOvertime = prefs.getBoolean("exclude_weekend_holiday_from_overtime", true),
        excludeRvdDoublePayFromOvertime = prefs.getBoolean("exclude_rvd_double_pay_from_overtime", true),
        excludeRvdSingleWithDayOffFromOvertime = prefs.getBoolean("exclude_rvd_single_with_day_off_from_overtime", false)
    )
}
private fun PayrollSettings.matchesLikelyLegacyEmbeddedPayrollDefaults(): Boolean {
    return baseSalary.nearlyEquals(LEGACY_EMBEDDED_BASE_SALARY) &&
            extraSalary.nearlyEquals(LEGACY_EMBEDDED_EXTRA_SALARY) &&
            housingPayment.nearlyEquals(0.0) &&
            housingPaymentLabel == "Выплата на квартиру" &&
            housingPaymentTaxable &&
            !housingPaymentWithAdvance &&
            monthlyNormHours.nearlyEquals(165.0) &&
            workdayHours.nearlyEquals(8.0) &&
            annualNormSourceMode == AnnualNormSourceMode.WORKDAY_HOURS.name &&
            annualNormHours.nearlyEquals(1970.0) &&
            normMode == NormMode.MANUAL.name &&
            payMode == PayMode.HOURLY.name &&
            extraSalaryMode == ExtraSalaryMode.INCLUDED_IN_RATE.name &&
            advanceMode == AdvanceMode.ACTUAL_EARNINGS.name &&
            advancePercent.nearlyEquals(50.0) &&
            applyShortDayReduction &&
            nightPercent.nearlyEquals(0.4) &&
            holidayRateMultiplier.nearlyEquals(2.0) &&
            ndflPercent.nearlyEquals(0.13) &&
            vacationAverageDaily.nearlyEquals(0.0) &&
            vacationAccruals12Months.nearlyEquals(0.0) &&
            sickAverageDaily.nearlyEquals(0.0) &&
            sickIncomeYear1.nearlyEquals(0.0) &&
            sickIncomeYear2.nearlyEquals(0.0) &&
            sickLimitYear1.nearlyEquals(0.0) &&
            sickLimitYear2.nearlyEquals(0.0) &&
            sickCalculationPeriodDays == 730 &&
            sickExcludedDays == 0 &&
            sickPayPercent.nearlyEquals(1.0) &&
            sickMaxDailyAmount.nearlyEquals(6827.40) &&
            !progressiveNdflEnabled &&
            taxableIncomeYtdBeforeCurrentMonth.nearlyEquals(0.0) &&
            advanceDay == 20 &&
            salaryDay == 5 &&
            movePaymentsToPreviousWorkday &&
            overtimeEnabled &&
            overtimePeriod == OvertimePeriod.YEAR.name &&
            excludeWeekendHolidayFromOvertime &&
            excludeRvdDoublePayFromOvertime &&
            !excludeRvdSingleWithDayOffFromOvertime
}

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
    var pendingImportConfirmationText by rememberSaveable { mutableStateOf<String?>(null) }
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
    fun saveManualHoliday(record: ManualHolidayRecord) {
        val existingIndex = manualHolidayRecords.indexOfFirst { it.date == record.date }
        if (existingIndex >= 0) {
            manualHolidayRecords[existingIndex] = record
        } else {
            manualHolidayRecords.add(record)
        }
        val sorted = manualHolidayRecords.sortedBy { it.date }
        manualHolidayRecords.clear()
        manualHolidayRecords.addAll(sorted)
        writeManualHolidayRecords(manualHolidayPrefs, manualHolidayRecords.toList())
    }

    fun deleteManualHoliday(date: String) {
        manualHolidayRecords.removeAll { it.date == date }
        writeManualHolidayRecords(manualHolidayPrefs, manualHolidayRecords.toList())
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

    val productionCalendarInfo = remember(currentMonth, resolvedHolidayMap, payrollSettings.workdayHours) {
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

    val shiftColors = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        val defaults = defaultShiftColors()
        defaults.forEach { (key, value) ->
            shiftColors[key] = shiftColorsPrefs.getInt(key, value)
        }
    }

    fun saveShiftColor(key: String, colorValue: Int) {
        shiftColors[key] = colorValue
        shiftColorsPrefs.edit().putInt(key, colorValue).apply()
        ShiftMonthWidgetProvider.requestUpdate(context)
    }

    fun resetShiftColors() {
        val defaults = defaultShiftColors()
        defaults.forEach { (key, value) ->
            saveShiftColor(key, value)
        }
    }

    fun saveShiftSpecialRule(code: String, rule: ShiftSpecialRule) {
        shiftSpecialRules[code] = rule
        writeShiftSpecialRule(shiftSpecialPrefs, code, rule)
    }

    fun removeShiftSpecialRule(code: String) {
        shiftSpecialRules.remove(code)
        deleteShiftSpecialRule(shiftSpecialPrefs, code)
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
            removeShiftSpecialRule(template.code)
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
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: throw IllegalStateException("Не удалось прочитать файл")

                val backupData = parseAppBackupJson(raw)

                backupData.sharedPrefs.forEach { (name, snapshot) ->
                    val targetPrefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    applySharedPreferencesSnapshot(targetPrefs, snapshot)
                }

                val importedShiftCodes = backupData.shiftTemplates.map { it.code }.toSet()
                backupData.shiftTemplates.forEach { template ->
                    shiftTemplateDao.upsert(template)
                }
                shiftTemplates
                    .filter { it.code !in importedShiftCodes }
                    .forEach { template ->
                        shiftTemplateDao.delete(template)
                    }

                val importedDates = backupData.shiftDays.map { it.date }.toSet()
                backupData.shiftDays.forEach { day ->
                    shiftDayDao.upsert(day)
                }
                savedDays
                    .filter { it.date !in importedDates }
                    .forEach { day ->
                        shiftDayDao.deleteByDate(day.date)
                    }

                manualHolidayRecords.clear()
                manualHolidayRecords.addAll(readManualHolidayRecords(manualHolidayPrefs))

                shiftColors.clear()
                val defaults = defaultShiftColors()
                defaults.forEach { (key, value) ->
                    shiftColors[key] = shiftColorsPrefs.getInt(key, value)
                }

                backupRestoreStatusMessage = buildString {
                    append("Восстановление завершено. Смен: ")
                    append(backupData.shiftDays.size)
                    append(" • шаблонов: ")
                    append(backupData.shiftTemplates.size)
                    append(". Экран будет обновлён.")
                }

                delay(200)
                (context as? Activity)?.recreate()
            }.onFailure { error ->
                backupRestoreStatusMessage = "Не удалось восстановить копию: ${error.message ?: "неизвестно"}"
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
                saveShiftColor(key, colorValue)
            },
            onResetDefaults = {
                resetShiftColors()
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
                deleteManualHoliday(record.date)
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
                pendingBackupJsonContent = exportAppBackupJson(
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
                    deleteManualHoliday(editingManualHolidayDate!!)
                }
                saveManualHoliday(record)
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
                        removeShiftSpecialRule(oldCode)
                        shiftAlarmStore.removeTemplateConfig(oldCode)
                    }

                    saveShiftColor(
                        template.code,
                        parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                    )
                    shiftAlarmStore.upsertTemplateConfig(alarmTemplateConfig.copy(shiftCode = template.code))
                }

                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
            },
            onSaveSpecialRule = { code, rule ->
                saveShiftSpecialRule(code, rule)
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
                    removeShiftSpecialRule(template.code)
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
fun MonthHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit
) {
    val context = LocalContext.current

    val formatter = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    }

    val monthTitle = currentMonth.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
                .clickable(onClick = onPrevMonth),
            contentAlignment = Alignment.Center
        ) {
            Text("←", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clickable {
                    val initialDate = currentMonth.atDay(1)

                    DatePickerDialog(
                        context,
                        { _, year, month, _ ->
                            onPickMonth(YearMonth.of(year, month + 1))
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                }
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(10.dp))
                .clickable(onClick = onNextMonth),
            contentAlignment = Alignment.Center
        ) {
            Text("→", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ColorChoiceChip(
    colorValue: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(colorValue))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ShiftColorPalette(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val colors = shiftEditorPalette()
    val selectedColorInt = parseColorHex(selectedColorHex, 0xFFE0E0E0.toInt())

    Column {
        Text(
            text = "Цвет смены",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        colors.chunked(5).forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowColors.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(colorInt))
                            .border(
                                width = if (colorInt == selectedColorInt) 3.dp else 1.dp,
                                color = if (colorInt == selectedColorInt) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onColorSelected(colorIntToHex(colorInt))
                            }
                    )
                }

                repeat(5 - rowColors.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.trackContinuousTouch(
    onTouch: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onTouch(down.position)

        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            onTouch(change.position)
            change.consume()
        } while (change.pressed)
    }
}

@Composable
fun FullColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val favoriteColors = listOf(
        "#1E88E5", "#1976D2", "#5C6BC0", "#7E57C2",
        "#43A047", "#26A69A", "#F9A825", "#FB8C00",
        "#EF5350", "#D81B60", "#8D6E63", "#78909C"
    )

    val initialHsv = remember(selectedColorHex) { hexToHsv(selectedColorHex) }

    var hue by remember(selectedColorHex) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(selectedColorHex) { mutableStateOf(initialHsv[1]) }
    var value by remember(selectedColorHex) { mutableStateOf(initialHsv[2]) }

    var colorAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var hueBarSize by remember { mutableStateOf(IntSize.Zero) }

    val selectedColor = remember(hue, saturation, value) {
        Color.hsv(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    }

    fun commitColor(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newValue: Float = value
    ) {
        val color = Color.hsv(
            newHue.coerceIn(0f, 360f),
            newSaturation.coerceIn(0f, 1f),
            newValue.coerceIn(0f, 1f)
        )
        onColorSelected(colorIntToHex(color.toArgb()))
    }

    fun updateColorArea(offset: Offset) {
        if (colorAreaSize.width <= 0 || colorAreaSize.height <= 0) return

        val newSaturation = (offset.x / colorAreaSize.width.toFloat()).coerceIn(0f, 1f)
        val newValue = (1f - (offset.y / colorAreaSize.height.toFloat())).coerceIn(0f, 1f)

        saturation = newSaturation
        value = newValue
        commitColor(newSaturation = newSaturation, newValue = newValue)
    }

    fun updateHueBar(offset: Offset) {
        if (hueBarSize.width <= 0) return

        val newHue = ((offset.x / hueBarSize.width.toFloat()).coerceIn(0f, 1f)) * 360f
        hue = newHue
        commitColor(newHue = newHue)
    }

    val colorIndicatorX = saturation * colorAreaSize.width
    val colorIndicatorY = (1f - value) * colorAreaSize.height
    val hueIndicatorX = (hue / 360f) * hueBarSize.width

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Текущий цвет",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(selectedColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(21.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Избранные",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        favoriteColors.chunked(6).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { colorHex ->
                    FavoriteColorChip(
                        colorHex = colorHex,
                        selected = normalizeHexColor(selectedColorHex) == normalizeHexColor(colorHex),
                        onClick = { onColorSelected(colorHex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Точная настройка",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White,
                            Color.hsv(hue, 1f, 1f)
                        )
                    )
                )
                .onSizeChanged { colorAreaSize = it }
                .pointerInput(hue) {
                    trackContinuousTouch { offset ->
                        updateColorArea(offset)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = colorIndicatorX.roundToInt() - 10,
                            y = colorIndicatorY.roundToInt() - 10
                        )
                    }
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Оттенок",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
                .onSizeChanged { hueBarSize = it }
                .pointerInput(Unit) {
                    trackContinuousTouch { offset ->
                        updateHueBar(offset)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = hueIndicatorX.roundToInt() - 9,
                            y = 3
                        )
                    }
                    .size(width = 18.dp, height = 20.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(9.dp)
                    )
            )
        }
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

@Composable
fun PayrollNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(
                newValue
                    .replace(',', '.')
                    .filter { it.isDigit() || it == '.' }
            )
        },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
fun PayrollIntField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue.filter { it.isDigit() })
        },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
fun IconChoiceButton(
    iconKey: String,
    codeFallback: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconGlyph(iconKey, codeFallback),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ColorChoiceButton(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(parseColorHex(colorHex, 0xFFE0E0E0.toInt())))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun FavoriteColorChip(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(parseColorHex(colorHex, 0xFFE0E0E0.toInt())))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
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
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    return month.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString()
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