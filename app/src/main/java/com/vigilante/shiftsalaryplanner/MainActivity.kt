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
const val KEY_EMPTY_DAY = "__EMPTY_DAY__"
const val BRUSH_CLEAR = "__BRUSH_CLEAR__"
private val SYSTEM_SHIFT_CODES = setOf("ВЫХ", "ОТ", "Б")

private const val PREFS_MANUAL_HOLIDAYS = "manual_holidays"
private const val PREFS_CALENDAR_SYNC = "calendar_sync_meta"
private const val CALENDAR_AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
private const val SICK_LIMITS_AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
const val PREFS_SICK_LIMITS_CACHE = "sick_limits_cache"
private const val MANUAL_HOLIDAY_SCOPE = "MANUAL"
private const val MANUAL_HOLIDAY_SEPARATOR = "\u001F"

private const val PREFS_ONE_TIME_MIGRATIONS = "one_time_migrations"
private const val KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1 = "legacy_defaults_cleanup_v1"
private const val LEGACY_EMBEDDED_BASE_SALARY = 102050.0
private const val LEGACY_EMBEDDED_EXTRA_SALARY = 49733.0

data class ManualHolidayRecord(
    val date: String,
    val title: String,
    val kind: String,
    val isNonWorking: Boolean
)

private data class CalendarSyncCheckResult(
    val updated: Boolean,
    val changedOnServer: Boolean,
    val dayCount: Int,
    val message: String
)

data class ShiftSpecialRule(
    val specialDayTypeName: String,
    val specialDayCompensationName: String
)

enum class BottomTab(val label: String, val icon: String) {
    CALENDAR("Календарь", "📅"),
    PAYROLL("Расчёт", "🧮"),
    PAYMENTS("Выплаты", "💸"),
    ALARMS("Будильники", "⏰"),
    SHIFTS("Смены", "📋"),
    SETTINGS("Настройки", "⚙️")
}
enum class TemplateMode {
    SHIFTS,
    CYCLES
}

private fun ShiftTemplateEntity.isSystemShiftTemplate(): Boolean = code in SYSTEM_SHIFT_CODES

private fun ShiftTemplateEntity.isAlarmEligibleShiftTemplate(): Boolean = !isSystemShiftTemplate()

private fun List<ShiftTemplateEntity>.alarmEligibleTemplates(): List<ShiftTemplateEntity> =
    filter { it.isAlarmEligibleShiftTemplate() }.sortedBy { it.sortOrder }

private fun neutralInitialPayrollSettings(): PayrollSettings = PayrollSettings(
    baseSalary = 0.0,
    extraSalary = 0.0
)


private fun Double.nearlyEquals(other: Double, epsilon: Double = 0.0001): Boolean =
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

private fun legacyBuiltInWorkingShiftTemplates(): List<ShiftTemplateEntity> = listOf(
    ShiftTemplateEntity(
        code = "Д",
        title = "Дневная",
        iconKey = "SUN",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 0.0,
        colorHex = "#1E88E5",
        isWeekendPaid = false,
        active = true,
        sortOrder = 10
    ),
    ShiftTemplateEntity(
        code = "Н",
        title = "Ночная",
        iconKey = "MOON",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 8.0,
        colorHex = "#43A047",
        isWeekendPaid = false,
        active = true,
        sortOrder = 20
    ),
    ShiftTemplateEntity(
        code = "8",
        title = "8 СП",
        iconKey = "EIGHT",
        totalHours = 8.0,
        breakHours = 0.0,
        nightHours = 0.0,
        colorHex = "#EF5350",
        isWeekendPaid = false,
        active = true,
        sortOrder = 30
    ),
    ShiftTemplateEntity(
        code = "РВД",
        title = "Работа в выходной день",
        iconKey = "STAR",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 0.0,
        colorHex = "#66BB6A",
        isWeekendPaid = true,
        active = true,
        sortOrder = 70
    ),
    ShiftTemplateEntity(
        code = "РВН",
        title = "Работа в выходной день (ночь)",
        iconKey = "STAR",
        totalHours = 11.5,
        breakHours = 0.75,
        nightHours = 8.0,
        colorHex = "#BDBDBD",
        isWeekendPaid = true,
        active = true,
        sortOrder = 80
    )
)

private val LEGACY_BUILT_IN_WORKING_SHIFT_BY_CODE =
    legacyBuiltInWorkingShiftTemplates().associateBy { it.code }

private fun ShiftTemplateEntity.matchesLegacyBuiltInWorkingTemplate(): Boolean {
    val legacy = LEGACY_BUILT_IN_WORKING_SHIFT_BY_CODE[code] ?: return false
    return title == legacy.title &&
            iconKey == legacy.iconKey &&
            totalHours.nearlyEquals(legacy.totalHours) &&
            breakHours.nearlyEquals(legacy.breakHours) &&
            nightHours.nearlyEquals(legacy.nightHours) &&
            colorHex.equals(legacy.colorHex, ignoreCase = true) &&
            isWeekendPaid == legacy.isWeekendPaid &&
            active == legacy.active &&
            sortOrder == legacy.sortOrder
}

private fun ShiftTemplateAlarmConfig?.isMeaningfullyCustomizedFor(template: ShiftTemplateEntity): Boolean {
    if (this == null) return false
    return this != defaultShiftTemplateAlarmConfig(template)
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
fun CalendarTab(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit,
    holidayMap: Map<LocalDate, HolidayEntity>,
    shiftCodesByDate: Map<LocalDate, String>,
    templateMap: Map<String, ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    quickShiftTemplates: List<ShiftTemplateEntity>,
    quickPickerOpen: Boolean,
    activeBrushCode: String?,
    isLegendExpanded: Boolean,
    onToggleLegend: () -> Unit,
    onOpenColorSettings: () -> Unit,
    onToggleQuickPicker: () -> Unit,
    onCloseQuickPicker: () -> Unit,
    onSelectBrush: (String) -> Unit,
    onClearBrush: () -> Unit,
    onDisableBrush: () -> Unit,
    onAddNewShift: () -> Unit,
    pendingPatternRangeStartDate: LocalDate?,
    pendingPatternRangeEndDate: LocalDate?,
    onOpenPatternPreview: () -> Unit,
    activePattern: PatternTemplate?,
    patternRangeStartDate: LocalDate?,
    onCancelPatternMode: () -> Unit,
    onOpenPatternEditor: () -> Unit,
    onEraseDate: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier

) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val swipeEnabled = activeBrushCode == null && activePattern == null
    val monthHolidayItems = remember(currentMonth, holidayMap) {
        holidayMap.entries
            .filter { YearMonth.from(it.key) == currentMonth }
            .sortedBy { it.key }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (isLandscape) 12.dp else 16.dp)
        ) {
            Box(
                modifier = Modifier.pointerInput(currentMonth, swipeEnabled) {
                    if (swipeEnabled) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                accumulated += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    accumulated > 80f -> onPrevMonth()
                                    accumulated < -80f -> onNextMonth()
                                }
                            }
                        )
                    }
                }
            ) {
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        val initialValue = initialState.year * 12 + initialState.monthValue
                        val targetValue = targetState.year * 12 + targetState.monthValue
                        if (targetValue > initialValue) {
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
                    label = "calendar_month"
                ) { shownMonth ->
                    if (isLandscape) {
                        Column {
                            MonthHeader(
                                currentMonth = shownMonth,
                                onPrevMonth = onPrevMonth,
                                onNextMonth = onNextMonth,
                                onPickMonth = onPickMonth
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.width(112.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (activePattern != null) {
                                        PatternApplyModeCard(
                                            pattern = activePattern,
                                            rangeStartDate = patternRangeStartDate,
                                            previewRangeStartDate = pendingPatternRangeStartDate,
                                            previewRangeEndDate = pendingPatternRangeEndDate,
                                            onOpenPreview = onOpenPatternPreview,
                                            onCancel = onCancelPatternMode
                                        )
                                    } else if (activeBrushCode != null) {
                                        ActiveBrushCard(
                                            activeBrushCode = activeBrushCode,
                                            templateMap = templateMap,
                                            onDisableBrush = onDisableBrush
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    CalendarGrid(
                                        currentMonth = shownMonth,
                                        shiftCodesByDate = shiftCodesByDate,
                                        holidayMap = holidayMap,
                                        templateMap = templateMap,
                                        shiftColors = shiftColors,
                                        activeBrushCode = activeBrushCode,
                                        previewRangeStartDate = pendingPatternRangeStartDate,
                                        previewRangeEndDate = pendingPatternRangeEndDate,
                                        onEraseDate = onEraseDate,
                                        onDayClick = onDayClick,
                                        compactMode = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            MonthHolidayInfoCard(
                                holidayEntries = monthHolidayItems
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ShiftLegend(
                                shiftTemplates = templateMap.values.sortedBy { it.sortOrder },
                                shiftColors = shiftColors,
                                isExpanded = isLegendExpanded,
                                onToggle = onToggleLegend,
                                onOpenSettings = onOpenColorSettings
                            )
                        }
                    } else {
                        Column {
                            MonthHeader(
                                currentMonth = shownMonth,
                                onPrevMonth = onPrevMonth,
                                onNextMonth = onNextMonth,
                                onPickMonth = onPickMonth
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (activePattern != null) {
                                PatternApplyModeCard(
                                    pattern = activePattern,
                                    rangeStartDate = patternRangeStartDate,
                                    previewRangeStartDate = pendingPatternRangeStartDate,
                                    previewRangeEndDate = pendingPatternRangeEndDate,
                                    onOpenPreview = onOpenPatternPreview,
                                    onCancel = onCancelPatternMode
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            } else if (activeBrushCode != null) {
                                ActiveBrushCard(
                                    activeBrushCode = activeBrushCode,
                                    templateMap = templateMap,
                                    onDisableBrush = onDisableBrush
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            CalendarGrid(
                                currentMonth = shownMonth,
                                shiftCodesByDate = shiftCodesByDate,
                                holidayMap = holidayMap,
                                templateMap = templateMap,
                                shiftColors = shiftColors,
                                activeBrushCode = activeBrushCode,
                                previewRangeStartDate = pendingPatternRangeStartDate,
                                previewRangeEndDate = pendingPatternRangeEndDate,
                                onEraseDate = onEraseDate,
                                onDayClick = onDayClick,
                                compactMode = false
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MonthHolidayInfoCard(
                                holidayEntries = monthHolidayItems
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ShiftLegend(
                                shiftTemplates = templateMap.values.sortedBy { it.sortOrder },
                                shiftColors = shiftColors,
                                isExpanded = isLegendExpanded,
                                onToggle = onToggleLegend,
                                onOpenSettings = onOpenColorSettings
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 100.dp))
        }

        FloatingActionButton(
            onClick = onToggleQuickPicker,
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isLandscape) 8.dp else 16.dp)
        ) {
            Text(if (quickPickerOpen) "✕" else "✎")
        }

        if (quickPickerOpen) {
            QuickShiftBar(
                shiftTemplates = quickShiftTemplates,
                activeBrushCode = activeBrushCode,
                onSelectBrush = onSelectBrush,
                onClearBrush = onClearBrush,
                onDisableBrush = onDisableBrush,
                onAddNewShift = onAddNewShift,
                onOpenPatternEditor = onOpenPatternEditor,
                onClose = onCloseQuickPicker,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = if (isLandscape) 88.dp else 16.dp, end = 16.dp, bottom = if (isLandscape) 12.dp else 84.dp)
            )
        }
    }
}

@Composable
fun ActiveBrushCard(
    activeBrushCode: String,
    templateMap: Map<String, ShiftTemplateEntity>,
    onDisableBrush: () -> Unit
) {
    val title = when (activeBrushCode) {
        BRUSH_CLEAR -> "Ластик"
        else -> {
            val template = templateMap[activeBrushCode]
            if (template != null) {
                "${template.code} — ${template.title}"
            } else {
                activeBrushCode
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Активный инструмент",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        TextButton(onClick = onDisableBrush) {
            Text("Выключить")
        }
    }
}

@Composable
fun PatternApplyModeCard(
    pattern: PatternTemplate,
    rangeStartDate: LocalDate?,
    previewRangeStartDate: LocalDate?,
    previewRangeEndDate: LocalDate?,
    onOpenPreview: () -> Unit,
    onCancel: () -> Unit
) {
    val subtitle = when {
        previewRangeStartDate != null && previewRangeEndDate != null -> {
            "Диапазон: ${formatDate(previewRangeStartDate)} — ${formatDate(previewRangeEndDate)}"
        }

        rangeStartDate != null -> {
            "Начало: ${formatDate(rangeStartDate)}. Выбери последний день"
        }

        else -> {
            "Выбери первый день диапазона"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Режим чередования",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = pattern.name.ifBlank { "Без названия" },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (previewRangeStartDate != null && previewRangeEndDate != null) {
                TextButton(onClick = onOpenPreview) {
                    Text("Предпросмотр")
                }
            }

            TextButton(onClick = onCancel) {
                Text("Сбросить")
            }
        }
    }
}

@Composable
fun QuickShiftBar(
    shiftTemplates: List<ShiftTemplateEntity>,
    activeBrushCode: String?,
    onSelectBrush: (String) -> Unit,
    onClearBrush: () -> Unit,
    onDisableBrush: () -> Unit,
    onAddNewShift: () -> Unit,
    onOpenPatternEditor: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mainItems = shiftTemplates.take(4)
    val extraItems = shiftTemplates.drop(4)
    var showMore by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF1B2232) else Color(0xFF20273F))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (showMore) "Все смены" else "Быстрый ввод",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (!showMore) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                mainItems.forEach { template ->
                    QuickShiftButton(
                        glyph = iconGlyph(template.iconKey, template.code),
                        title = template.code,
                        color = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())),
                        isSelected = activeBrushCode == template.code,
                        onClick = { onSelectBrush(template.code) },
                        modifier = Modifier.weight(1f),
                        useColorAsBackground = true
                    )
                }

                repeat(4 - mainItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickShiftButton(
                    glyph = "⌫",
                    title = "Ластик",
                    color = Color(0xFFEF9A9A.toInt()),
                    isSelected = activeBrushCode == BRUSH_CLEAR,
                    onClick = onClearBrush,
                    modifier = Modifier.weight(1f)
                )

                QuickShiftButton(
                    glyph = "•",
                    title = "Обычный",
                    color = Color(0xFFBDBDBD.toInt()),
                    isSelected = activeBrushCode == null,
                    onClick = onDisableBrush,
                    modifier = Modifier.weight(1f)
                )

                if (extraItems.isNotEmpty()) {
                    QuickShiftButton(
                        glyph = "⋯",
                        title = "Ещё",
                        color = Color(0xFF81C784.toInt()),
                        isSelected = false,
                        onClick = { showMore = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    QuickShiftButton(
                        glyph = "+",
                        title = "Новая",
                        color = Color(0xFF64B5F6.toInt()),
                        isSelected = false,
                        onClick = onAddNewShift,
                        modifier = Modifier.weight(1f)
                    )
                }

                QuickShiftButton(
                    glyph = "✕",
                    title = "Закрыть",
                    color = Color(0xFF90A4AE.toInt()),
                    isSelected = false,
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                extraItems.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { template ->
                            QuickShiftButton(
                                glyph = iconGlyph(template.iconKey, template.code),
                                title = template.title,
                                color = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())),
                                isSelected = activeBrushCode == template.code,
                                onClick = { onSelectBrush(template.code) },
                                modifier = Modifier.weight(1f),
                                useColorAsBackground = true
                            )
                        }

                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickShiftButton(
                    glyph = "+",
                    title = "Новая",
                    color = Color(0xFF64B5F6.toInt()),
                    isSelected = false,
                    onClick = onAddNewShift,
                    modifier = Modifier.weight(1f)
                )

                QuickShiftButton(
                    glyph = "↻",
                    title = "Черед.",
                    color = Color(0xFFFFB74D.toInt()),
                    isSelected = false,
                    onClick = onOpenPatternEditor,
                    modifier = Modifier.weight(1f)
                )

                QuickShiftButton(
                    glyph = "←",
                    title = "Назад",
                    color = Color(0xFF81C784.toInt()),
                    isSelected = false,
                    onClick = { showMore = false },
                    modifier = Modifier.weight(1f)
                )

                QuickShiftButton(
                    glyph = "✕",
                    title = "Закрыть",
                    color = Color(0xFF90A4AE.toInt()),
                    isSelected = false,
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickShiftButton(
    glyph: String,
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useColorAsBackground: Boolean = false
) {
    val backgroundColor = when {
        useColorAsBackground && isSelected -> color.copy(alpha = 0.42f)
        useColorAsBackground -> color.copy(alpha = 0.22f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.White
    }

    val borderColor = when {
        useColorAsBackground && isSelected -> color
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (backgroundColor.luminance() < 0.5f) {
        Color.White
    } else {
        Color(0xFF1A1A1A)
    }
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = glyph,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = (shiftGlyphFontSize(glyph) - 1).coerceAtLeast(12).sp,
            maxLines = 1,
            lineHeight = 12.sp
        )

        Text(
            text = title,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            maxLines = 1,
            lineHeight = 9.sp
        )
    }
}


@Composable
fun MonthlyReportScreen(
    currentMonth: YearMonth,
    payrollSettings: PayrollSettings,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats,
    onBack: () -> Unit,
    onExportCsv: () -> Unit
) {
    val activePayments = remember(additionalPayments) { additionalPayments.filter { it.active } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Отчёт за ${formatMonthYearTitle(currentMonth)}",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onExportCsv) {
                        Text("Экспорт CSV")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Период и режимы") {
                    PaymentInfoRow("Период", formatMonthYearTitle(currentMonth))
                    PaymentInfoRow("Режим оплаты", payModeLabel(payrollSettings.payMode))
                    PaymentInfoRow("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
                    PaymentInfoRow("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
                    PaymentInfoRow("Режим нормы", normModeLabel(payrollSettings.normMode))
                    PaymentInfoRow("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Отработанное время") {
                    PaymentInfoRow("Отработано часов", formatDouble(payroll.workedHours), bold = payroll.workedHours > 0.0)
                    PaymentInfoRow("Ночных часов", formatDouble(payroll.nightHours))
                    PaymentInfoRow("Праздничных/выходных часов", formatDouble(payroll.holidayHours))
                    PaymentInfoRow("Часовая ставка", formatMoney(payroll.hourlyRate))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Статистика смен") {
                    PaymentInfoRow("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
                    PaymentInfoRow("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
                    PaymentInfoRow("Дневных", detailedShiftStats.dayShiftCount.toString())
                    PaymentInfoRow("Ночных", detailedShiftStats.nightShiftCount.toString())
                    PaymentInfoRow("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
                    PaymentInfoRow("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
                    PaymentInfoRow("Отпуск", detailedShiftStats.vacationShiftCount.toString())
                    PaymentInfoRow("Больничный", detailedShiftStats.sickShiftCount.toString())
                    PaymentInfoRow("Смен в первой половине", detailedShiftStats.firstHalfWorkedShifts.toString())
                    PaymentInfoRow("Смен во 2-й половине", detailedShiftStats.secondHalfWorkedShifts.toString())
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Начисления") {
                    PaymentInfoRow("Базовая оплата", formatMoney(payroll.basePay))
                    PaymentInfoRow("Ночные", formatMoney(payroll.nightExtra))
                    PaymentInfoRow("РВД/праздничные", formatMoney(payroll.holidayExtra))
                    PaymentInfoRow(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
                    PaymentInfoRow("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
                    PaymentInfoRow("Всего начислено", formatMoney(payroll.grossTotal), bold = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Стоимость смены") {
                    PaymentInfoRow("База для расчёта", formatMoney(detailedShiftStats.shiftCostBaseTotal))
                    PaymentInfoRow("Учтено доплат и премий", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
                    PaymentInfoRow("Средняя стоимость смены (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross), bold = true)
                    PaymentInfoRow("Средняя стоимость смены (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet), bold = detailedShiftStats.shiftCostAverageNet > 0.0)
                    PaymentInfoRow("Дневная смена (до НДФЛ)", formatMoney(detailedShiftStats.dayShiftCostAverageGross), bold = detailedShiftStats.dayShiftCostAverageGross > 0.0)
                    PaymentInfoRow("Дневная смена (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet), bold = detailedShiftStats.dayShiftCostAverageNet > 0.0)
                    PaymentInfoRow("Ночная смена (до НДФЛ)", formatMoney(detailedShiftStats.nightShiftCostAverageGross), bold = detailedShiftStats.nightShiftCostAverageGross > 0.0)
                    PaymentInfoRow("Ночная смена (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet), bold = detailedShiftStats.nightShiftCostAverageNet > 0.0)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Отпуск и больничный") {
                    PaymentInfoRow("Дней отпуска", payroll.vacationDays.toString())
                    PaymentInfoRow("Отпускные", formatMoney(payroll.vacationPay))
                    PaymentInfoRow("Дней больничного", payroll.sickDays.toString())
                    PaymentInfoRow("Больничный", formatMoney(payroll.sickPay))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Налоги и выплаты") {
                    PaymentInfoRow("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
                    PaymentInfoRow("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
                    PaymentInfoRow("НДФЛ", formatMoney(payroll.ndfl))
                    PaymentInfoRow("На руки", formatMoney(payroll.netTotal), bold = true)
                    PaymentInfoRow("Аванс", formatMoney(payroll.advanceAmount))
                    PaymentInfoRow("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
                    PaymentInfoRow("Дата аванса", formatDate(paymentDates.advanceDate))
                    PaymentInfoRow("К зарплате", formatMoney(payroll.salaryPaymentAmount))
                    PaymentInfoRow("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
                    PaymentInfoRow("Дата зарплаты", formatDate(paymentDates.salaryDate))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Сверхурочка") {
                    PaymentInfoRow("Период", annualOvertime.periodLabel)
                    PaymentInfoRow("Статус", if (annualOvertime.enabled) "Включена" else "Отключена")
                    PaymentInfoRow("Норма периода", formatDouble(annualOvertime.annualNormHours))
                    PaymentInfoRow("Отработано", formatDouble(annualOvertime.workedHours))
                    PaymentInfoRow("Переработка до исключений", formatDouble(annualOvertime.rawOvertimeHours))
                    PaymentInfoRow("Исключено из переработки", formatDouble(annualOvertime.holidayExcludedHours))
                    PaymentInfoRow("К оплате как сверхурочные", formatDouble(annualOvertime.payableOvertimeHours), bold = annualOvertime.payableOvertimeHours > 0.0)
                    PaymentInfoRow("Доплата за переработку", formatMoney(annualOvertime.overtimePremiumAmount), bold = annualOvertime.overtimePremiumAmount > 0.0)
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Сработавшие доплаты и премии") {
                    if (resolvedAdditionalPaymentsBreakdown.isEmpty()) {
                        Text("В этом месяце дополнительных начислений нет.")
                    } else {
                        resolvedAdditionalPaymentsBreakdown.forEachIndexed { index, item ->
                            PaymentInfoRow(item.payment.displayName, additionalPaymentTypeLabel(item.payment.sourceTypeName), bold = true)
                            PaymentInfoRow("До НДФЛ", formatMoney(item.grossAmount), bold = item.grossAmount != 0.0)
                            PaymentInfoRow("НДФЛ", formatMoney(item.ndflAmount))
                            PaymentInfoRow("На руки", formatMoney(item.netAmount), bold = item.netAmount != 0.0)
                            PaymentInfoRow("Параметры", buildString {
                                append(if (item.payment.withAdvance) "в аванс" else "в зарплату")
                                append(" • ")
                                append(if (item.payment.taxable) "облагается" else "не облагается")
                            })
                            if (index != resolvedAdditionalPaymentsBreakdown.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                if (activePayments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    InfoCard(title = "Активные настройки начислений") {
                        activePayments.forEachIndexed { index, payment ->
                            PaymentInfoRow(payment.name.ifBlank { "Без названия" }, additionalPaymentTypeLabel(payment.type), bold = true)
                            PaymentInfoRow("Параметры", additionalPaymentDetailsLabel(payment))
                            PaymentInfoRow("Куда начислять", paymentDistributionLabel(payment.distribution))
                            if (index != activePayments.lastIndex) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun buildMonthlyReportCsv(
    currentMonth: YearMonth,
    payrollSettings: PayrollSettings,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    additionalPayments: List<AdditionalPayment>,
    resolvedAdditionalPaymentsBreakdown: List<ResolvedAdditionalPaymentBreakdown>,
    detailedShiftStats: DetailedShiftStats
): String {
    val rows = mutableListOf<List<String>>()
    val activePayments = additionalPayments.filter { it.active }

    rows += listOf("Показатель", "Значение")
    rows += listOf("Период", formatMonthYearTitle(currentMonth))
    rows += listOf("Режим оплаты", payModeLabel(payrollSettings.payMode))
    rows += listOf("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
    rows += listOf("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
    rows += listOf("Режим нормы", normModeLabel(payrollSettings.normMode))
    rows += listOf("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
    rows += listOf("Отработано часов", formatDouble(payroll.workedHours))
    rows += listOf("Ночных часов", formatDouble(payroll.nightHours))
    rows += listOf("Праздничных/выходных часов", formatDouble(payroll.holidayHours))
    rows += listOf("Всего отмеченных дней", detailedShiftStats.totalAssignedDays.toString())
    rows += listOf("Рабочих смен", detailedShiftStats.workedShiftCount.toString())
    rows += listOf("Дневных", detailedShiftStats.dayShiftCount.toString())
    rows += listOf("Ночных", detailedShiftStats.nightShiftCount.toString())
    rows += listOf("Выходных/праздничных", detailedShiftStats.weekendHolidayShiftCount.toString())
    rows += listOf("Восьмичасовой раб.день", detailedShiftStats.eightHourShiftCount.toString())
    rows += listOf("Отпуск", detailedShiftStats.vacationShiftCount.toString())
    rows += listOf("Больничный", detailedShiftStats.sickShiftCount.toString())
    rows += listOf("Часовая ставка", formatMoney(payroll.hourlyRate))
    rows += listOf("Базовая оплата", formatMoney(payroll.basePay))
    rows += listOf("Ночные", formatMoney(payroll.nightExtra))
    rows += listOf("РВД/праздничные", formatMoney(payroll.holidayExtra))
    rows += listOf(displayHousingPaymentLabel(housingPaymentLabel), formatMoney(payroll.housingPayment))
    rows += listOf("Допвыплаты всего", formatMoney(payroll.additionalPaymentsTotal))
    rows += listOf("Стоимость смены: база", formatMoney(detailedShiftStats.shiftCostBaseTotal))
    rows += listOf("Стоимость смены: из них доплаты/премии", formatMoney(detailedShiftStats.shiftCostIncludedPayments))
    rows += listOf("Стоимость смены: средняя (до НДФЛ)", formatMoney(detailedShiftStats.shiftCostAverageGross))
    rows += listOf("Стоимость смены: средняя (на руки)", formatMoney(detailedShiftStats.shiftCostAverageNet))
    rows += listOf("Стоимость смены: дневная (до НДФЛ)", formatMoney(detailedShiftStats.dayShiftCostAverageGross))
    rows += listOf("Стоимость смены: дневная (на руки)", formatMoney(detailedShiftStats.dayShiftCostAverageNet))
    rows += listOf("Стоимость смены: ночная (до НДФЛ)", formatMoney(detailedShiftStats.nightShiftCostAverageGross))
    rows += listOf("Стоимость смены: ночная (на руки)", formatMoney(detailedShiftStats.nightShiftCostAverageNet))
    rows += listOf("Дней отпуска", payroll.vacationDays.toString())
    rows += listOf("Отпускные", formatMoney(payroll.vacationPay))
    rows += listOf("Дней больничного", payroll.sickDays.toString())
    rows += listOf("Больничный", formatMoney(payroll.sickPay))
    rows += listOf("Облагаемая база", formatMoney(payroll.taxableGrossTotal))
    rows += listOf("Необлагаемые выплаты", formatMoney(payroll.nonTaxableTotal))
    rows += listOf("Всего начислено", formatMoney(payroll.grossTotal))
    rows += listOf("НДФЛ", formatMoney(payroll.ndfl))
    rows += listOf("На руки", formatMoney(payroll.netTotal))
    rows += listOf("Аванс", formatMoney(payroll.advanceAmount))
    rows += listOf("Аванс только по сменам", formatMoney(payroll.shiftOnlyAdvanceNetAmount))
    rows += listOf("Дата аванса", formatDate(paymentDates.advanceDate))
    rows += listOf("К зарплате", formatMoney(payroll.salaryPaymentAmount))
    rows += listOf("Зарплата только по сменам", formatMoney(payroll.shiftOnlySalaryNetAmount))
    rows += listOf("Дата зарплаты", formatDate(paymentDates.salaryDate))
    rows += listOf("Сверхурочка: период", annualOvertime.periodLabel)
    rows += listOf("Сверхурочка: статус", if (annualOvertime.enabled) "Включена" else "Отключена")
    rows += listOf("Сверхурочка: норма периода", formatDouble(annualOvertime.annualNormHours))
    rows += listOf("Сверхурочка: отработано", formatDouble(annualOvertime.workedHours))
    rows += listOf("Сверхурочка: переработка до исключений", formatDouble(annualOvertime.rawOvertimeHours))
    rows += listOf("Сверхурочка: исключено", formatDouble(annualOvertime.holidayExcludedHours))
    rows += listOf("Сверхурочка: к оплате", formatDouble(annualOvertime.payableOvertimeHours))
    rows += listOf("Сверхурочка: доплата", formatMoney(annualOvertime.overtimePremiumAmount))

    if (resolvedAdditionalPaymentsBreakdown.isNotEmpty()) {
        rows += listOf("", "")
        rows += listOf("Сработавшие доплаты и премии", "Значение")
        resolvedAdditionalPaymentsBreakdown.forEach { payment ->
            rows += listOf(payment.payment.displayName, additionalPaymentTypeLabel(payment.payment.sourceTypeName))
            rows += listOf("  До НДФЛ", formatMoney(payment.grossAmount))
            rows += listOf("  НДФЛ", formatMoney(payment.ndflAmount))
            rows += listOf("  На руки", formatMoney(payment.netAmount))
            rows += listOf(
                "  Параметры",
                buildString {
                    append(if (payment.payment.withAdvance) "в аванс" else "в зарплату")
                    append(" • ")
                    append(if (payment.payment.taxable) "облагается" else "не облагается")
                }
            )
        }
    }

    if (activePayments.isNotEmpty()) {
        rows += listOf("", "")
        rows += listOf("Активные настройки начислений", "Параметры")
        activePayments.forEach { payment ->
            rows += listOf(
                payment.name.ifBlank { "Без названия" },
                "${additionalPaymentTypeLabel(payment.type)} • ${additionalPaymentDetailsLabel(payment)} • ${paymentDistributionLabel(payment.distribution)}"
            )
        }
    }

    return rows.joinToString("\n") { row ->
        row.joinToString(";") { value -> csvEscape(value) }
    }
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}



@Composable
fun ManualHolidaysScreen(
    records: List<ManualHolidayRecord>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (ManualHolidayRecord) -> Unit,
    onDelete: (ManualHolidayRecord) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeaderAction(
                title = "Ручные праздники",
                onBack = onBack,
                actionText = "+",
                onAction = onAdd
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Локальные дни",
                    subtitle = "Можно добавить региональные праздники и особые дни поверх загруженного календаря"
                ) {
                    if (records.isEmpty()) {
                        Text(
                            text = "Пока нет ручных праздничных дней.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        records.forEachIndexed { index, record ->
                            ManualHolidayRow(
                                record = record,
                                onEdit = { onEdit(record) },
                                onDelete = { onDelete(record) }
                            )

                            if (index != records.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ManualHolidayRow(
    record: ManualHolidayRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeLabel = when {
        record.kind == HolidayKinds.SHORT_DAY -> "Сокращённый день"
        record.isNonWorking -> "Нерабочий праздник"
        else -> "Особый день"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDate(LocalDate.parse(record.date)),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = record.title.ifBlank { typeLabel },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row {
            TextButton(onClick = onEdit) {
                Text("Изм.")
            }
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

@Composable
fun BackupRestoreScreen(
    shiftDaysCount: Int,
    shiftTemplatesCount: Int,
    additionalPaymentsCount: Int,
    patternTemplatesCount: Int,
    manualHolidayCount: Int,
    statusMessage: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Резервная копия",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Что входит в копию",
                    subtitle = "Экспортируются все основные пользовательские данные",
                    content = {
                        Text("Смены в календаре: $shiftDaysCount")
                        Text("Шаблоны смен: $shiftTemplatesCount")
                        Text("Доплаты и премии: $additionalPaymentsCount")
                        Text("Шаблоны чередований: $patternTemplatesCount")
                        Text("Ручные праздники: $manualHolidayCount")
                        Text("Также сохраняются: зарплатные настройки, будильники, цвета и спецправила смен.")
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSectionCard(
                    title = "Действия",
                    subtitle = "Экспорт и импорт резервной копии",
                    content = {
                        Text(
                            text = "Экспорт создаёт JSON-файл, который можно перенести на другое устройство. Импорт заменяет текущие данные приложения данными из файла.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExport,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Экспорт")
                            }

                            Button(
                                onClick = onImport,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Импорт")
                            }
                        }
                    }
                )

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionCard(
                        title = "Статус",
                        subtitle = "Последний результат операции",
                        content = {
                            Text(statusMessage)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetShiftSettingsCard(
    template: ShiftTemplateEntity,
    calendarColorInt: Int,
    initialSettings: WidgetShiftOverride,
    refreshToken: Int,
    onSave: (WidgetShiftOverride) -> Unit,
    onReset: () -> Unit,
    onDraftChanged: (WidgetShiftOverride, Boolean) -> Unit
) {
    var linkWithTemplate by remember(refreshToken, template.code) { mutableStateOf(initialSettings.linkWithTemplate) }
    var fullLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.fullLabel) }
    var shortLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.shortLabel) }
    var metaLabel by remember(refreshToken, template.code) { mutableStateOf(initialSettings.metaLabel) }
    var useCustomColor by remember(refreshToken, template.code) { mutableStateOf(initialSettings.useCustomColor) }
    var colorHex by remember(refreshToken, template.code) {
        mutableStateOf(initialSettings.colorHex.ifBlank { colorIntToHex(calendarColorInt) })
    }
    var showColorDialog by remember(refreshToken, template.code) { mutableStateOf(false) }

    val defaultFull = defaultWidgetLongLabel(template)
    val defaultShort = defaultWidgetShortLabel(template)
    val defaultMeta = defaultWidgetMetaLabel(template)

    val draftOverride = remember(linkWithTemplate, fullLabel, shortLabel, metaLabel, useCustomColor, colorHex) {
        WidgetShiftOverride(
            fullLabel = if (linkWithTemplate) "" else fullLabel.trim(),
            shortLabel = if (linkWithTemplate) "" else shortLabel.trim(),
            metaLabel = if (linkWithTemplate) "" else metaLabel.trim(),
            useCustomColor = if (linkWithTemplate) false else useCustomColor,
            colorHex = if (!linkWithTemplate && useCustomColor) normalizeHexColor(colorHex) else "",
            linkWithTemplate = linkWithTemplate
        )
    }

    val hasChanges = remember(draftOverride, initialSettings, calendarColorInt) {
        draftOverride != WidgetShiftOverride(
            fullLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.fullLabel.trim(),
            shortLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.shortLabel.trim(),
            metaLabel = if (initialSettings.linkWithTemplate) "" else initialSettings.metaLabel.trim(),
            useCustomColor = if (initialSettings.linkWithTemplate) false else initialSettings.useCustomColor,
            colorHex = if (!initialSettings.linkWithTemplate && initialSettings.useCustomColor) {
                normalizeHexColor(initialSettings.colorHex.ifBlank { colorIntToHex(calendarColorInt) })
            } else {
                ""
            },
            linkWithTemplate = initialSettings.linkWithTemplate
        )
    }

    LaunchedEffect(draftOverride, hasChanges) {
        onDraftChanged(draftOverride, hasChanges)
    }

    SettingsSectionCard(
        title = "${template.title.ifBlank { template.code }} (${template.code})",
        subtitle = "По умолчанию: $defaultFull • $defaultShort • $defaultMeta"
    ) {
        CompactSwitchRow(
            title = "Связать с шаблоном",
            checked = linkWithTemplate,
            onCheckedChange = { linkWithTemplate = it }
        )

        if (linkWithTemplate) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Виджет будет брать цвет и стандартные подписи из шаблона смены.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (!linkWithTemplate) {
            OutlinedTextField(
                value = fullLabel,
                onValueChange = { fullLabel = it },
                label = { Text("Полная подпись") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(defaultFull) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = shortLabel,
                onValueChange = { shortLabel = it },
                label = { Text("Короткая подпись") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(defaultShort) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = metaLabel,
                onValueChange = { metaLabel = it },
                label = { Text("Нижняя подпись") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(defaultMeta) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CompactSwitchRow(
                title = "Использовать свой цвет в виджете",
                checked = useCustomColor,
                onCheckedChange = { useCustomColor = it }
            )

            if (useCustomColor) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(parseColorHex(colorHex, calendarColorInt)))
                            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                    )

                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = normalizeHexColor(it) },
                        label = { Text("HEX") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showColorDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Выбрать цвет")
                    }
                    OutlinedButton(
                        onClick = { colorHex = colorIntToHex(calendarColorInt) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Цвет календаря")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onSave(draftOverride)
                },
                enabled = hasChanges,
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить")
            }
            OutlinedButton(
                onClick = {
                    linkWithTemplate = true
                    fullLabel = ""
                    shortLabel = ""
                    metaLabel = ""
                    useCustomColor = false
                    colorHex = colorIntToHex(calendarColorInt)
                    onReset()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сбросить")
            }
        }
    }

    if (showColorDialog) {
        Dialog(
            onDismissRequest = { showColorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Цвет для ${template.title.ifBlank { template.code }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FullColorPicker(
                        selectedColorHex = colorHex,
                        onColorSelected = { colorHex = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showColorDialog = false }) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun PaymentInfoRow(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AdditionalPaymentsCard(
    payments: List<AdditionalPayment>,
    onAddPayment: () -> Unit,
    onEditPayment: (AdditionalPayment) -> Unit,
    onDeletePayment: (AdditionalPayment) -> Unit
) {
    InfoCard(title = "Доплаты, выплаты и премии") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onAddPayment) {
                Text("Добавить")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (payments.isEmpty()) {
            Text("Пока нет ни одного начисления.")
        } else {
            payments.forEach { payment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onEditPayment(payment) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = payment.name.ifBlank { "Без названия" },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(additionalPaymentTypeLabel(payment.type))
                    Text(additionalPaymentDetailsLabel(payment))
                    Text(
                        buildString {
                            append(paymentDistributionLabel(payment.distribution))
                            append(" • ")
                            append(if (payment.taxable) "облагается НДФЛ" else "не облагается")
                            append(" • ")
                            append(if (payment.includeInShiftCost) "входит в стоимость смены" else "не входит в стоимость смены")
                            append(" • ")
                            append(if (payment.active) "активна" else "неактивна")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onEditPayment(payment) }) {
                            Text("Изменить")
                        }
                        TextButton(onClick = { onDeletePayment(payment) }) {
                            Text("Удалить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
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
fun SummaryCard(
    summary: MonthSummary,
    payroll: PayrollResult,
    annualOvertime: AnnualOvertimeResult,
    paymentDates: PaymentDates,
    housingPaymentLabel: String,
    detailedShiftStats: DetailedShiftStats,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Сводка за месяц",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "Нажми, чтобы свернуть" else "Нажми, чтобы развернуть",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = if (isExpanded) "▲" else "▼",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenSettings) {
                Text("Настройки")
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("Рабочих дней: ${summary.workedDays}")
            Text("Оплачиваемых часов: ${formatDouble(summary.workedHours)}")
            Text("Ночных часов: ${formatDouble(summary.nightHours)}")
            Text("Праздничных/выходных часов: ${formatDouble(payroll.holidayHours)}")
            Text("Дней отпуска: ${payroll.vacationDays}")
            Text("Дней больничного: ${payroll.sickDays}")
            Text("Сверхурочка (${annualOvertime.periodLabel}): ${formatDouble(annualOvertime.payableOvertimeHours)} ч")
            Text("Всего отмеченных дней: ${detailedShiftStats.totalAssignedDays}")
            Text("Рабочих смен: ${detailedShiftStats.workedShiftCount}")
            Text("Дневных / ночных: ${detailedShiftStats.dayShiftCount} / ${detailedShiftStats.nightShiftCount}")
            Text("Выходных/праздничных смен: ${detailedShiftStats.weekendHolidayShiftCount}")
            Text("Восьмичасовой раб.день: ${detailedShiftStats.eightHourShiftCount}")
            Text("Смен в первой половине: ${detailedShiftStats.firstHalfWorkedShifts}")
            Text("Смен во второй половине: ${detailedShiftStats.secondHalfWorkedShifts}")
            if (detailedShiftStats.workedShiftCount > 0) {
                Text("Средняя стоимость смены: ${formatMoney(detailedShiftStats.shiftCostAverageGross)} / ${formatMoney(detailedShiftStats.shiftCostAverageNet)}")
                Text("Дневная смена: ${formatMoney(detailedShiftStats.dayShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.dayShiftCostAverageNet)}")
                Text("Ночная смена: ${formatMoney(detailedShiftStats.nightShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.nightShiftCostAverageNet)}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Часовая ставка: ${formatMoney(payroll.hourlyRate)}")
            Text("База: ${formatMoney(payroll.basePay)}")
            Text("Ночные: ${formatMoney(payroll.nightExtra)}")
            Text("Праздничные/выходные: ${formatMoney(payroll.holidayExtra)}")
            Text("Отпускные: ${formatMoney(payroll.vacationPay)}")
            Text("Больничный: ${formatMoney(payroll.sickPay)}")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "${displayHousingPaymentLabel(housingPaymentLabel)}: ${formatMoney(payroll.housingPayment)} " +
                        if (payroll.housingPaymentTaxable) "(облагается)" else "(не облагается)"
            )
            Text("Из неё в аванс: ${formatMoney(payroll.housingAdvancePart)}")
            Text("Из неё в зарплату: ${formatMoney(payroll.housingSalaryPart)}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("Допвыплаты всего: ${formatMoney(payroll.additionalPaymentsTotal)}")
            Text("Из них в аванс: ${formatMoney(payroll.additionalPaymentsAdvancePart)}")
            Text("Из них в зарплату: ${formatMoney(payroll.additionalPaymentsSalaryPart)}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("Облагаемая база: ${formatMoney(payroll.taxableGrossTotal)}")
            Text("Необлагаемые выплаты: ${formatMoney(payroll.nonTaxableTotal)}")
            Text("Всего начислено: ${formatMoney(payroll.grossTotal)}")
            Text("НДФЛ: ${formatMoney(payroll.ndfl)}")
            Text("Доплата за переработку: ${formatMoney(annualOvertime.overtimePremiumAmount)}")
            if (payroll.taxableIncomeYtdAfterCurrentMonth > 0.0) {
                Text("Налоговая база с начала года до месяца: ${formatMoney(payroll.taxableIncomeYtdBeforeCurrentMonth)}")
                Text("Налоговая база с начала года после месяца: ${formatMoney(payroll.taxableIncomeYtdAfterCurrentMonth)}")
            }
            Text("На руки за месяц: ${formatMoney(payroll.netTotal)}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("Аванс: ${formatMoney(payroll.advanceAmount)}")
            Text("Аванс только по сменам: ${formatMoney(payroll.shiftOnlyAdvanceNetAmount)}")
            Text("Дата аванса: ${formatDate(paymentDates.advanceDate)}")
            Text(
                text = "К зарплате: ${formatMoney(payroll.salaryPaymentAmount)}",
                fontWeight = FontWeight.Bold
            )
            Text("Зарплата только по сменам: ${formatMoney(payroll.shiftOnlySalaryNetAmount)}")
            Text("Дата зарплаты: ${formatDate(paymentDates.salaryDate)}")
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Часы: ${formatDouble(summary.workedHours)}")
            Text("Смен: ${detailedShiftStats.workedShiftCount} (Д ${detailedShiftStats.dayShiftCount} / Н ${detailedShiftStats.nightShiftCount})")
            if (detailedShiftStats.workedShiftCount > 0) {
                Text("Средняя смена (до/после НДФЛ): ${formatMoney(detailedShiftStats.shiftCostAverageGross)} / ${formatMoney(detailedShiftStats.shiftCostAverageNet)}")
                Text("Дневная (до/после НДФЛ): ${formatMoney(detailedShiftStats.dayShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.dayShiftCostAverageNet)}")
                Text("Ночная (до/после НДФЛ): ${formatMoney(detailedShiftStats.nightShiftCostAverageGross)} / ${formatMoney(detailedShiftStats.nightShiftCostAverageNet)}")
            }
            Text("Аванс: ${formatMoney(payroll.advanceAmount)}")
            if (payroll.vacationPay > 0.0 || payroll.sickPay > 0.0) {
                Text("Отп./бол.: ${formatMoney(payroll.vacationPay + payroll.sickPay)}")
            }
            if (annualOvertime.payableOvertimeHours > 0.0) {
                Text("Сверхурочка: ${formatDouble(annualOvertime.payableOvertimeHours)} ч")
            }
            Text(
                text = "К зарплате: ${formatMoney(payroll.salaryPaymentAmount)}",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ShiftLegend(
    shiftTemplates: List<ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Обозначения смен",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExpanded) "Нажми, чтобы свернуть" else "Нажми, чтобы развернуть",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = if (isExpanded) "▲" else "▼",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text("Цвета")
                }
            }

            shiftTemplates.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        LegendItem(
                            code = item.code,
                            label = item.title,
                            color = Color(
                                shiftColors[item.code]
                                    ?: parseColorHex(item.colorHex, defaultShiftColors()[item.code] ?: 0xFFE0E0E0.toInt())
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    code: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = code,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    shiftCodesByDate: Map<LocalDate, String>,
    holidayMap: Map<LocalDate, HolidayEntity>,
    templateMap: Map<String, ShiftTemplateEntity>,
    shiftColors: Map<String, Int>,
    activeBrushCode: String?,
    previewRangeStartDate: LocalDate?,
    previewRangeEndDate: LocalDate?,
    onEraseDate: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    compactMode: Boolean = false
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val cellBounds = remember(currentMonth) { mutableStateMapOf<LocalDate, Rect>() }
    val gap = if (compactMode) 4.dp else 6.dp
    val cellHeight = if (compactMode) 58.dp else 72.dp

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            daysOfWeek.forEachIndexed { index, dayName ->
                val isWeekendHeader = index >= 5

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compactMode) 24.dp else 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        fontWeight = FontWeight.Bold,
                        color = if (isWeekendHeader) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (compactMode) 6.dp else 8.dp))

        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val leadingCells = firstDay.dayOfWeek.value - 1
        val firstVisibleDate = firstDay.minusDays(leadingCells.toLong())
        val totalCells = ((leadingCells + daysInMonth + 6) / 7) * 7
        val calendarCells = List(totalCells) { offset -> firstVisibleDate.plusDays(offset.toLong()) }
        val weeks = calendarCells.chunked(7)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(activeBrushCode, cellBounds.size) {
                    if (activeBrushCode != null) {
                        var lastHitDate: LocalDate? = null

                        fun applyAt(position: Offset) {
                            val hitDate = cellBounds.entries
                                .firstOrNull { it.value.contains(position) }
                                ?.key

                            if (hitDate != null && hitDate != lastHitDate) {
                                lastHitDate = hitDate

                                if (activeBrushCode == BRUSH_CLEAR) {
                                    onEraseDate(hitDate)
                                } else {
                                    onDayClick(hitDate)
                                }
                            }
                        }

                        detectDragGestures(
                            onDragStart = { offset ->
                                applyAt(offset)
                            },
                            onDrag = { change, _ ->
                                applyAt(change.position)
                            },
                            onDragEnd = {
                                lastHitDate = null
                            },
                            onDragCancel = {
                                lastHitDate = null
                            }
                        )
                    }
                }
        ) {
            Column {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = gap),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        week.forEach { date ->
                            val isCurrentMonthCell = YearMonth.from(date) == currentMonth
                            val code = shiftCodesByDate[date]
                            val template = code?.let { templateMap[it] }
                            val isSpecialDay = isCalendarDayOff(date, holidayMap)
                            val isInPreviewRange = isDateInRange(
                                date = date,
                                start = previewRangeStartDate,
                                end = previewRangeEndDate
                            )
                            val isPreviewEdge =
                                date == previewRangeStartDate || date == previewRangeEndDate

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coordinates ->
                                        if (isCurrentMonthCell) {
                                            val pos = coordinates.positionInParent()
                                            cellBounds[date] = Rect(
                                                left = pos.x,
                                                top = pos.y,
                                                right = pos.x + coordinates.size.width,
                                                bottom = pos.y + coordinates.size.height
                                            )
                                        } else {
                                            cellBounds.remove(date)
                                        }
                                    }
                            ) {
                                DayCell(
                                    date = date,
                                    shiftCode = code,
                                    template = template,
                                    backgroundColor = shiftCellColor(code, shiftColors, templateMap),
                                    isSpecialDay = isSpecialDay,
                                    isInPreviewRange = isInPreviewRange,
                                    isPreviewEdge = isPreviewEdge,
                                    isCurrentMonthCell = isCurrentMonthCell,
                                    compactMode = compactMode,
                                    onClick = { onDayClick(date) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DayCell(
    date: LocalDate,
    shiftCode: String?,
    template: ShiftTemplateEntity?,
    backgroundColor: Color,
    isSpecialDay: Boolean,
    isInPreviewRange: Boolean,
    isPreviewEdge: Boolean,
    isCurrentMonthCell: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val isDark = isSystemInDarkTheme()
    val holidayTint = if (isDark) Color(0xFF3A2B35) else Color(0xFFFFEFEF)
    val emptyBase = if (isDark) Color(0xFF1B2231) else backgroundColor
    val previewTint = MaterialTheme.colorScheme.primaryContainer

    val baseCellBackground = when {
        shiftCode == null && isSpecialDay -> if (isDark) Color(0xFF312631) else holidayTint
        shiftCode == null -> emptyBase
        isSpecialDay -> lerp(backgroundColor, holidayTint, if (isDark) 0.24f else 0.22f)
        else -> backgroundColor
    }

    val specialBackground = if (isCurrentMonthCell) {
        baseCellBackground
    } else {
        lerp(baseCellBackground, MaterialTheme.colorScheme.background, if (isDark) 0.46f else 0.52f)
    }

    val finalBackground = if (isInPreviewRange) {
        lerp(specialBackground, previewTint, if (isPreviewEdge) 0.60f else 0.36f)
    } else {
        specialBackground
    }

    val borderColor = when {
        isPreviewEdge -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary
        !isCurrentMonthCell -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.70f else 0.35f)
        else -> if (isDark) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = when {
        isPreviewEdge -> 2.dp
        isToday -> 2.dp
        else -> 1.dp
    }
    val glyph = when {
        template != null -> iconGlyph(template.iconKey, template.code)
        shiftCode != null -> shiftCode
        else -> ""
    }

    val glyphFontSize = (if (compactMode) shiftGlyphFontSize(glyph) - 2 else shiftGlyphFontSize(glyph)).coerceAtLeast(10).sp
    val mainTextColor = when {
        !isCurrentMonthCell && isSpecialDay -> Color(0xFFD32F2F).copy(alpha = 0.55f)
        !isCurrentMonthCell -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)
        isSpecialDay -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .height(if (compactMode) 58.dp else 72.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactMode) 10.dp else 12.dp))
            .background(finalBackground)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(if (compactMode) 10.dp else 12.dp)
            )
            .clickable(onClick = onClick)
            .padding(if (compactMode) 4.dp else 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = mainTextColor
        )

        Text(
            text = glyph,
            fontSize = glyphFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = if (isCurrentMonthCell) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        )
    }
}

@Composable
fun ShiftPickerOptionCard(
    template: ShiftTemplateEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
    val glyph = iconGlyph(template.iconKey, template.code)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = shiftGlyphFontSize(glyph).sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "Код: ${template.code}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = buildString {
                    append("Оплач. ")
                    append(formatDouble(template.paidHours()))
                    append(" ч")
                    if (template.breakHours > 0.0) {
                        append(" • Обед ")
                        append(formatDouble(template.breakHours))
                        append(" ч")
                    }
                    if (template.nightHours > 0.0) {
                        append(" • Ночь ")
                        append(formatDouble(template.nightHours))
                        append(" ч")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge
        )
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
    val summaryText = buildString {
        append(if (config.enabled) "Вкл" else "Выкл")
        append(" • всего ")
        append(config.alarms.size)
        append(" • активных ")
        append(activeAlarmCount)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(14.dp))
            .clickable { onToggleExpanded() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(parseColorHex(template.colorHex, 0xFF42A5F5.toInt())))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shiftAlarmTemplateLabel(template),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Будильников: ${config.alarms.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onAddAlarm) {
                    Text("+ Будильник")
                }
            }

            if (config.alarms.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Пока пусто. Можно добавить сколько угодно будильников.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                config.alarms.sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute }).forEach { alarm ->
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

@Composable
fun ShiftTemplateAlarmItemCard(
    alarm: ShiftAlarmConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.title.ifBlank { "Без названия" },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(formatClockHm(alarm.triggerHour, alarm.triggerMinute))
                    append(" • ")
                    append(alarm.volumePercent.coerceIn(0, 100))
                    append("% • ")
                    append(shiftAlarmSoundSummary(alarm))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            modifier = Modifier.scale(0.76f),
            checked = alarm.enabled,
            onCheckedChange = onToggleEnabled
        )

        TextButton(
            onClick = onEdit,
            modifier = Modifier.height(32.dp)
        ) {
            Text("✏️")
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.height(32.dp)
        ) {
            Text("🗑️")
        }
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
fun ShiftAlarmWheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftAlarmNumberWheel(
            label = "Часы",
            value = hour.coerceIn(0, 23),
            range = 0..23,
            formatter = { "%02d".format(it) },
            onValueChange = onHourChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        ShiftAlarmNumberWheel(
            label = "Минуты",
            value = minute.coerceIn(0, 59),
            range = 0..59,
            formatter = { "%02d".format(it) },
            onValueChange = onMinuteChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ShiftAlarmNumberWheel(
    label: String,
    value: Int,
    range: IntRange,
    formatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = range.first
                    maxValue = range.last
                    wrapSelectorWheel = true
                    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                    setFormatter { formatter(it) }
                    setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
                }
            },
            update = { picker ->
                picker.minValue = range.first
                picker.maxValue = range.last
                picker.displayedValues = null
                picker.setFormatter { formatter(it) }
                if (picker.value != value.coerceIn(range.first, range.last)) {
                    picker.value = value.coerceIn(range.first, range.last)
                }
            }
        )
    }
}

fun normalizeShiftAlarmSettings(settings: ShiftAlarmSettings): ShiftAlarmSettings {
    return settings.copy(
        scheduleHorizonDays = settings.scheduleHorizonDays.coerceIn(7, 365),
        templateConfigs = settings.templateConfigs
            .map { config ->
                config.copy(
                    startHour = config.startHour.coerceIn(0, 23),
                    startMinute = config.startMinute.coerceIn(0, 59),
                    endHour = config.endHour.coerceIn(0, 23),
                    endMinute = config.endMinute.coerceIn(0, 59),
                    alarms = config.alarms
                        .map { alarm ->
                            alarm.copy(
                                triggerHour = alarm.triggerHour.coerceIn(0, 23),
                                triggerMinute = alarm.triggerMinute.coerceIn(0, 59),
                                volumePercent = alarm.volumePercent.coerceIn(0, 100),
                                soundUri = alarm.soundUri?.takeIf { it.isNotBlank() },
                                soundLabel = alarm.soundLabel.trim()
                            )
                        }
                        .sortedWith(compareBy<ShiftAlarmConfig> { it.triggerHour }.thenBy { it.triggerMinute })
                )
            }
            .sortedBy { it.shiftCode }
    )
}

fun mergeShiftAlarmConfigsWithTemplates(
    settings: ShiftAlarmSettings,
    templates: List<ShiftTemplateEntity>
): List<ShiftTemplateAlarmConfig> {
    val existingByCode = settings.templateConfigs.associateBy { it.shiftCode }
    return templates
        .sortedBy { it.sortOrder }
        .map { template ->
            existingByCode[template.code] ?: defaultShiftTemplateAlarmConfig(template)
        }
}

fun upsertShiftTemplateAlarmConfig(
    items: List<ShiftTemplateAlarmConfig>,
    updated: ShiftTemplateAlarmConfig
): List<ShiftTemplateAlarmConfig> {
    val mutable = items.toMutableList()
    val index = mutable.indexOfFirst { it.shiftCode == updated.shiftCode }
    if (index >= 0) {
        mutable[index] = updated
    } else {
        mutable.add(updated)
    }
    return mutable
}

fun upsertShiftAlarmItem(
    items: List<ShiftAlarmConfig>,
    updated: ShiftAlarmConfig
): List<ShiftAlarmConfig> {
    val mutable = items.toMutableList()
    val index = mutable.indexOfFirst { it.id == updated.id }
    if (index >= 0) {
        mutable[index] = updated
    } else {
        mutable.add(updated)
    }
    return mutable
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
fun CurrentParametersScreen(
    payrollSettings: PayrollSettings,
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
                title = "Текущие параметры",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                InfoCard(title = "Оплата") {
                    PaymentInfoRow("Режим оплаты", payModeLabel(payrollSettings.payMode))
                    PaymentInfoRow("Режим надбавки", extraSalaryModeLabel(payrollSettings.extraSalaryMode))
                    PaymentInfoRow("Оклад", formatMoney(payrollSettings.baseSalary))
                    PaymentInfoRow("Надбавка", formatMoney(payrollSettings.extraSalary))
                    PaymentInfoRow(displayHousingPaymentLabel(payrollSettings.housingPaymentLabel), formatMoney(payrollSettings.housingPayment))
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Расчёт") {
                    PaymentInfoRow("Режим нормы", normModeLabel(payrollSettings.normMode))
                    PaymentInfoRow("Норма часов", formatDouble(payrollSettings.monthlyNormHours))
                    if (runCatching { NormMode.valueOf(payrollSettings.normMode) }.getOrElse { NormMode.MANUAL } != NormMode.MANUAL) {
                        PaymentInfoRow("Часов в рабочем дне", formatDouble(payrollSettings.workdayHours))
                    }
                    if (runCatching { NormMode.valueOf(payrollSettings.normMode) }.getOrElse { NormMode.MANUAL } == NormMode.AVERAGE_ANNUAL) {
                        PaymentInfoRow("Источник среднегодовой нормы", annualNormSourceModeLabel(payrollSettings.annualNormSourceMode))
                        if (runCatching { AnnualNormSourceMode.valueOf(payrollSettings.annualNormSourceMode) }.getOrElse { AnnualNormSourceMode.WORKDAY_HOURS } == AnnualNormSourceMode.YEAR_TOTAL_HOURS) {
                            PaymentInfoRow("Часов в году", formatDouble(payrollSettings.annualNormHours))
                        }
                    }
                    PaymentInfoRow(
                        "Сокращённый день",
                        if (payrollSettings.applyShortDayReduction) "Учитывается" else "Не учитывается"
                    )
                    PaymentInfoRow(
                        "Сверхурочка",
                        if (payrollSettings.overtimeEnabled) "Включена" else "Отключена"
                    )
                    PaymentInfoRow("Период сверхурочки", overtimePeriodLabel(payrollSettings.overtimePeriod))
                    PaymentInfoRow(
                        "Искл. выходные / праздничные",
                        if (payrollSettings.excludeWeekendHolidayFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "Искл. РВД двойная",
                        if (payrollSettings.excludeRvdDoublePayFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "Искл. РВД с отгулом",
                        if (payrollSettings.excludeRvdSingleWithDayOffFromOvertime) "Да" else "Нет"
                    )
                    PaymentInfoRow("Ночные", payrollSettings.nightPercent.toPlainString())
                    PaymentInfoRow("РВД/РВН", payrollSettings.holidayRateMultiplier.toPlainString())
                    PaymentInfoRow("НДФЛ", payrollSettings.ndflPercent.toPlainString())
                    PaymentInfoRow("Начисления за 12 мес. (отпуск)", formatMoney(payrollSettings.vacationAccruals12Months))
                    PaymentInfoRow("Отпуск (ср. день)", formatMoney(payrollSettings.vacationAverageDaily))
                    PaymentInfoRow("Больничный: доход за ${LocalDate.now().year - 2}", formatMoney(payrollSettings.sickIncomeYear1))
                    PaymentInfoRow("Больничный: доход за ${LocalDate.now().year - 1}", formatMoney(payrollSettings.sickIncomeYear2))
                    PaymentInfoRow("Больничный: лимит ${LocalDate.now().year - 2}", formatMoney(payrollSettings.sickLimitYear1))
                    PaymentInfoRow("Больничный: лимит ${LocalDate.now().year - 1}", formatMoney(payrollSettings.sickLimitYear2))
                    PaymentInfoRow("Больничный: дней периода", payrollSettings.sickCalculationPeriodDays.toString())
                    PaymentInfoRow("Больничный: исключаемые дни", payrollSettings.sickExcludedDays.toString())
                    PaymentInfoRow("Больничный (ср. день)", formatMoney(payrollSettings.sickAverageDaily))
                    PaymentInfoRow("Больничный коэффициент", payrollSettings.sickPayPercent.toPlainString())
                    PaymentInfoRow("Макс. больничный в день", formatMoney(payrollSettings.sickMaxDailyAmount))
                    PaymentInfoRow("Прогрессивный НДФЛ", if (payrollSettings.progressiveNdflEnabled) "Включён" else "Выключен")
                    if (payrollSettings.progressiveNdflEnabled) {
                        PaymentInfoRow("Доход с начала года", formatMoney(payrollSettings.taxableIncomeYtdBeforeCurrentMonth))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoCard(title = "Выплаты") {
                    PaymentInfoRow("Режим аванса", advanceModeLabel(payrollSettings.advanceMode))
                    if (runCatching { AdvanceMode.valueOf(payrollSettings.advanceMode) }.getOrElse { AdvanceMode.ACTUAL_EARNINGS } == AdvanceMode.FIXED_PERCENT) {
                        PaymentInfoRow("Процент аванса", formatDouble(payrollSettings.advancePercent) + "%")
                    }
                    PaymentInfoRow("День аванса", payrollSettings.advanceDay.toString())
                    PaymentInfoRow("День зарплаты", payrollSettings.salaryDay.toString())
                    PaymentInfoRow(
                        "Сдвиг выплат",
                        if (payrollSettings.movePaymentsToPreviousWorkday) "На предыдущий рабочий день" else "Без сдвига"
                    )
                    PaymentInfoRow(
                        "${displayHousingPaymentLabel(payrollSettings.housingPaymentLabel)} в аванс",
                        if (payrollSettings.housingPaymentWithAdvance) "Да" else "Нет"
                    )
                    PaymentInfoRow(
                        "${displayHousingPaymentLabel(payrollSettings.housingPaymentLabel)} облагается НДФЛ",
                        if (payrollSettings.housingPaymentTaxable) "Да" else "Нет"
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
fun PatternGrid(
    steps: List<String>,
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>,
    onSetStep: (Int, String) -> Unit
) {
    val templateMap = remember(shiftTemplates) {
        shiftTemplates.associateBy { it.code }
    }

    Column {
        steps.chunked(7).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEachIndexed { columnIndex, stepCode ->
                    val absoluteIndex = rowIndex * 7 + columnIndex
                    val template = templateMap[stepCode]

                    PatternStepCell(
                        index = absoluteIndex + 1,
                        code = stepCode,
                        color = if (template != null) {
                            Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
                        } else {
                            Color(0xFFF2F2F2)
                        },
                        onClick = {
                            if (selectedBrushCode == BRUSH_CLEAR) {
                                onSetStep(absoluteIndex, "")
                            } else {
                                onSetStep(absoluteIndex, selectedBrushCode)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PatternStepCell(
    index: Int,
    code: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (code.isBlank()) Color(0xFFF7F7F7) else color.copy(alpha = 0.28f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = code,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun PatternBrushChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun PatternBrushGrid(
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>,
    onSelect: (String) -> Unit
) {
    val items = listOf(BRUSH_CLEAR) + shiftTemplates.map { it.code }

    items.chunked(4).forEach { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowItems.forEach { code ->
                val template = shiftTemplates.firstOrNull { it.code == code }

                PatternBrushChip(
                    label = if (code == BRUSH_CLEAR) "Очистить" else code,
                    selected = selectedBrushCode == code,
                    color = if (code == BRUSH_CLEAR) {
                        Color(0xFFEF9A9A)
                    } else {
                        Color(parseColorHex(template?.colorHex ?: "#E0E0E0", 0xFFE0E0E0.toInt()))
                    },
                    onClick = { onSelect(code) },
                    modifier = Modifier.weight(1f)
                )
            }

            repeat(4 - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActivePatternBrushCard(
    selectedBrushCode: String,
    shiftTemplates: List<ShiftTemplateEntity>
) {
    val currentTemplate = shiftTemplates.firstOrNull { it.code == selectedBrushCode }

    val title = when {
        selectedBrushCode == BRUSH_CLEAR -> "Очистить"
        currentTemplate != null -> "${currentTemplate.code} — ${currentTemplate.title}"
        else -> selectedBrushCode
    }

    val dotColor = when {
        selectedBrushCode == BRUSH_CLEAR -> Color(0xFFEF9A9A)
        currentTemplate != null -> Color(parseColorHex(currentTemplate.colorHex, 0xFFE0E0E0.toInt()))
        else -> Color(0xFFBDBDBD)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Активный инструмент",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PatternQuickActionsRow(
    onClearAll: () -> Unit,
    onTrimTail: () -> Unit,
    onShiftLeft: () -> Unit,
    onShiftRight: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("Сброс", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onTrimTail,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("Хвост", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onShiftLeft,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("←", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
            onClick = onShiftRight,
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
        ) {
            Text("→", style = MaterialTheme.typography.labelMedium)
        }
    }
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
fun TemplateModeSwitcher(
    mode: TemplateMode,
    onModeChange: (TemplateMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        TemplateModeButton(
            text = "Смены",
            selected = mode == TemplateMode.SHIFTS,
            onClick = { onModeChange(TemplateMode.SHIFTS) },
            modifier = Modifier.weight(1f)
        )

        TemplateModeButton(
            text = "Чередование",
            selected = mode == TemplateMode.CYCLES,
            onClick = { onModeChange(TemplateMode.CYCLES) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TemplateModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.background
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TemplateListItem(
    template: ShiftTemplateEntity,
    specialRule: ShiftSpecialRule?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftTemplateBadge(template = template)

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = shiftTemplateSubtitle(template),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                text = specialShiftRuleLabel(specialRule, template.isWeekendPaid),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun PatternListItem(
    pattern: PatternTemplate,
    onEdit: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pattern.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (pattern.previewText().isBlank()) {
                        "Пустой график"
                    } else {
                        pattern.previewText()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = "Дней в цикле: ${pattern.usedLength()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onApply) {
                Text("Применить")
            }
            TextButton(onClick = onEdit) {
                Text("Изменить")
            }
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

fun readableContentColor(background: Color): Color {
    return if (background.luminance() > 0.55f) {
        Color.Black.copy(alpha = 0.87f)
    } else {
        Color.White
    }
}

@Composable
fun ShiftTemplateBadge(template: ShiftTemplateEntity) {
    val bgColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconGlyph(template.iconKey, template.code),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
@Composable
fun ShiftTemplateEditorScreen(
    currentTemplate: ShiftTemplateEntity?,
    currentSpecialRule: ShiftSpecialRule? = null,
    currentAlarmTemplateConfig: ShiftTemplateAlarmConfig? = null,
    onBack: () -> Unit,
    onSave: (ShiftTemplateEntity, ShiftTemplateAlarmConfig) -> Unit,
    onSaveSpecialRule: (String, ShiftSpecialRule) -> Unit = { _, _ -> },
    onDelete: (ShiftTemplateEntity) -> Unit
) {
    val isEditing = currentTemplate != null
    val isProtectedTemplate = isProtectedSystemTemplate(currentTemplate)

    var titleText by rememberSaveable { mutableStateOf(currentTemplate?.title ?: "") }
    var codeText by rememberSaveable { mutableStateOf(currentTemplate?.code ?: "") }
    var iconKey by rememberSaveable {
        mutableStateOf(currentTemplate?.iconKey?.takeUnless { it.startsWith("EMOJI:") } ?: "TEXT")
    }
    var totalHoursText by rememberSaveable { mutableStateOf(currentTemplate?.totalHours?.toPlainString() ?: "0") }
    var breakHoursText by rememberSaveable { mutableStateOf(currentTemplate?.breakHours?.toPlainString() ?: "0") }
    var nightHoursText by rememberSaveable { mutableStateOf(currentTemplate?.nightHours?.toPlainString() ?: "0") }
    var colorHexText by rememberSaveable { mutableStateOf(currentTemplate?.colorHex ?: "#1E88E5") }
    var specialDayTypeName by rememberSaveable {
        mutableStateOf(currentSpecialRule?.specialDayTypeName ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayTypeName)
    }
    var specialDayCompensationName by rememberSaveable {
        mutableStateOf(currentSpecialRule?.specialDayCompensationName ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false).specialDayCompensationName)
    }
    var active by rememberSaveable { mutableStateOf(currentTemplate?.active ?: true) }
    var sortOrderText by rememberSaveable { mutableStateOf((currentTemplate?.sortOrder ?: 100).toString()) }
    var shiftStartHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startHour ?: 8).toString()) }
    var shiftStartMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.startMinute ?: 0).toString()) }
    var shiftEndHourText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endHour ?: 20).toString()) }
    var shiftEndMinuteText by rememberSaveable { mutableStateOf((currentAlarmTemplateConfig?.endMinute ?: 0).toString()) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showUnsavedExitConfirm by rememberSaveable { mutableStateOf(false) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var emojiText by rememberSaveable {
        mutableStateOf(currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: "")
    }

    val iconOptions = listOf("SUN", "MOON", "EIGHT", "HOME", "OT", "SICK", "STAR", "TEXT")
    val previewIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
    val selectedSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }.getOrElse { SpecialDayType.NONE }
    val selectedSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }.getOrElse { SpecialDayCompensation.NONE }
    val hasSpecialDayRule = selectedSpecialDayType != SpecialDayType.NONE

    val normalizedCurrentColor = normalizeHexColor(currentTemplate?.colorHex ?: "#1E88E5")
    val normalizedEditedColor = normalizeHexColor(colorHexText)
    val originalEmoji = currentTemplate?.iconKey?.takeIf { it.startsWith("EMOJI:") }?.removePrefix("EMOJI:") ?: ""
    val originalIconKey = currentTemplate?.iconKey?.takeUnless { it.startsWith("EMOJI:") } ?: "TEXT"
    val originalSpecialRule = currentSpecialRule ?: defaultShiftSpecialRule(currentTemplate?.isWeekendPaid ?: false)
    val originalAlarmConfig = currentAlarmTemplateConfig ?: currentTemplate?.let { defaultShiftTemplateAlarmConfig(it) }

    val hasUnsavedChanges = remember(
        currentTemplate,
        currentSpecialRule,
        currentAlarmTemplateConfig,
        titleText,
        codeText,
        iconKey,
        emojiText,
        totalHoursText,
        breakHoursText,
        nightHoursText,
        colorHexText,
        specialDayTypeName,
        specialDayCompensationName,
        active,
        sortOrderText,
        shiftStartHourText,
        shiftStartMinuteText,
        shiftEndHourText,
        shiftEndMinuteText
    ) {
        if (currentTemplate == null) {
            titleText.isNotBlank() ||
                    codeText.isNotBlank() ||
                    emojiText.isNotBlank() ||
                    iconKey != "TEXT" ||
                    normalizeHexColor(colorHexText) != "#1E88E5" ||
                    parseDouble(totalHoursText, 0.0) != 0.0 ||
                    parseDouble(breakHoursText, 0.0) != 0.0 ||
                    parseDouble(nightHoursText, 0.0) != 0.0 ||
                    parseInt(sortOrderText, 100) != 100
        } else {
            val codeChanged = codeText.trim().uppercase() != currentTemplate.code
            val titleChanged = titleText.trim() != currentTemplate.title
            val iconChanged = iconKey != originalIconKey
            val emojiChanged = emojiText.trim() != originalEmoji
            val hoursChanged = parseDouble(totalHoursText, currentTemplate.totalHours) != currentTemplate.totalHours
            val breakChanged = parseDouble(breakHoursText, currentTemplate.breakHours) != currentTemplate.breakHours
            val nightChanged = parseDouble(nightHoursText, currentTemplate.nightHours) != currentTemplate.nightHours
            val colorChanged = normalizedEditedColor != normalizedCurrentColor
            val activeChanged = active != currentTemplate.active
            val sortChanged = parseInt(sortOrderText, currentTemplate.sortOrder) != currentTemplate.sortOrder
            val specialTypeChanged = specialDayTypeName != originalSpecialRule.specialDayTypeName
            val specialCompensationChanged = specialDayCompensationName != originalSpecialRule.specialDayCompensationName
            val alarmChanged = originalAlarmConfig != null && (
                    parseInt(shiftStartHourText, originalAlarmConfig.startHour) != originalAlarmConfig.startHour ||
                            parseInt(shiftStartMinuteText, originalAlarmConfig.startMinute) != originalAlarmConfig.startMinute ||
                            parseInt(shiftEndHourText, originalAlarmConfig.endHour) != originalAlarmConfig.endHour ||
                            parseInt(shiftEndMinuteText, originalAlarmConfig.endMinute) != originalAlarmConfig.endMinute
                    )

            if (isProtectedTemplate) {
                colorChanged || activeChanged || specialTypeChanged || specialCompensationChanged || alarmChanged
            } else {
                codeChanged || titleChanged || iconChanged || emojiChanged || hoursChanged || breakChanged ||
                        nightChanged || colorChanged || activeChanged || sortChanged ||
                        specialTypeChanged || specialCompensationChanged || alarmChanged
            }
        }
    }

    fun requestClose() {
        if (hasUnsavedChanges) {
            showUnsavedExitConfirm = true
        } else {
            onBack()
        }
    }

    fun performSave() {
        val finalCode = codeText.trim().uppercase()
        if (finalCode.isBlank()) return

        val finalIconKey = if (emojiText.isNotBlank()) "EMOJI:${emojiText.trim()}" else iconKey
        val effectiveSpecialDayType = runCatching { SpecialDayType.valueOf(specialDayTypeName) }.getOrElse { SpecialDayType.NONE }
        val effectiveSpecialDayCompensation = runCatching { SpecialDayCompensation.valueOf(specialDayCompensationName) }.getOrElse { SpecialDayCompensation.NONE }
        val legacyWeekendPaid = legacyWeekendPaidFlag(
            specialDayType = effectiveSpecialDayType,
            specialDayCompensation = effectiveSpecialDayCompensation
        )

        val savedTemplate = ShiftTemplateEntity(
            code = finalCode,
            title = if (isProtectedTemplate) (currentTemplate?.title ?: finalCode) else titleText.trim().ifBlank { finalCode },
            iconKey = finalIconKey,
            totalHours = if (isProtectedTemplate) (currentTemplate?.totalHours ?: 0.0) else parseDouble(totalHoursText, currentTemplate?.totalHours ?: 0.0),
            breakHours = if (isProtectedTemplate) (currentTemplate?.breakHours ?: 0.0) else parseDouble(breakHoursText, currentTemplate?.breakHours ?: 0.0),
            nightHours = if (isProtectedTemplate) (currentTemplate?.nightHours ?: 0.0) else parseDouble(nightHoursText, currentTemplate?.nightHours ?: 0.0),
            colorHex = normalizeHexColor(colorHexText),
            isWeekendPaid = if (isProtectedTemplate) (currentTemplate?.isWeekendPaid ?: false) else legacyWeekendPaid,
            active = active,
            sortOrder = if (isProtectedTemplate) (currentTemplate?.sortOrder ?: 100) else parseInt(sortOrderText, currentTemplate?.sortOrder ?: 100)
        )

        val alarmTemplateConfig = (currentAlarmTemplateConfig ?: defaultShiftTemplateAlarmConfig(savedTemplate)).copy(
            shiftCode = finalCode,
            startHour = parseInt(shiftStartHourText, currentAlarmTemplateConfig?.startHour ?: 8).coerceIn(0, 23),
            startMinute = parseInt(shiftStartMinuteText, currentAlarmTemplateConfig?.startMinute ?: 0).coerceIn(0, 59),
            endHour = parseInt(shiftEndHourText, currentAlarmTemplateConfig?.endHour ?: 20).coerceIn(0, 23),
            endMinute = parseInt(shiftEndMinuteText, currentAlarmTemplateConfig?.endMinute ?: 0).coerceIn(0, 59)
        )

        onSave(savedTemplate, alarmTemplateConfig)
        onSaveSpecialRule(
            finalCode,
            ShiftSpecialRule(
                specialDayTypeName = if (isProtectedTemplate) (currentSpecialRule?.specialDayTypeName ?: SpecialDayType.NONE.name) else effectiveSpecialDayType.name,
                specialDayCompensationName = if (isProtectedTemplate) (currentSpecialRule?.specialDayCompensationName ?: SpecialDayCompensation.NONE.name) else when (effectiveSpecialDayType) {
                    SpecialDayType.RVD -> effectiveSpecialDayCompensation.name
                    SpecialDayType.WEEKEND_HOLIDAY -> SpecialDayCompensation.DOUBLE_PAY.name
                    SpecialDayType.NONE -> SpecialDayCompensation.NONE.name
                }
            )
        )
    }

    val imeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeaderAction(
                title = if (isEditing) "Смена" else "Новая смена",
                onBack = { requestClose() },
                actionText = "💾",
                onAction = { performSave() },
                actionEnabled = codeText.trim().isNotBlank()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SettingsSectionCard(
                    title = "Основное",
                    subtitle = "Название, код и внешний вид"
                ) {
                    if (!isProtectedTemplate) {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text("Название") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = codeText,
                            onValueChange = { codeText = it.uppercase() },
                            label = { Text("Код") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            singleLine = true
                        )
                    } else {
                        PaymentInfoRow("Название", currentTemplate?.title ?: "")
                        PaymentInfoRow("Код", currentTemplate?.code ?: "")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(text = "Иконка", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Текущий значок", style = MaterialTheme.typography.bodyMedium)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt()))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = iconGlyph(previewIconKey, codeText.ifBlank { "?" }),
                                color = readableContentColor(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt()))),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emojiText,
                        onValueChange = { emojiText = it.take(8) },
                        label = { Text("Эмодзи-значок") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        placeholder = { Text("Например 🚚 или 🛠️") }
                    )

                    Text(
                        text = "Если поле заполнено, будет использоваться эмодзи. Чтобы вернуться к обычной иконке, очисти поле.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    iconOptions.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                IconChoiceButton(
                                    iconKey = item,
                                    codeFallback = codeText.ifBlank { "?" },
                                    selected = emojiText.isBlank() && iconKey == item,
                                    onClick = {
                                        iconKey = item
                                        emojiText = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Цвет", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(parseColorHex(colorHexText, 0xFFE0E0E0.toInt())))
                                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(18.dp))
                        )

                        OutlinedTextField(
                            value = colorHexText,
                            onValueChange = { colorHexText = normalizeHexColor(it) },
                            label = { Text("HEX") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showColorPickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть палитру")
                    }
                }

                if (!isProtectedTemplate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionCard(
                        title = "Расчёт",
                        subtitle = "Часы, обед, ночные"
                    ) {
                        PayrollNumberField(label = "Всего часов", value = totalHoursText, onValueChange = { totalHoursText = it })
                        PayrollNumberField(label = "Неоплачиваемый обед, часов", value = breakHoursText, onValueChange = { breakHoursText = it })
                        PayrollNumberField(label = "Ночные часы", value = nightHoursText, onValueChange = { nightHoursText = it })
                        PayrollIntField(label = "Порядок сортировки", value = sortOrderText, onValueChange = { sortOrderText = it })

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Время смены", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactIntField(label = "Начало, ч", value = shiftStartHourText, onValueChange = { shiftStartHourText = it }, modifier = Modifier.weight(1f))
                            CompactIntField(label = "Начало, мин", value = shiftStartMinuteText, onValueChange = { shiftStartMinuteText = it }, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactIntField(label = "Конец, ч", value = shiftEndHourText, onValueChange = { shiftEndHourText = it }, modifier = Modifier.weight(1f))
                            CompactIntField(label = "Конец, мин", value = shiftEndMinuteText, onValueChange = { shiftEndMinuteText = it }, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        CompactSwitchRow(
                            title = "Работа в выходной / праздник",
                            checked = hasSpecialDayRule,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name
                                    specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                } else {
                                    specialDayTypeName = SpecialDayType.NONE.name
                                    specialDayCompensationName = SpecialDayCompensation.NONE.name
                                }
                            }
                        )

                        if (hasSpecialDayRule) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Тип дня", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                PayModeChoiceCard(
                                    title = "Выходная / праздничная",
                                    subtitle = "Обычная работа в выходной или праздник с повышенной оплатой",
                                    selected = selectedSpecialDayType == SpecialDayType.WEEKEND_HOLIDAY,
                                    onClick = {
                                        specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name
                                        specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                PayModeChoiceCard(
                                    title = "РВД",
                                    subtitle = "Работа в выходной день по распоряжению работодателя",
                                    selected = selectedSpecialDayType == SpecialDayType.RVD,
                                    onClick = {
                                        specialDayTypeName = SpecialDayType.RVD.name
                                        if (selectedSpecialDayCompensation == SpecialDayCompensation.NONE) {
                                            specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
                                        }
                                    }
                                )
                            }

                            if (selectedSpecialDayType == SpecialDayType.RVD) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Компенсация РВД", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(8.dp)
                                ) {
                                    PayModeChoiceCard(
                                        title = "Двойная оплата",
                                        subtitle = "Часы не требуют отдельного отгула",
                                        selected = selectedSpecialDayCompensation == SpecialDayCompensation.DOUBLE_PAY,
                                        onClick = { specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    PayModeChoiceCard(
                                        title = "Одинарная оплата + отгул",
                                        subtitle = "Повышенная доплата не начисляется, сверхурочка настраивается отдельно",
                                        selected = selectedSpecialDayCompensation == SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF,
                                        onClick = { specialDayCompensationName = SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF.name }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            PaymentInfoRow(
                                label = "Режим дня",
                                value = specialShiftRuleLabel(
                                    ShiftSpecialRule(
                                        specialDayTypeName = specialDayTypeName,
                                        specialDayCompensationName = specialDayCompensationName
                                    ),
                                    fallbackWeekendPaid = false
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        CompactSwitchRow(title = "Активна", checked = active, onCheckedChange = { active = it })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Оплачиваемые часы: ${formatDouble(max(0.0, parseDouble(totalHoursText, 0.0) - parseDouble(breakHoursText, 0.0)))}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isEditing && !isProtectedTemplate && !imeVisible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить шаблон")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showColorPickerDialog) {
        Dialog(
            onDismissRequest = { showColorPickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Цвет смены",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FullColorPicker(
                        selectedColorHex = colorHexText,
                        onColorSelected = { colorHexText = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showColorPickerDialog = false }) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && currentTemplate != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить шаблон?") },
            text = { Text("Шаблон будет удалён. Дни в календаре с этим кодом тоже очистятся.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(currentTemplate)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }

    if (showUnsavedExitConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitConfirm = false },
            title = { Text("Сохранить изменения?") },
            text = { Text("В шаблоне есть несохранённые изменения.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedExitConfirm = false
                    performSave()
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedExitConfirm = false
                        onBack()
                    }) {
                        Text("Не сохранять")
                    }
                    TextButton(onClick = { showUnsavedExitConfirm = false }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }
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


fun extraSalaryModeLabel(extraSalaryModeName: String): String {
    return when (runCatching { ExtraSalaryMode.valueOf(extraSalaryModeName) }.getOrElse { ExtraSalaryMode.INCLUDED_IN_RATE }) {
        ExtraSalaryMode.INCLUDED_IN_RATE -> "Включена в часовую ставку"
        ExtraSalaryMode.FIXED_MONTHLY -> "Фиксированная месячная"
    }
}

fun advanceModeLabel(advanceModeName: String): String {
    return when (runCatching { AdvanceMode.valueOf(advanceModeName) }.getOrElse { AdvanceMode.ACTUAL_EARNINGS }) {
        AdvanceMode.ACTUAL_EARNINGS -> "По фактически начисленному"
        AdvanceMode.FIXED_PERCENT -> "Фиксированный процент"
    }
}

fun payModeLabel(payModeName: String): String {
    return when (runCatching { PayMode.valueOf(payModeName) }.getOrElse { PayMode.HOURLY }) {
        PayMode.HOURLY -> "Почасовая"
        PayMode.MONTHLY_SALARY -> "Помесячная по окладу"
    }
}


fun normModeLabel(normModeName: String): String {
    return when (runCatching { NormMode.valueOf(normModeName) }.getOrElse { NormMode.MANUAL }) {
        NormMode.MANUAL -> "Ручная"
        NormMode.PRODUCTION_CALENDAR -> "По производственному календарю"
        NormMode.AVERAGE_ANNUAL -> "Среднегодовая"
        NormMode.AVERAGE_QUARTERLY -> "Среднеквартальная"
    }
}

fun annualNormSourceModeLabel(modeName: String): String {
    return when (
        runCatching { AnnualNormSourceMode.valueOf(modeName) }
            .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    ) {
        AnnualNormSourceMode.WORKDAY_HOURS -> "По часам в рабочем дне"
        AnnualNormSourceMode.YEAR_TOTAL_HOURS -> "По общему количеству часов в году"
    }
}

fun iconGlyph(iconKey: String, fallbackCode: String): String {
    return when {
        iconKey.startsWith("EMOJI:") -> {
            iconKey.removePrefix("EMOJI:").ifBlank { fallbackCode }
        }

        iconKey == "SUN" -> "☀"
        iconKey == "MOON" -> "☾"
        iconKey == "EIGHT" -> "8"
        iconKey == "HOME" -> "⌂"
        iconKey == "OT" -> "ОТ"
        iconKey == "SICK" -> "✚"
        iconKey == "STAR" -> "★"
        iconKey == "TEXT" -> fallbackCode
        else -> fallbackCode
    }
}
fun shiftGlyphFontSize(glyph: String) = when {
    glyph.length <= 2 -> 18
    glyph.length == 3 -> 14
    else -> 12
}
data class OvertimePeriodInfo(
    val label: String,
    val startMonth: YearMonth,
    val endMonth: YearMonth
) {
    val startDate: LocalDate get() = startMonth.atDay(1)
    val endDate: LocalDate get() = endMonth.atEndOfMonth()
}

fun defaultShiftSpecialRule(fallbackWeekendPaid: Boolean): ShiftSpecialRule {
    return if (fallbackWeekendPaid) {
        ShiftSpecialRule(
            specialDayTypeName = SpecialDayType.WEEKEND_HOLIDAY.name,
            specialDayCompensationName = SpecialDayCompensation.DOUBLE_PAY.name
        )
    } else {
        ShiftSpecialRule(
            specialDayTypeName = SpecialDayType.NONE.name,
            specialDayCompensationName = SpecialDayCompensation.NONE.name
        )
    }
}

fun readShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String,
    fallbackWeekendPaid: Boolean
): ShiftSpecialRule {
    val fallback = defaultShiftSpecialRule(fallbackWeekendPaid)
    return ShiftSpecialRule(
        specialDayTypeName = prefs.getString("special_day_type_$code", fallback.specialDayTypeName) ?: fallback.specialDayTypeName,
        specialDayCompensationName = prefs.getString("special_day_compensation_$code", fallback.specialDayCompensationName)
            ?: fallback.specialDayCompensationName
    )
}

fun writeShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String,
    rule: ShiftSpecialRule
) {
    prefs.edit()
        .putString("special_day_type_$code", rule.specialDayTypeName)
        .putString("special_day_compensation_$code", rule.specialDayCompensationName)
        .apply()
}

fun deleteShiftSpecialRule(
    prefs: android.content.SharedPreferences,
    code: String
) {
    prefs.edit()
        .remove("special_day_type_$code")
        .remove("special_day_compensation_$code")
        .apply()
}

fun resolveSpecialDayType(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): SpecialDayType {
    return runCatching {
        SpecialDayType.valueOf(rule?.specialDayTypeName ?: defaultShiftSpecialRule(fallbackWeekendPaid).specialDayTypeName)
    }.getOrElse {
        if (fallbackWeekendPaid) SpecialDayType.WEEKEND_HOLIDAY else SpecialDayType.NONE
    }
}

fun resolveSpecialDayCompensation(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): SpecialDayCompensation {
    return runCatching {
        SpecialDayCompensation.valueOf(
            rule?.specialDayCompensationName ?: defaultShiftSpecialRule(fallbackWeekendPaid).specialDayCompensationName
        )
    }.getOrElse {
        if (fallbackWeekendPaid) SpecialDayCompensation.DOUBLE_PAY else SpecialDayCompensation.NONE
    }
}

fun legacyWeekendPaidFlag(
    specialDayType: SpecialDayType,
    specialDayCompensation: SpecialDayCompensation
): Boolean {
    return when (specialDayType) {
        SpecialDayType.NONE -> false
        SpecialDayType.WEEKEND_HOLIDAY -> true
        SpecialDayType.RVD -> specialDayCompensation == SpecialDayCompensation.DOUBLE_PAY
    }
}

fun specialShiftRuleLabel(
    rule: ShiftSpecialRule?,
    fallbackWeekendPaid: Boolean
): String {
    val specialDayType = resolveSpecialDayType(rule, fallbackWeekendPaid)
    val specialDayCompensation = resolveSpecialDayCompensation(rule, fallbackWeekendPaid)
    return when (specialDayType) {
        SpecialDayType.NONE -> "Обычная"
        SpecialDayType.WEEKEND_HOLIDAY -> "Выходная / праздничная"
        SpecialDayType.RVD -> when (specialDayCompensation) {
            SpecialDayCompensation.DOUBLE_PAY -> "РВД • двойная оплата"
            SpecialDayCompensation.SINGLE_PAY_WITH_DAY_OFF -> "РВД • одинарная + отгул"
            SpecialDayCompensation.NONE -> "РВД"
        }
    }
}

fun overtimePeriodLabel(overtimePeriodName: String): String {
    return when (runCatching { OvertimePeriod.valueOf(overtimePeriodName) }.getOrElse { OvertimePeriod.YEAR }) {
        OvertimePeriod.MONTH -> "Месяц"
        OvertimePeriod.QUARTER -> "Квартал"
        OvertimePeriod.HALF_YEAR -> "Полугодие"
        OvertimePeriod.YEAR -> "Год"
    }
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

fun shiftCellColor(
    shiftCode: String?,
    shiftColors: Map<String, Int>,
    templateMap: Map<String, ShiftTemplateEntity>
): Color {
    if (shiftCode == null) {
        val emptyColor = shiftColors[KEY_EMPTY_DAY]
            ?: defaultShiftColors()[KEY_EMPTY_DAY]
            ?: 0xFFF5F5F5.toInt()
        return Color(emptyColor)
    }

    val templateColor = templateMap[shiftCode]?.colorHex
    val fallback = parseColorHex(templateColor ?: "#E0E0E0", 0xFFE0E0E0.toInt())
    val colorValue = shiftColors[shiftCode]
        ?: fallback
        ?: defaultShiftColors()[shiftCode]
        ?: 0xFFE0E0E0.toInt()

    return Color(colorValue)
}

fun defaultShiftColors(): Map<String, Int> {
    return mapOf(
        "Д" to 0xFFBBDEFB.toInt(),
        "Н" to 0xFFD1C4E9.toInt(),
        "РВД" to 0xFFFFE0B2.toInt(),
        "РВН" to 0xFFFFCDD2.toInt(),
        "8" to 0xFFC8E6C9.toInt(),
        "ОТ" to 0xFFFFF9C4.toInt(),
        "Б" to 0xFFF8BBD0.toInt(),
        "ВЫХ" to 0xFFE0E0E0.toInt(),
        KEY_EMPTY_DAY to 0xFFF5F5F5.toInt()
    )
}

fun shiftEditorPalette(): List<Int> {
    return listOf(
        0xFFBBDEFB.toInt(),
        0xFF90CAF9.toInt(),
        0xFFD1C4E9.toInt(),
        0xFFB39DDB.toInt(),
        0xFFFFE0B2.toInt(),
        0xFFFFCC80.toInt(),
        0xFFFFCDD2.toInt(),
        0xFFEF9A9A.toInt(),
        0xFFC8E6C9.toInt(),
        0xFFA5D6A7.toInt(),
        0xFFFFF9C4.toInt(),
        0xFFFFF59D.toInt(),
        0xFFF8BBD0.toInt(),
        0xFFF48FB1.toInt(),
        0xFFE0E0E0.toInt(),
        0xFFBDBDBD.toInt(),
        0xFFB2DFDB.toInt(),
        0xFF80CBC4.toInt(),
        0xFFFFE082.toInt(),
        0xFFFFCCBC.toInt()
    )
}
fun parseDouble(text: String, fallback: Double): Double {
    return text.replace(',', '.').toDoubleOrNull() ?: fallback
}

fun parseInt(text: String, fallback: Int): Int {
    return text.filter { it.isDigit() }.toIntOrNull() ?: fallback
}

fun normalizeHexColor(input: String): String {
    val cleaned = input.trim().uppercase()
    return when {
        cleaned.matches(Regex("^#[0-9A-F]{6}$")) -> cleaned
        cleaned.matches(Regex("^[0-9A-F]{6}$")) -> "#$cleaned"
        else -> "#E0E0E0"
    }
}

fun parseColorHex(input: String, fallback: Int): Int {
    return try {
        android.graphics.Color.parseColor(normalizeHexColor(input))
    } catch (_: Exception) {
        fallback
    }
}

fun colorIntToHex(colorInt: Int): String {
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

fun hexToHsv(colorHex: String): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        parseColorHex(colorHex, 0xFF1E88E5.toInt()),
        hsv
    )
    return hsv
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

@Composable
fun MonthHolidayInfoCard(
    holidayEntries: List<Map.Entry<LocalDate, HolidayEntity>>
) {
    if (holidayEntries.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Праздничные и особые дни месяца",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        holidayEntries.forEachIndexed { index, entry ->
            val date = entry.key
            val holiday = entry.value

            val kindLabel = when (holiday.kind) {
                HolidayKinds.SHORT_DAY -> "Сокращённый день"
                HolidayKinds.TRANSFERRED_DAY_OFF -> "Перенесённый выходной"
                else -> "Нерабочий праздничный день"
            }

            val scopeLabel = when (holiday.scopeCode) {
                "RU-FED" -> "Фед."
                MANUAL_HOLIDAY_SCOPE -> "Ручн."
                else -> holiday.scopeCode
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatDate(date),
                    modifier = Modifier.width(86.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = holiday.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = kindLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFD32F2F).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = scopeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (index != holidayEntries.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
fun CompactScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    AppScreenHeader(
        title = title,
        onBack = onBack
    )
}
fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}


private fun calendarSyncSuccessKey(year: Int): String = "federal_year_${year}_success_at"

private fun calendarSyncFingerprintKey(year: Int): String = "federal_year_${year}_fingerprint"

private fun calendarSyncLastCheckKey(year: Int): String = "federal_year_${year}_last_check_at"

private suspend fun checkAndSyncFederalCalendarIfChanged(
    holidaySyncRepository: HolidaySyncRepository,
    prefs: android.content.SharedPreferences,
    year: Int,
    hasLocalYear: Boolean,
    forceNetworkCheck: Boolean
): CalendarSyncCheckResult {
    val successKey = calendarSyncSuccessKey(year)
    val fingerprintKey = calendarSyncFingerprintKey(year)
    val lastCheckKey = calendarSyncLastCheckKey(year)
    val now = System.currentTimeMillis()
    val lastSuccessAt = prefs.getLong(successKey, 0L)
    val lastCheckAt = prefs.getLong(lastCheckKey, 0L)
    val savedFingerprint = prefs.getString(fingerprintKey, null)

    if (hasLocalYear && !forceNetworkCheck && lastCheckAt > 0L && now - lastCheckAt < CALENDAR_AUTO_CHECK_INTERVAL_MS) {
        return CalendarSyncCheckResult(
            updated = false,
            changedOnServer = false,
            dayCount = 0,
            message = if (lastSuccessAt > 0L) {
                "Используется локальный календарь ${year}. Последняя проверка: ${formatCalendarSyncMoment(lastCheckAt)}"
            } else {
                "Используется локальный календарь ${year}"
            }
        )
    }

    val latestFingerprint = fetchFederalCalendarFingerprint(year)

    if (hasLocalYear && savedFingerprint != null && latestFingerprint == savedFingerprint) {
        prefs.edit()
            .putLong(lastCheckKey, now)
            .apply()

        return CalendarSyncCheckResult(
            updated = false,
            changedOnServer = false,
            dayCount = 0,
            message = "Изменений для календаря ${year} не найдено. Проверено: ${formatCalendarSyncMoment(now)}"
        )
    }

    val syncedCount = holidaySyncRepository.syncFederalYear(year)
    prefs.edit()
        .putLong(successKey, now)
        .putLong(lastCheckKey, now)
        .putString(fingerprintKey, latestFingerprint)
        .apply()

    return CalendarSyncCheckResult(
        updated = true,
        changedOnServer = hasLocalYear,
        dayCount = syncedCount,
        message = when {
            !hasLocalYear -> "Календарь ${year} загружен и сохранён локально. Дней: ${syncedCount}"
            savedFingerprint == null -> "Календарь ${year} проверен и обновлён. Дней: ${syncedCount}"
            else -> "Найдены изменения на сервере. Календарь ${year} обновлён. Дней: ${syncedCount}"
        }
    )
}

private suspend fun fetchFederalCalendarFingerprint(year: Int): String = withContext(Dispatchers.IO) {
    sha256(loadFederalCalendarRawText(year).trim())
}

private fun loadFederalCalendarRawText(year: Int): String {
    val urlString = "https://calendar.kuzyak.in/api/calendar/${year}/holidays"
    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "ShiftSalaryPlanner/1.0")
    }

    return try {
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorText = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            throw IllegalStateException("HTTP ${code}${if (errorText.isNotBlank()) ": ${errorText}" else ""}")
        }

        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun sha256(text: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun formatCalendarSyncMoment(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    return java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
}

private fun readManualHolidayRecords(prefs: android.content.SharedPreferences): List<ManualHolidayRecord> {
    return prefs.getStringSet("items", emptySet())
        ?.mapNotNull { serialized ->
            parseManualHolidayRecord(serialized)
        }
        ?.sortedBy { it.date }
        ?: emptyList()
}

private fun writeManualHolidayRecords(
    prefs: android.content.SharedPreferences,
    records: List<ManualHolidayRecord>
) {
    prefs.edit()
        .putStringSet(
            "items",
            records.map { serializeManualHolidayRecord(it) }.toSet()
        )
        .apply()
}

private fun serializeManualHolidayRecord(record: ManualHolidayRecord): String {
    val safeTitle = record.title.replace(MANUAL_HOLIDAY_SEPARATOR, " ").replace("\n", " ").trim()
    val nonWorkingFlag = if (record.isNonWorking) "1" else "0"
    return listOf(record.date, safeTitle, record.kind, nonWorkingFlag)
        .joinToString(MANUAL_HOLIDAY_SEPARATOR)
}

private fun parseManualHolidayRecord(serialized: String): ManualHolidayRecord? {
    val parts = serialized.split(MANUAL_HOLIDAY_SEPARATOR)
    if (parts.size < 4) return null

    return runCatching {
        LocalDate.parse(parts[0])
        ManualHolidayRecord(
            date = parts[0],
            title = parts[1],
            kind = parts[2],
            isNonWorking = parts[3] == "1"
        )
    }.getOrNull()
}

private fun ManualHolidayRecord.toHolidayEntity(): HolidayEntity {
    return HolidayEntity(
        id = "MANUAL|$date|$kind",
        date = date,
        title = title,
        scopeCode = MANUAL_HOLIDAY_SCOPE,
        kind = kind,
        isNonWorking = isNonWorking
    )
}

fun fixedFederalHolidayMap(year: Int): Map<LocalDate, HolidayEntity> {
    val items = mutableListOf<HolidayEntity>()

    for (day in 1..8) {
        val date = LocalDate.of(year, 1, day)
        val title = if (day == 7) "Рождество Христово" else "Новогодние каникулы"
        items += HolidayEntity(
            id = "RU-FED-FIXED|${date}|${HolidayKinds.HOLIDAY}",
            date = date.toString(),
            title = title,
            scopeCode = "RU-FED",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )
    }

    listOf(
        LocalDate.of(year, 2, 23) to "День защитника Отечества",
        LocalDate.of(year, 3, 8) to "Международный женский день",
        LocalDate.of(year, 5, 1) to "Праздник Весны и Труда",
        LocalDate.of(year, 5, 9) to "День Победы",
        LocalDate.of(year, 6, 12) to "День России",
        LocalDate.of(year, 11, 4) to "День народного единства"
    ).forEach { (date, title) ->
        items += HolidayEntity(
            id = "RU-FED-FIXED|${date}|${HolidayKinds.HOLIDAY}",
            date = date.toString(),
            title = title,
            scopeCode = "RU-FED",
            kind = HolidayKinds.HOLIDAY,
            isNonWorking = true
        )
    }

    return items.associateBy { LocalDate.parse(it.date) }
}

fun isCalendarDayOff(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>
): Boolean {
    return isWeekendDay(date) || holidayMap.containsKey(date)
}

fun isWeekendDay(date: LocalDate): Boolean {
    return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}

fun isStatutoryHoliday(
    date: LocalDate,
    holidayMap: Map<LocalDate, HolidayEntity>
): Boolean {
    return holidayMap[date]?.kind == com.vigilante.shiftsalaryplanner.data.HolidayKinds.HOLIDAY
}

suspend fun applyPatternToMonth(
    shiftDayDao: ShiftDayDao,
    pattern: PatternTemplate,
    cycleStartDate: LocalDate,
    month: YearMonth,
    validShiftCodes: Set<String>
) {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())

    if (cycle.isEmpty()) return

    var date = month.atDay(1)
    val endDate = month.atEndOfMonth()

    while (!date.isAfter(endDate)) {
        val diffDays = ChronoUnit.DAYS.between(cycleStartDate, date).toInt()
        val cycleIndex = ((diffDays % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        if (code.isBlank()) {
            shiftDayDao.deleteByDate(date.toString())
        } else if (validShiftCodes.contains(code)) {
            shiftDayDao.upsert(
                ShiftDayEntity(
                    date = date.toString(),
                    shiftCode = code
                )
            )
        }

        date = date.plusDays(1)
    }
}
suspend fun applyPatternToRange(
    shiftDayDao: ShiftDayDao,
    pattern: PatternTemplate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    validShiftCodes: Set<String>,
    phaseOffset: Int = 0
) {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())
    if (cycle.isEmpty()) return

    var date = rangeStart
    while (!date.isAfter(rangeEnd)) {
        val diffDays = ChronoUnit.DAYS.between(rangeStart, date).toInt()
        val rawIndex = diffDays + phaseOffset
        val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        if (code.isBlank()) {
            shiftDayDao.deleteByDate(date.toString())
        } else if (validShiftCodes.contains(code)) {
            shiftDayDao.upsert(
                ShiftDayEntity(
                    date = date.toString(),
                    shiftCode = code
                )
            )
        }

        date = date.plusDays(1)
    }
}
fun buildPatternPreviewRows(
    pattern: PatternTemplate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    phaseOffset: Int,
    maxItems: Int
): List<Pair<LocalDate, String>> {
    val cycle = pattern.normalizedSteps().take(pattern.usedLength())
    if (cycle.isEmpty()) return emptyList()

    val result = mutableListOf<Pair<LocalDate, String>>()
    var date = rangeStart

    while (!date.isAfter(rangeEnd) && result.size < maxItems) {
        val diffDays = ChronoUnit.DAYS.between(rangeStart, date).toInt()
        val rawIndex = diffDays + phaseOffset
        val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
        val code = cycle[cycleIndex]

        result.add(date to code)
        date = date.plusDays(1)
    }

    return result
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
private const val DEBUG_HEADER_MARKER = "COMPACT_V2"