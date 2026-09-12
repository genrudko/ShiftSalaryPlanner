@file:Suppress(
    "DEPRECATION",
    "UNUSED_VALUE",
    "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE"
)

package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.vigilante.shiftsalaryplanner.data.DefaultShiftTemplates
import com.vigilante.shiftsalaryplanner.data.FederalHolidaySeed
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.excel.ExcelImportParseResult
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.resolvePayrollSettingsWorkplaceId
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetDraftFactory
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculatePaymentDates
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppEventLogStore
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettingsStore
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.AssistantAiSettings
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncMeta
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryItem
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryStore
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettings
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettingsStore
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsState
import com.vigilante.shiftsalaryplanner.settings.defaultWorkplaces
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.ui.theme.AppColorSchemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppFontMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppSectionTypography
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceFontSection
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.CalendarDefaultWorkplaceMode
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.fontModeForSection
import com.vigilante.shiftsalaryplanner.widget.EXTRA_OPEN_TAB
import com.vigilante.shiftsalaryplanner.widget.PREFS_WIDGET_SETTINGS
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProviderV2
import com.vigilante.shiftsalaryplanner.widget.clearWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetDisplaySettings
import com.vigilante.shiftsalaryplanner.widget.writeWidgetThemeMode
import com.vigilante.shiftsalaryplanner.wearsync.WearSyncBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialRawTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        val initialNavigationState = initialAppNavigationState(initialRawTab)
        intent?.removeExtra(EXTRA_OPEN_TAB)
        setContent {
            ShiftSalaryPlannerRoot(
                initialNavigationState = initialNavigationState
            )
        }
    }
}

private fun ShiftDayEntity.hasIndividualShiftOverride(): Boolean =
    !overrideStartTime.isNullOrBlank() ||
        !overrideEndTime.isNullOrBlank() ||
        overrideTotalHours != null ||
        overrideBreakHours != null ||
        overrideNightHours != null ||
        overridePaidHours != null ||
        overrideShiftPayAmount != null ||
        !overrideNote.isNullOrBlank()

private fun BottomTab.appearanceFontSection(): AppearanceFontSection {
    return when (this) {
        BottomTab.CALENDAR -> AppearanceFontSection.CALENDAR
        BottomTab.TODAY -> AppearanceFontSection.TODAY
        BottomTab.ASSISTANT -> AppearanceFontSection.ASSISTANT
        BottomTab.NOTES -> AppearanceFontSection.NOTES
        BottomTab.FINANCE -> AppearanceFontSection.FINANCE
        BottomTab.ALARMS -> AppearanceFontSection.ALARMS
        BottomTab.SHIFTS -> AppearanceFontSection.SHIFTS
        BottomTab.SETTINGS -> AppearanceFontSection.SETTINGS
    }
}

private fun formatBackupTimestamp(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "ещё не было"
    val dateTime = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return "${formatDate(dateTime.toLocalDate())} ${formatClockHm(dateTime.hour, dateTime.minute)}"
}

private fun formatYearMonthLabel(month: YearMonth): String {
    return "${month.monthValue.toString().padStart(2, '0')}.${month.year}"
}

private fun currentAppVersionCode(context: Context): Long {
    return runCatching {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }.getOrDefault(0L)
}

private fun buildCompactDayWorkSummary(
    date: LocalDate,
    assignmentsByDate: Map<LocalDate, List<CalendarDayAssignment>>,
    templateMap: Map<String, ShiftTemplateEntity>,
    workplaces: List<Workplace>
): String {
    val workplaceNames = workplaces.associate { it.id to it.name }
    val assignments = assignmentsByDate[date].orEmpty()
    if (assignments.isEmpty()) return "смен нет"
    return assignments.joinToString(" • ") { assignment ->
        val template = templateMap[assignment.shiftCode]
        val workplace = workplaceNames[assignment.workplaceId].orEmpty()
        val title = template?.title?.takeIf { it.isNotBlank() } ?: stripWorkplaceScopeFromShiftCode(assignment.shiftCode)
        val hours = template?.totalHours?.minus(template.breakHours)?.coerceAtLeast(0.0)
        buildString {
            if (workplace.isNotBlank()) {
                append(workplace)
                append(": ")
            }
            append(title)
            if (hours != null && hours > 0.0) {
                append(" · ")
                append(formatHours(hours))
            }
        }
    }
}

private fun buildTodayShiftPreviews(
    date: LocalDate,
    assignmentsByDate: Map<LocalDate, List<CalendarDayAssignment>>,
    templateMap: Map<String, ShiftTemplateEntity>,
    workplaces: List<Workplace>,
    templateAlarmConfigs: Map<String, ShiftTemplateAlarmConfig>
): List<TodayShiftPreview> {
    val workplaceNames = workplaces.associate { it.id to it.name }
    return assignmentsByDate[date].orEmpty().map { assignment ->
        val template = templateMap[assignment.shiftCode]
        val displayCode = stripWorkplaceScopeFromShiftCode(assignment.shiftCode)
        val timing = templateAlarmConfigs[assignment.shiftCode]
        val timeLabel = timing?.let { config ->
            "${formatClockHm(config.startHour, config.startMinute)}-${formatClockHm(config.endHour, config.endMinute)}"
        } ?: "время не задано"
        TodayShiftPreview(
            title = template?.title?.takeIf { it.isNotBlank() } ?: displayCode,
            workplace = workplaceNames[assignment.workplaceId].orEmpty(),
            code = displayCode,
            iconKey = template?.iconKey.orEmpty(),
            badgeColor = Color(parseColorHex(template?.colorHex ?: "#1E88E5", 0xFF1E88E5.toInt())).toArgb(),
            timeLabel = timeLabel,
            hoursLabel = template?.let { "${formatHours(it.paidHours())} ч" } ?: "часы не заданы"
        )
    }
}

private fun buildUpcomingPaymentItems(
    today: LocalDate,
    anchorMonth: YearMonth,
    settings: com.vigilante.shiftsalaryplanner.payroll.PayrollSettings,
    extraDayOffDates: Set<LocalDate>,
    currentPayroll: com.vigilante.shiftsalaryplanner.payroll.PayrollResult,
    shiftCodesByDate: Map<LocalDate, List<String>>,
    templateMap: Map<String, ShiftTemplateEntity>
): List<UpcomingPaymentItem> {
    val scheduleMode = runCatching {
        com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.valueOf(settings.paymentScheduleMode)
    }.getOrElse { com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.TWICE_MONTHLY }

    if (scheduleMode == com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.PER_SHIFT) {
        return shiftCodesByDate
            .asSequence()
            .filter { (date, codes) -> !date.isBefore(today) && codes.isNotEmpty() }
            .sortedBy { (date, _) -> date }
            .flatMap { (date, codes) ->
                codes.distinct().asSequence().map { code ->
                    val template = templateMap[code]
                    UpcomingPaymentItem(
                        title = "После смены ${stripWorkplaceScopeFromShiftCode(code)}",
                        periodLabel = template?.title?.takeIf { it.isNotBlank() }
                            ?: formatYearMonthLabel(YearMonth.from(date)),
                        date = date,
                        amount = template?.shiftPayAmount?.takeIf { it > 0.0 }
                    )
                }
            }
            .take(12)
            .toList()
    }

    return (-1..3)
        .flatMap { offset ->
            val month = YearMonth.from(today).plusMonths(offset.toLong())
            val dates = calculatePaymentDates(
                month = month,
                settings = settings,
                extraDayOffDates = extraDayOffDates
            )
            when (scheduleMode) {
                com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.ONCE_MONTHLY -> listOf(
                    UpcomingPaymentItem(
                        title = "Выплата",
                        periodLabel = formatYearMonthLabel(month),
                        date = dates.salaryDate,
                        amount = currentPayroll.netSalaryAfterDeductions.takeIf { month == anchorMonth }
                    )
                )
                com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.TWICE_MONTHLY -> listOf(
                    UpcomingPaymentItem(
                        title = "Аванс",
                        periodLabel = formatYearMonthLabel(month),
                        date = dates.advanceDate,
                        amount = currentPayroll.netAdvanceAfterDeductions.takeIf { month == anchorMonth }
                    ),
                    UpcomingPaymentItem(
                        title = "Зарплата",
                        periodLabel = formatYearMonthLabel(month),
                        date = dates.salaryDate,
                        amount = currentPayroll.netSalaryAfterDeductions.takeIf { month == anchorMonth }
                    )
                )
                com.vigilante.shiftsalaryplanner.payroll.PaymentScheduleMode.PER_SHIFT -> emptyList()
            }
        }
        .filter { item -> !item.date.isBefore(today) }
        .distinctBy { item -> "${item.title}_${item.periodLabel}_${item.date}" }
        .sortedBy { item -> item.date }
}

private const val PAYROLL_WORKPLACE_ALL_ID = "__all_workplaces__"
private const val KEY_WORKPLACE_TEMPLATE_SEEDED_IDS = "workplace_template_seeded_ids"

private fun normalizeWorkplaceId(value: String): String {
    return value.trim().ifBlank { WORKPLACE_MAIN_ID }
}

private fun belongsToWorkplace(value: String, workplaceId: String): Boolean {
    return normalizeWorkplaceId(value) == workplaceId
}

private fun readSeededWorkplaceTemplateIds(
    prefs: SharedPreferences
): MutableSet<String> {
    return prefs.getStringSet(KEY_WORKPLACE_TEMPLATE_SEEDED_IDS, emptySet())
        ?.toMutableSet()
        ?: mutableSetOf()
}

private fun markWorkplaceTemplatesSeeded(
    prefs: SharedPreferences,
    workplaceId: String
) {
    val normalizedId = workplaceId.trim()
    if (normalizedId.isBlank() || normalizedId == WORKPLACE_MAIN_ID) return
    val seeded = readSeededWorkplaceTemplateIds(prefs)
    if (!seeded.add(normalizedId)) return
    prefs.edit {
        putStringSet(KEY_WORKPLACE_TEMPLATE_SEEDED_IDS, seeded)
    }
}

private fun appearanceSettingsSummary(settings: AppearanceSettings): String {
    val themeLabel = when (settings.themeMode) {
        ThemeMode.LIGHT -> "светлая"
        ThemeMode.DARK -> "тёмная"
        ThemeMode.AUTO -> "авто"
        ThemeMode.SCHEDULE -> "по расписанию"
    }

    val paletteLabel = when (settings.colorSchemeMode) {
        AppColorSchemeMode.MINT -> "Mint"
        AppColorSchemeMode.OCEAN -> "Ocean"
        AppColorSchemeMode.SUNSET -> "Sunset"
        AppColorSchemeMode.GRAPHITE -> "Graphite"
        AppColorSchemeMode.CUSTOM -> "Своя"
        AppColorSchemeMode.DYNAMIC -> "Material You"
    }

    val fontLabel = when (settings.fontMode) {
        AppFontMode.SYSTEM -> "Сист."
        AppFontMode.SANS -> "Sans"
        AppFontMode.SERIF -> "Serif"
        AppFontMode.MONO -> "Mono"
        AppFontMode.EXTERNAL_MANROPE -> "Manrope"
        AppFontMode.EXTERNAL_CUSTOM -> "Свой"
    }

    return "$themeLabel • $paletteLabel • $fontLabel • ${settings.currencySymbolMode.symbol} ${(settings.fontScale * 100f).roundToInt()}%"
}

private const val DEFAULT_FONT_PROBE_BYTES = 256

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}

private fun formatGoogleSignInFailureMessage(context: Context, error: Throwable): String {
    val apiException = error as? ApiException
    if (apiException == null) {
        return "Не удалось войти в Google: ${error.message ?: "неизвестно"}"
    }

    return when (apiException.statusCode) {
        CommonStatusCodes.CANCELED -> "Вход в Google отменён."
        CommonStatusCodes.NETWORK_ERROR -> "Сетевая ошибка при входе в Google. Проверь интернет и повтори."
        CommonStatusCodes.SIGN_IN_REQUIRED -> "Требуется повторный вход в Google."
        CommonStatusCodes.DEVELOPER_ERROR -> {
            val signing = readAppSigningDiagnostics(context)
            if (signing.sha1.isNullOrBlank()) {
                "Ошибка OAuth (код 10): добавь Android OAuth client для ${signing.packageName} и подписи текущей сборки."
            } else {
                buildString {
                    append("Ошибка OAuth (код 10): добавь Android OAuth client для ")
                    append(signing.packageName)
                    append(" и SHA-1 ")
                    append(signing.sha1)
                    if (!signing.sha256.isNullOrBlank()) {
                        append(" (SHA-256: ")
                        append(signing.sha256)
                        append(")")
                    }
                    append(".")
                }
            }
        }
        else -> "Не удалось войти в Google (${apiException.statusCode}): ${apiException.localizedMessage ?: "неизвестно"}"
    }
}

private data class AppSigningDiagnostics(
    val packageName: String,
    val sha1: String?,
    val sha256: String?
)

private fun readAppSigningDiagnostics(context: Context): AppSigningDiagnostics {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return AppSigningDiagnostics(
            packageName = context.packageName,
            sha1 = null,
            sha256 = null
        )

        val firstSignature = signatures.firstOrNull() ?: return AppSigningDiagnostics(
            packageName = context.packageName,
            sha1 = null,
            sha256 = null
        )
        val certificateFactory = CertificateFactory.getInstance("X509")
        val certificate = certificateFactory.generateCertificate(
            ByteArrayInputStream(firstSignature.toByteArray())
        )
        val sha1Bytes = MessageDigest.getInstance("SHA-1").digest(certificate.encoded)
        val sha256Bytes = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        AppSigningDiagnostics(
            packageName = context.packageName,
            sha1 = sha1Bytes.joinToString(":") { byte -> "%02X".format(byte) },
            sha256 = sha256Bytes.joinToString(":") { byte -> "%02X".format(byte) }
        )
    }.getOrElse {
        AppSigningDiagnostics(
            packageName = context.packageName,
            sha1 = null,
            sha256 = null
        )
    }
}

private fun readAppSigningSha1(context: Context): String? {
    return readAppSigningDiagnostics(context).sha1
}

@Composable
fun ShiftSalaryApp(
    initialNavigationState: AppNavigationState = AppNavigationState(),
    appearanceSettings: AppearanceSettings,
    onSaveAppearanceSettings: (AppearanceSettings) -> Unit,
    profilesState: com.vigilante.shiftsalaryplanner.settings.AppProfilesState,
    appDependencies: AppDependencies,
    profileDependencies: ProfileDependencies
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val financeFeatureState = rememberFinanceFeatureState(currentMonth)
    val shiftFeatureState = rememberShiftFeatureState()
    val settingsFeatureState = rememberSettingsFeatureState()
    var navigationState by rememberSaveable(
        initialNavigationState,
        stateSaver = AppNavigationStateSaver
    ) {
        mutableStateOf(initialNavigationState)
    }
    val patternWorkflowState = rememberCalendarPatternWorkflowState()
    var activeWorkplaceId by rememberSaveable { mutableStateOf(WORKPLACE_MAIN_ID) }
    val calendarInteractionState = rememberCalendarInteractionState(
        workplaceFilterKey = appearanceSettings.calendarDefaultWorkplaceMode.name,
        initialWorkplaceFilterId = if (
            appearanceSettings.calendarDefaultWorkplaceMode == CalendarDefaultWorkplaceMode.ALL_WORKPLACES
        ) {
            CALENDAR_WORKPLACE_ALL_ID
        } else {
            activeWorkplaceId
        }
    )
    val notesFeatureState = rememberNotesFeatureState()

    val hasFullscreenUi = navigationState.screenStack.isNotEmpty()

    BackHandler(enabled = hasFullscreenUi) {
        when (navigationState.currentScreen) {
            AppScreen.SHIFT_TEMPLATE_EDITOR -> {
                    shiftFeatureState.clearEditor()
                    navigationState = navigationState.popScreen()
                }
            AppScreen.NOTE_EDITOR -> {
                    notesFeatureState.clearEditor()
                    navigationState = navigationState.popScreen()
                }
            AppScreen.DEDUCTION_EDITOR -> {
                    financeFeatureState.clearDeductionEdit()
                    navigationState = navigationState.popScreen()
                }
            null -> Unit
            else -> navigationState = navigationState.popScreen()
        }
    }

    val selectedTab = navigationState.selectedTab
    val financeSubTab = navigationState.financeSubTab
    val templateMode = TemplateMode.valueOf(shiftFeatureState.templateModeName)
    val payrollPeriodMode = remember(financeFeatureState.payrollPeriodModeName) {
        runCatching { PayrollPeriodMode.valueOf(financeFeatureState.payrollPeriodModeName) }.getOrElse { PayrollPeriodMode.MONTH }
    }
    val parsedRangeStartDate = remember(financeFeatureState.payrollRangeStartIso) {
        financeFeatureState.payrollRangeStartIso?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }
    val parsedRangeEndDate = remember(financeFeatureState.payrollRangeEndIso) {
        financeFeatureState.payrollRangeEndIso?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }
    val fallbackRangeStart = remember(currentMonth) { currentMonth.atDay(1) }
    val fallbackRangeEnd = remember(currentMonth) { currentMonth.atEndOfMonth() }
    val normalizedRangeStart = parsedRangeStartDate ?: fallbackRangeStart
    val normalizedRangeEnd = parsedRangeEndDate ?: fallbackRangeEnd
    val normalizedRangeBounds = remember(normalizedRangeStart, normalizedRangeEnd) {
        if (normalizedRangeStart.isAfter(normalizedRangeEnd)) {
            normalizedRangeEnd to normalizedRangeStart
        } else {
            normalizedRangeStart to normalizedRangeEnd
        }
    }
    val payrollPeriodStartDate = remember(
        payrollPeriodMode,
        currentMonth,
        financeFeatureState.payrollSelectedYear,
        normalizedRangeBounds
    ) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> currentMonth.atDay(1)
            PayrollPeriodMode.YEAR -> LocalDate.of(financeFeatureState.payrollSelectedYear, 1, 1)
            PayrollPeriodMode.RANGE -> normalizedRangeBounds.first
        }
    }
    val payrollPeriodEndDate = remember(
        payrollPeriodMode,
        currentMonth,
        financeFeatureState.payrollSelectedYear,
        normalizedRangeBounds
    ) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> currentMonth.atEndOfMonth()
            PayrollPeriodMode.YEAR -> LocalDate.of(financeFeatureState.payrollSelectedYear, 12, 31)
            PayrollPeriodMode.RANGE -> normalizedRangeBounds.second
        }
    }
    val payrollPeriodLabel = remember(payrollPeriodMode, currentMonth, payrollPeriodStartDate, payrollPeriodEndDate) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> formatMonthYearTitle(currentMonth)
            PayrollPeriodMode.YEAR -> "${payrollPeriodStartDate.year} год"
            PayrollPeriodMode.RANGE -> {
                if (payrollPeriodStartDate == payrollPeriodEndDate) {
                    formatDate(payrollPeriodStartDate)
                } else {
                    "${formatDate(payrollPeriodStartDate)} — ${formatDate(payrollPeriodEndDate)}"
                }
            }
        }
    }
    val payrollPeriodFileLabel = remember(payrollPeriodMode, currentMonth, payrollPeriodStartDate, payrollPeriodEndDate) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> "${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}"
            PayrollPeriodMode.YEAR -> "${payrollPeriodStartDate.year}"
            PayrollPeriodMode.RANGE -> "${payrollPeriodStartDate}_$payrollPeriodEndDate"
        }
    }
    val payrollPeriodSummarySuffix = remember(payrollPeriodMode, payrollPeriodStartDate, payrollPeriodEndDate) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> "за месяц"
            PayrollPeriodMode.YEAR -> "за год"
            PayrollPeriodMode.RANGE -> {
                if (payrollPeriodStartDate == payrollPeriodEndDate) "за день" else "за период"
            }
        }
    }
    val payrollPeriodAnchorMonth = remember(payrollPeriodEndDate) {
        YearMonth.from(payrollPeriodEndDate)
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alarmRuntimeState = rememberAlarmRuntimeState(profilesState.activeProfileId)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                alarmRuntimeState.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val appSigningDiagnostics = remember(context) { readAppSigningDiagnostics(context) }
    val profileStore = appDependencies.profileStore
    val activeProfileId = profilesState.activeProfileId
    val activeProfileName = profilesState.activeProfile?.name ?: AppProfileStore.DEFAULT_PROFILE_NAME

    val payrollSettingsStore = profileDependencies.payrollSettingsStore
    val reportVisibilitySettingsStore = profileDependencies.reportVisibilitySettingsStore
    val scheduleData = profileDependencies.scheduleData
    val workplacePayrollSettingsStore = profileDependencies.workplacePayrollSettingsStore
    val shiftAlarmStore = profileDependencies.shiftAlarmStore
    val patternTemplatesStore = profileDependencies.patternTemplatesStore
    val additionalPaymentsStore = profileDependencies.additionalPaymentsStore
    val deductionsStore = profileDependencies.deductionsStore
    val appEventLogStore = profileDependencies.appEventLogStore
    val reportHistoryStore = profileDependencies.reportHistoryStore
    val appWorkflowSettingsStore = profileDependencies.appWorkflowSettingsStore
    val assistantAiSettingsStore = profileDependencies.assistantAiSettingsStore
    val appNotesStore = profileDependencies.appNotesStore
    val todayLayoutSettingsStore = profileDependencies.todayLayoutSettingsStore
    val googleDriveSyncStore = profileDependencies.googleDriveSyncStore
    val googleDriveScope = appDependencies.googleDriveScope
    val googleSignInClient = appDependencies.googleSignInClient
    val initialGoogleSignedInAccount = remember {
        GoogleSignIn.getLastSignedInAccount(context)
            ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
    }
    val serviceWorkflowState = rememberServiceWorkflowState(initialGoogleSignedInAccount)
    val googleSyncMeta by googleDriveSyncStore.metaFlow.collectAsState(initial = GoogleDriveSyncMeta())
    val db = profileDependencies.database
    val holidaySyncRepository = profileDependencies.holidaySyncRepository
    val excelScheduleParser = appDependencies.excelScheduleParser
    val excelScheduleImporter = profileDependencies.excelScheduleImporter
    val scope = rememberCoroutineScope()
    val appSnackbarHostState = remember { SnackbarHostState() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        alarmRuntimeState.refreshPermissions()
    }

    val showInfoSnackbar: (String) -> Unit = { message ->
        appEventLogStore.add(title = message)
        scope.launch {
            appSnackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }
    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
        appEventLogStore.add(title = message, category = "UNDO")
        scope.launch {
            val result = appSnackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Отменить",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }
    val activateProfile: (String) -> Unit = { profileId ->
        if (profilesState.activeProfileId != profileId && profileStore.setActiveProfile(profileId)) {
            showInfoSnackbar("Профиль переключён")
            (context as? Activity)?.recreate()
        }
    }

    LaunchedEffect(serviceWorkflowState.googleSignedInAccount?.email) {
        googleDriveSyncStore.setAccountEmail(serviceWorkflowState.googleSignedInAccount?.email.orEmpty())
    }
    setCurrencySymbol(appearanceSettings.currencySymbolMode.symbol)

    val reportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = financeFeatureState.pendingReportCsvContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
            }
        }
        financeFeatureState.clearCsvPayload()
    }

    val reportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = financeFeatureState.pendingReportPdfBytes
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                }
            }
        }
        financeFeatureState.clearPdfPayload()
    }

    val excelImportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Не удалось прочитать Excel-файл")
            val fileName = uri.lastPathSegment ?: "tabel.xlsm"
            serviceWorkflowState.stageExcelFile(
                bytes = bytes,
                fileName = fileName,
                statusMessage = "Файл выбран: $fileName"
            )
        }.onFailure { error ->
            serviceWorkflowState.failExcelFile(
                "Не удалось открыть файл: ${error.message ?: "неизвестно"}"
            )
        }
    }
    val customFontFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_FONT_PROBE_BYTES)
                input.read(buffer)
            } ?: throw IllegalStateException("Не удалось открыть файл шрифта")

            val fontName = queryDisplayName(context, uri)
                ?: uri.lastPathSegment
                ?: "custom-font.ttf"

            onSaveAppearanceSettings(
                appearanceSettings.copy(
                    fontMode = AppFontMode.EXTERNAL_CUSTOM,
                    customFontUri = uri.toString(),
                    customFontDisplayName = fontName
                )
            )
            settingsFeatureState.setCustomFontStatus("Загружен шрифт: $fontName")
        }.onFailure { error ->
            settingsFeatureState.setCustomFontStatus("Ошибка загрузки шрифта: ${error.message ?: "неизвестно"}")
        }
    }
    val savedDays by scheduleData.shiftDays.collectAsState(initial = emptyList())
    val shiftTemplates by scheduleData.shiftTemplates.collectAsState(initial = emptyList())
    val holidays by scheduleData.holidays.collectAsState(initial = emptyList())
    val additionalPayments by additionalPaymentsStore.paymentsFlow.collectAsState(initial = emptyList())
    val deductions by deductionsStore.deductionsFlow.collectAsState(initial = emptyList())
    val patternTemplates by patternTemplatesStore.patternsFlow.collectAsState(initial = emptyList())

    val payrollSettings by payrollSettingsStore.settingsFlow.collectAsState(
        initial = neutralInitialPayrollSettings()
    )
    val reportVisibilitySettings by reportVisibilitySettingsStore.settingsFlow.collectAsState(
        initial = ReportVisibilitySettings()
    )
    val appEventLogItems by appEventLogStore.eventsFlow.collectAsState(
        initial = emptyList()
    )
    val reportHistoryItems by reportHistoryStore.itemsFlow.collectAsState(
        initial = emptyList()
    )
    val appWorkflowSettings by appWorkflowSettingsStore.settingsFlow.collectAsState(
        initial = com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettings()
    )
    val assistantAiSettings by assistantAiSettingsStore.settingsFlow.collectAsState(
        initial = AssistantAiSettings()
    )
    val todayLayoutSettings by todayLayoutSettingsStore.settingsFlow.collectAsState(
        initial = TodayLayoutSettings()
    )
    val appNotes by appNotesStore.notesFlow.collectAsState(initial = emptyList())
    val appNoteDates = remember(appNotes) {
        appNotes
            .mapNotNull { note -> runCatching { LocalDate.parse(note.date) }.getOrNull() }
            .toSet()
    }
    LaunchedEffect(activeProfileId, appWorkflowSettings.lastCheckedVersionCode) {
        val versionCode = currentAppVersionCode(context)
        if (versionCode > 0L && appWorkflowSettings.lastCheckedVersionCode != 0L &&
            appWorkflowSettings.lastCheckedVersionCode != versionCode
        ) {
            serviceWorkflowState.openPostUpdateCheck()
        }
        if (versionCode > 0L && appWorkflowSettings.lastCheckedVersionCode == 0L) {
            appWorkflowSettingsStore.save(appWorkflowSettings.copy(lastCheckedVersionCode = versionCode))
        }
    }
    LaunchedEffect(activeProfileId, appWorkflowSettings.quickStartDismissed) {
        navigationState = applyQuickStartNavigation(
            state = navigationState,
            quickStartDismissed = appWorkflowSettings.quickStartDismissed
        )
    }
    val workAssignmentsState by scheduleData.workAssignments.collectAsState(
        initial = WorkAssignmentsState(
            workplaces = defaultWorkplaces(),
            extraAssignmentsByDate = emptyMap()
        )
    )
    val workplacePayrollSettingsState by workplacePayrollSettingsStore.stateFlow.collectAsState(
        initial = WorkplacePayrollSettingsState(
            settingsByWorkplaceId = emptyMap()
        )
    )
    val shiftAlarmSettings by shiftAlarmStore.settingsFlow.collectAsState(
        initial = ShiftAlarmSettings()
    )
    val appearanceSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_APPEARANCE_SETTINGS)
    }
    val shiftSpecialPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREFS_SHIFT_SPECIAL_RULES)
    }
    val migrationPrefs = remember {
        context.getSharedPreferences(PREFS_ONE_TIME_MIGRATIONS, Context.MODE_PRIVATE)
    }
    val payrollSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences("payroll_settings")
    }
    val additionalPaymentsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences("additional_payments")
    }
    val payrollDeductionsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_PAYROLL_DEDUCTIONS)
    }
    val payrollYtdPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_PAYROLL_YTD)
    }
    val patternTemplatesPrefs = remember(activeProfileId) {
        context.profileSharedPreferences("pattern_templates")
    }
    val reportVisibilitySettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_REPORT_VISIBILITY_SETTINGS)
    }
    val workAssignmentsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_WORK_ASSIGNMENTS)
    }
    val workplacePayrollSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_WORKPLACE_PAYROLL_SETTINGS)
    }
    val workplacePayrollLegacyPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_WORKPLACE_PAYROLL_SALARIES_LEGACY)
    }
    val shiftAlarmSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences("shift_alarm_settings")
    }
    val shiftAlarmSchedulerPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_SHIFT_ALARM_SCHEDULER)
    }
    val shiftColorsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREFS_SHIFT_COLORS)
    }
    val shiftSpecialRules = remember(activeProfileId) { mutableStateMapOf<String, ShiftSpecialRule>() }
    val manualHolidayPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREFS_MANUAL_HOLIDAYS)
    }
    val calendarSyncPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREFS_CALENDAR_SYNC)
    }
    val sickLimitsCachePrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_SICK_LIMITS_CACHE)
    }
    val widgetSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREFS_WIDGET_SETTINGS)
    }
    val googleDriveSyncMetaPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(PREF_NAME_GOOGLE_DRIVE_SYNC_META)
    }
    val appEventLogPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(AppEventLogStore.PREFS_NAME)
    }
    val reportHistoryPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(ReportHistoryStore.PREFS_NAME)
    }
    val appWorkflowSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(AppWorkflowSettingsStore.PREFS_NAME)
    }
    val todayLayoutSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(TodayLayoutSettingsStore.PREFS_NAME)
    }
    val appNotesPrefs = remember(activeProfileId) {
        context.profileSharedPreferences(AppNotesStore.PREFS_NAME)
    }
    val manualHolidayRecords = remember(activeProfileId) { mutableStateListOf<ManualHolidayRecord>() }
    val widgetSettingsRuntimeState = rememberWidgetSettingsRuntimeState(activeProfileId)

    LaunchedEffect(savedDays, shiftTemplates) {
            ShiftMonthWidgetProviderV2.requestUpdate(context)
    }
    val editingAdditionalPayment = remember(financeFeatureState.editingAdditionalPaymentId, additionalPayments) {
        additionalPayments.firstOrNull { it.id == financeFeatureState.editingAdditionalPaymentId }
    }
    val editingDeduction = remember(financeFeatureState.editingDeductionId, deductions) {
        deductions.firstOrNull { it.id == financeFeatureState.editingDeductionId }
    }
    val editingShiftTemplate = remember(shiftFeatureState.editingShiftTemplateCode, shiftTemplates) {
        shiftTemplates.firstOrNull { it.code == shiftFeatureState.editingShiftTemplateCode }
    }
    val editingPattern = remember(patternWorkflowState.editingPatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == patternWorkflowState.editingPatternId }
    }
    val activePattern = remember(patternWorkflowState.activePatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == patternWorkflowState.activePatternId }
    }

    LaunchedEffect(currentMonth.year, holidays) {
        delay(700)

        if (settingsFeatureState.isHolidaySyncing) return@LaunchedEffect

        val hasFederalYear = holidays.any { it.date.startsWith("${currentMonth.year}-") }

        settingsFeatureState.startHolidaySync()
        try {
            val result = checkAndSyncFederalCalendarIfChanged(
                holidaySyncRepository = holidaySyncRepository,
                prefs = calendarSyncPrefs,
                year = currentMonth.year,
                hasLocalYear = hasFederalYear,
                forceNetworkCheck = !hasFederalYear
            )
            settingsFeatureState.updateHolidaySyncMessage(result.message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            settingsFeatureState.updateHolidaySyncMessage(if (hasFederalYear) {
                "Используется локальный календарь ${currentMonth.year}. Проверка не удалась: ${e.message ?: "неизвестно"}"
            } else {
                "Автозагрузка не удалась: ${e.message ?: "неизвестно"}"
            })
        } finally {
            settingsFeatureState.finishHolidaySync()
        }
    }

    val patternRangeStartDate = remember(patternWorkflowState.patternRangeStartIso) {
        patternWorkflowState.patternRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingPatternRangeStartDate = remember(patternWorkflowState.pendingPatternRangeStartIso) {
        patternWorkflowState.pendingPatternRangeStartIso?.let { LocalDate.parse(it) }
    }

    val pendingPatternRangeEndDate = remember(patternWorkflowState.pendingPatternRangeEndIso) {
        patternWorkflowState.pendingPatternRangeEndIso?.let { LocalDate.parse(it) }
    }
    val clearRangeStartDate = remember(patternWorkflowState.clearRangeStartIso) {
        patternWorkflowState.clearRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingClearRangeStartDate = remember(patternWorkflowState.pendingClearRangeStartIso) {
        patternWorkflowState.pendingClearRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingClearRangeEndDate = remember(patternWorkflowState.pendingClearRangeEndIso) {
        patternWorkflowState.pendingClearRangeEndIso?.let { LocalDate.parse(it) }
    }
    val applyingPattern = remember(patternWorkflowState.applyingPatternId, patternTemplates) {
        patternTemplates.firstOrNull { it.id == patternWorkflowState.applyingPatternId }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existingTemplates = scheduleData.shiftTemplates.first()
            if (existingTemplates.isEmpty()) {
                scheduleData.upsertShiftTemplates(DefaultShiftTemplates.items())
            }
        }
    }
    LaunchedEffect(holidays) {
        if (holidays.isEmpty()) {
            scheduleData.upsertHolidays(FederalHolidaySeed.federal2026())
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
    val systemStatusCodes = remember(shiftTemplates, shiftSpecialRulesSnapshot) {
        shiftTemplates
            .filter { template ->
                isProtectedSystemTemplate(template) ||
                        (shiftSpecialRulesSnapshot[template.code]?.isSystemStatus == true)
            }
            .map { stripWorkplaceScopeFromShiftCode(it.code) }
            .toSet()
    }

    val manualHolidayRecordsSnapshot by remember {
        derivedStateOf { manualHolidayRecords.toList() }
    }

    val editingManualHoliday = remember(settingsFeatureState.editingManualHolidayDate, manualHolidayRecordsSnapshot) {
        manualHolidayRecordsSnapshot.firstOrNull { it.date == settingsFeatureState.editingManualHolidayDate }
    }

    val editingShiftSpecialRule = remember(shiftFeatureState.editingShiftTemplateCode, shiftSpecialRulesSnapshot, editingShiftTemplate) {
        editingShiftTemplate?.let { template ->
            shiftSpecialRulesSnapshot[template.code] ?: defaultShiftSpecialRule(template.isWeekendPaid)
        }
    }
    val editingShiftAlarmTemplateConfig = remember(shiftFeatureState.editingShiftTemplateCode, shiftAlarmSettings, editingShiftTemplate) {
        editingShiftTemplate?.let { template ->
            shiftAlarmSettings.templateConfigs.firstOrNull { it.shiftCode == template.code }
        }
    }
    val shiftAlarmTemplateConfigsByCode = remember(shiftAlarmSettings.templateConfigs) {
        shiftAlarmSettings.templateConfigs.associateBy { it.shiftCode }
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

    val payrollSettingsOverridesByWorkplace = workplacePayrollSettingsState.settingsByWorkplaceId
    val payrollSettingsWorkplaceId = remember(
        savedDays,
        workAssignmentsState.extraAssignmentsByDate,
        financeFeatureState.payrollWorkplaceFilterId,
        payrollPeriodStartDate,
        payrollPeriodEndDate,
        systemStatusCodes
    ) {
        resolvePayrollSettingsWorkplaceId(
            selectedWorkplaceId = financeFeatureState.payrollWorkplaceFilterId,
            allWorkplacesId = PAYROLL_WORKPLACE_ALL_ID,
            periodStart = payrollPeriodStartDate,
            periodEnd = payrollPeriodEndDate,
            savedDays = savedDays,
            extraAssignmentsByDate = workAssignmentsState.extraAssignmentsByDate,
            systemStatusCodes = systemStatusCodes
        )
    }
    val payrollSettingsForSelectedWorkplace = remember(
        payrollSettings,
        payrollSettingsWorkplaceId,
        payrollSettingsOverridesByWorkplace
    ) {
        when (payrollSettingsWorkplaceId) {
            PAYROLL_WORKPLACE_ALL_ID,
            WORKPLACE_MAIN_ID -> payrollSettings
            else -> payrollSettingsOverridesByWorkplace[payrollSettingsWorkplaceId] ?: payrollSettings
        }
    }

    val normMode = remember(payrollSettingsForSelectedWorkplace.normMode) {
        runCatching { NormMode.valueOf(payrollSettingsForSelectedWorkplace.normMode) }
            .getOrElse { NormMode.MANUAL }
    }

    val annualNormSourceMode = remember(payrollSettingsForSelectedWorkplace.annualNormSourceMode) {
        runCatching { AnnualNormSourceMode.valueOf(payrollSettingsForSelectedWorkplace.annualNormSourceMode) }
            .getOrElse { AnnualNormSourceMode.WORKDAY_HOURS }
    }

    val effectiveNormHours = remember(
        currentMonth,
        resolvedHolidayMap,
        payrollSettingsForSelectedWorkplace,
        normMode,
        annualNormSourceMode
    ) {
        when (normMode) {
            NormMode.MANUAL -> payrollSettingsForSelectedWorkplace.monthlyNormHours

            NormMode.PRODUCTION_CALENDAR -> {
                calculateProductionCalendarMonthInfo(
                    month = currentMonth,
                    holidayMap = resolvedHolidayMap,
                    workdayHours = payrollSettingsForSelectedWorkplace.workdayHours
                ).normHours
            }

            NormMode.AVERAGE_ANNUAL -> {
                when (annualNormSourceMode) {
                    AnnualNormSourceMode.WORKDAY_HOURS -> {
                        calculateAverageAnnualNormHours(
                            year = currentMonth.year,
                            holidayMap = resolvedHolidayMap,
                            workdayHours = payrollSettingsForSelectedWorkplace.workdayHours
                        )
                    }

                    AnnualNormSourceMode.YEAR_TOTAL_HOURS -> {
                        (payrollSettingsForSelectedWorkplace.annualNormHours / 12.0).coerceAtLeast(0.0)
                    }
                }
            }

            NormMode.AVERAGE_QUARTERLY -> {
                calculateAverageQuarterNormHours(
                    month = currentMonth,
                    holidayMap = resolvedHolidayMap,
                    workdayHours = payrollSettingsForSelectedWorkplace.workdayHours
                )
            }
        }
    }

    val benefitReferenceYear = remember { LocalDate.now().year }
    val defaultSickCalculationPeriodDays = remember(benefitReferenceYear) {
        calculateDefaultSickCalculationPeriodDays(benefitReferenceYear)
    }

    val effectivePayrollSettings = remember(
        payrollSettingsForSelectedWorkplace,
        effectiveNormHours,
        defaultSickCalculationPeriodDays
    ) {
        val computedVacationAverageDaily = if (payrollSettingsForSelectedWorkplace.vacationAccruals12Months > 0.0) {
            calculateVacationAverageDailyFromAccruals(payrollSettingsForSelectedWorkplace.vacationAccruals12Months)
        } else {
            payrollSettingsForSelectedWorkplace.vacationAverageDaily
        }

        val resolvedSickCalculationPeriodDays = if (payrollSettingsForSelectedWorkplace.sickCalculationPeriodDays > 0) {
            payrollSettingsForSelectedWorkplace.sickCalculationPeriodDays
        } else {
            defaultSickCalculationPeriodDays
        }

        val hasDetailedSickInputs = payrollSettingsForSelectedWorkplace.sickIncomeYear1 > 0.0 ||
                payrollSettingsForSelectedWorkplace.sickIncomeYear2 > 0.0 ||
                payrollSettingsForSelectedWorkplace.sickLimitYear1 > 0.0 ||
                payrollSettingsForSelectedWorkplace.sickLimitYear2 > 0.0

        val computedSickAverageDaily = if (hasDetailedSickInputs) {
            calculateSickAverageDailyFromInputs(
                incomeYear1 = payrollSettingsForSelectedWorkplace.sickIncomeYear1,
                incomeYear2 = payrollSettingsForSelectedWorkplace.sickIncomeYear2,
                limitYear1 = payrollSettingsForSelectedWorkplace.sickLimitYear1,
                limitYear2 = payrollSettingsForSelectedWorkplace.sickLimitYear2,
                calculationPeriodDays = resolvedSickCalculationPeriodDays,
                excludedDays = payrollSettingsForSelectedWorkplace.sickExcludedDays
            )
        } else {
            payrollSettingsForSelectedWorkplace.sickAverageDaily
        }

        val normalizedNightPercent = payrollSettingsForSelectedWorkplace.nightPercent
            .coerceAtLeast(0.0)
            .let { value ->
                if (value > 3.0 && value <= 100.0) value / 100.0 else value
            }
        val normalizedNdflPercent = payrollSettingsForSelectedWorkplace.ndflPercent
            .coerceAtLeast(0.0)
            .let { value ->
                if (value > 1.0 && value <= 100.0) value / 100.0 else value
            }

        payrollSettingsForSelectedWorkplace.copy(
            monthlyNormHours = effectiveNormHours,
            vacationAverageDaily = computedVacationAverageDaily,
            sickAverageDaily = computedSickAverageDaily,
            sickCalculationPeriodDays = resolvedSickCalculationPeriodDays,
            nightPercent = normalizedNightPercent,
            ndflPercent = normalizedNdflPercent
        )
    }

    val quickShiftTemplates = remember(shiftTemplates, activeWorkplaceId, systemStatusCodes) {
        shiftTemplates
            .asSequence()
            .filter { it.active }
            .filter { template ->
                isShiftCodeForWorkplace(template.code, activeWorkplaceId) ||
                        isSystemStatusCode(template.code, systemStatusCodes)
            }
            .groupBy { template ->
                if (isSystemStatusCode(template.code, systemStatusCodes)) {
                    "system:${stripWorkplaceScopeFromShiftCode(template.code)}"
                } else {
                    "shift:${template.code}"
                }
            }
            .values
            .map { group ->
                group.firstOrNull { template -> !isWorkplaceScopedShiftCode(template.code) } ?: group.first()
            }
            .sortedBy { it.sortOrder }
            .toList()
    }
    val editableShiftTemplatesForActiveWorkplace = remember(shiftTemplates, activeWorkplaceId, systemStatusCodes) {
        shiftTemplates
            .filter { template ->
                isShiftCodeForWorkplace(template.code, activeWorkplaceId) ||
                        isSystemStatusCode(template.code, systemStatusCodes)
            }
            .groupBy { template ->
                if (isSystemStatusCode(template.code, systemStatusCodes)) {
                    "system:${stripWorkplaceScopeFromShiftCode(template.code)}"
                } else {
                    "shift:${template.code}"
                }
            }
            .values
            .map { group ->
                group.firstOrNull { template -> !isWorkplaceScopedShiftCode(template.code) } ?: group.first()
            }
            .sortedBy { it.sortOrder }
    }

    val alarmEligibleTemplates = remember(shiftTemplates) {
        shiftTemplates
            .filter { template -> !isWorkplaceScopedShiftCode(template.code) }
            .alarmEligibleTemplates()
    }

    val workplaces = remember(workAssignmentsState.workplaces) {
        workAssignmentsState.workplaces.ifEmpty { defaultWorkplaces() }
    }
    val dayPickerShiftTemplates = remember(shiftTemplates, workplaces, systemStatusCodes) {
        val orderByWorkplace = workplaces
            .mapIndexed { index, workplace -> workplace.id to index }
            .toMap()
        shiftTemplates
            .asSequence()
            .filter { it.active }
            .sortedWith(
                compareBy(
                    { template ->
                        if (isSystemStatusCode(template.code, systemStatusCodes)) Int.MAX_VALUE - 1
                        else orderByWorkplace[workplaceIdFromShiftCode(template.code)] ?: Int.MAX_VALUE
                    },
                    { template -> template.sortOrder }
                )
            )
            .groupBy { template ->
                if (isSystemStatusCode(template.code, systemStatusCodes)) {
                    "system:${stripWorkplaceScopeFromShiftCode(template.code)}"
                } else {
                    "shift:${template.code}"
                }
            }
            .values
            .map { group ->
                group.firstOrNull { template -> !isWorkplaceScopedShiftCode(template.code) } ?: group.first()
            }
            .sortedWith(
                compareBy(
                    { template ->
                        if (isSystemStatusCode(template.code, systemStatusCodes)) Int.MAX_VALUE - 1
                        else orderByWorkplace[workplaceIdFromShiftCode(template.code)] ?: Int.MAX_VALUE
                    },
                    { template -> template.sortOrder }
                )
            )
            .toList()
    }
    LaunchedEffect(workplaces, activeWorkplaceId) {
        if (workplaces.none { it.id == activeWorkplaceId }) {
            activeWorkplaceId = workplaces.firstOrNull()?.id ?: WORKPLACE_MAIN_ID
        }
    }
    LaunchedEffect(activeWorkplaceId, calendarInteractionState.calendarWorkplaceFilterId) {
        if (
            calendarInteractionState.calendarWorkplaceFilterId != CALENDAR_WORKPLACE_ALL_ID &&
            calendarInteractionState.calendarWorkplaceFilterId != activeWorkplaceId
        ) {
            calendarInteractionState.calendarWorkplaceFilterId = activeWorkplaceId
        }
    }
    LaunchedEffect(workplaces, calendarInteractionState.calendarWorkplaceFilterId, activeWorkplaceId) {
        if (calendarInteractionState.calendarWorkplaceFilterId == CALENDAR_WORKPLACE_ALL_ID) return@LaunchedEffect
        if (workplaces.none { it.id == calendarInteractionState.calendarWorkplaceFilterId }) {
            calendarInteractionState.calendarWorkplaceFilterId = if (workplaces.any { it.id == activeWorkplaceId }) {
                activeWorkplaceId
            } else {
                workplaces.firstOrNull()?.id ?: WORKPLACE_MAIN_ID
            }
        }
    }
    LaunchedEffect(workplaces, financeFeatureState.settingsWorkplaceId) {
        if (workplaces.none { it.id == financeFeatureState.settingsWorkplaceId }) {
            financeFeatureState.settingsWorkplaceId = workplaces.firstOrNull()?.id ?: WORKPLACE_MAIN_ID
        }
    }
    val mainShiftCodesByDate = remember(savedDays) {
        savedDays.associate { LocalDate.parse(it.date) to it.shiftCode }
    }
    val mainShiftDaysByDateAndCode = remember(savedDays) {
        savedDays.mapNotNull { day ->
            runCatching { LocalDate.parse(day.date) }
                .getOrNull()
                ?.let { date -> (date to day.shiftCode) to day }
        }.toMap()
    }
    val shiftOverrideDates = remember(savedDays) {
        savedDays.mapNotNull { day ->
            if (day.hasIndividualShiftOverride()) {
                runCatching { LocalDate.parse(day.date) }.getOrNull()
            } else {
                null
            }
        }.toSet()
    }
    val extraAssignmentsByDate = workAssignmentsState.extraAssignmentsByDate
    val allDayAssignmentsByDate = remember(mainShiftCodesByDate, extraAssignmentsByDate, workplaces) {
        val grouped = mutableMapOf<LocalDate, MutableMap<String, String>>()
        mainShiftCodesByDate.forEach { (date, code) ->
            grouped.getOrPut(date) { mutableMapOf() }[WORKPLACE_MAIN_ID] = code
        }
        extraAssignmentsByDate.forEach { (date, assignments) ->
            val bucket = grouped.getOrPut(date) { mutableMapOf() }
            assignments.forEach { (workplaceId, code) ->
                if (workplaceId != WORKPLACE_MAIN_ID && code.isNotBlank()) {
                    bucket[workplaceId] = code
                }
            }
        }

        val order = workplaces.map { it.id }
        grouped.mapValues { (_, byWorkplace) ->
            order.mapNotNull { workplaceId ->
                byWorkplace[workplaceId]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { code ->
                        CalendarDayAssignment(
                            workplaceId = workplaceId,
                            shiftCode = code
                        )
                    }
            }
        }
    }
    val activeWorkplaceShiftCodesByDate = remember(allDayAssignmentsByDate, activeWorkplaceId) {
        allDayAssignmentsByDate.mapNotNull { (date, assignments) ->
            assignments
                .firstOrNull { it.workplaceId == activeWorkplaceId }
                ?.let { assignment -> date to assignment.shiftCode }
        }.toMap()
    }
    val calendarDayAssignmentsByDate = remember(allDayAssignmentsByDate, calendarInteractionState.calendarWorkplaceFilterId) {
        if (calendarInteractionState.calendarWorkplaceFilterId == CALENDAR_WORKPLACE_ALL_ID) {
            allDayAssignmentsByDate
        } else {
            allDayAssignmentsByDate.mapNotNull { (date, assignments) ->
                val filtered = assignments.filter { it.workplaceId == calendarInteractionState.calendarWorkplaceFilterId }
                if (filtered.isNotEmpty()) date to filtered else null
            }.toMap()
        }
    }
    val calendarShiftCodesByDate = remember(calendarDayAssignmentsByDate) {
        calendarDayAssignmentsByDate.mapNotNull { (date, assignments) ->
            assignments.firstOrNull()?.let { assignment -> date to assignment.shiftCode }
        }.toMap()
    }
    val calendarMonthAudit = remember(
        currentMonth,
        allDayAssignmentsByDate,
        shiftAlarmTemplateConfigsByCode
    ) {
        auditCalendarMonth(
            month = currentMonth,
            dayAssignmentsByDate = allDayAssignmentsByDate,
            templateAlarmConfigs = shiftAlarmTemplateConfigsByCode
        )
    }
    val calendarMonthHistoryItems = remember(appEventLogItems, currentMonth) {
        val monthKey = formatYearMonthLabel(currentMonth)
        appEventLogItems
            .filter { item -> item.message.contains(monthKey) || item.title.contains(monthKey) }
            .take(4)
            .map { item ->
                "${formatBackupTimestamp(item.timestampMillis)} · ${item.title}"
            }
    }
    suspend fun clearAllAssignmentsForDate(date: LocalDate) {
        ShiftAlarmScheduler.clearSuppressedAlarmsForDate(context, date)
        scheduleData.deleteShiftDay(date.toString())
        allDayAssignmentsByDate[date]
            .orEmpty()
            .asSequence()
            .map { assignment -> assignment.workplaceId }
            .filter { workplaceId -> workplaceId != WORKPLACE_MAIN_ID }
            .distinct()
            .forEach { workplaceId ->
                scheduleData.setWorkplaceShift(
                    workplaceId = workplaceId,
                    date = date,
                    shiftCode = null
                )
            }
    }
    val payrollWorkplaceOptions = remember(workplaces) {
        listOf(PayrollWorkplaceOption(PAYROLL_WORKPLACE_ALL_ID, "Все работы")) +
                workplaces.map { workplace ->
                    PayrollWorkplaceOption(
                        id = workplace.id,
                        title = workplace.name
                    )
                }
    }
    LaunchedEffect(payrollWorkplaceOptions, financeFeatureState.payrollWorkplaceFilterId) {
        if (payrollWorkplaceOptions.none { it.id == financeFeatureState.payrollWorkplaceFilterId }) {
            financeFeatureState.selectWorkplace(PAYROLL_WORKPLACE_ALL_ID)
        }
    }
    val selectedPayrollWorkplaceName = remember(financeFeatureState.payrollWorkplaceFilterId, workplaces) {
        if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            "Все работы"
        } else {
            workplaces.firstOrNull { it.id == financeFeatureState.payrollWorkplaceFilterId }?.name ?: "Работа"
        }
    }
    val payrollSettingsForEditorWorkplace = remember(
        payrollSettings,
        financeFeatureState.settingsWorkplaceId,
        payrollSettingsOverridesByWorkplace
    ) {
        if (financeFeatureState.settingsWorkplaceId == WORKPLACE_MAIN_ID) {
            payrollSettings
        } else {
            payrollSettingsOverridesByWorkplace[financeFeatureState.settingsWorkplaceId] ?: payrollSettings
        }
    }
    val effectivePayrollPeriodLabel = remember(payrollPeriodLabel, selectedPayrollWorkplaceName, financeFeatureState.payrollWorkplaceFilterId) {
        if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            payrollPeriodLabel
        } else {
            "$payrollPeriodLabel · $selectedPayrollWorkplaceName"
        }
    }
    val effectivePayrollPeriodFileLabel = remember(payrollPeriodFileLabel, financeFeatureState.payrollWorkplaceFilterId) {
        if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            payrollPeriodFileLabel
        } else {
            "${payrollPeriodFileLabel}_$financeFeatureState.payrollWorkplaceFilterId"
        }
    }
    val payrollAssignmentCodesByDate = remember(allDayAssignmentsByDate, financeFeatureState.payrollWorkplaceFilterId) {
        allDayAssignmentsByDate.mapValues { (_, assignments) ->
            when (financeFeatureState.payrollWorkplaceFilterId) {
                PAYROLL_WORKPLACE_ALL_ID -> assignments.map { it.shiftCode }
                else -> assignments
                    .filter { it.workplaceId == financeFeatureState.payrollWorkplaceFilterId }
                    .map { it.shiftCode }
            }
        }
    }
    val shiftTemplateTimingByCode = remember(shiftAlarmSettings.templateConfigs) {
        shiftAlarmSettings.templateConfigs.associateBy { it.shiftCode }
    }
    val additionalPaymentsForSettingsWorkplace = remember(additionalPayments, financeFeatureState.settingsWorkplaceId) {
        additionalPayments.filter { payment ->
            belongsToWorkplace(payment.workplaceId, financeFeatureState.settingsWorkplaceId)
        }
    }
    val deductionsForSettingsWorkplace = remember(deductions, financeFeatureState.settingsWorkplaceId) {
        deductions.filter { deduction ->
            belongsToWorkplace(deduction.workplaceId, financeFeatureState.settingsWorkplaceId)
        }
    }
    val additionalPaymentsForPayroll = remember(additionalPayments, financeFeatureState.payrollWorkplaceFilterId) {
        if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            additionalPayments
        } else {
            additionalPayments.filter { payment ->
                belongsToWorkplace(payment.workplaceId, financeFeatureState.payrollWorkplaceFilterId)
            }
        }
    }
    val deductionsForPayroll = remember(deductions, financeFeatureState.payrollWorkplaceFilterId) {
        if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            deductions
        } else {
            deductions.filter { deduction ->
                belongsToWorkplace(deduction.workplaceId, financeFeatureState.payrollWorkplaceFilterId)
            }
        }
    }

    val periodEntries = remember(payrollAssignmentCodesByDate, payrollPeriodStartDate, payrollPeriodEndDate) {
        payrollAssignmentCodesByDate.entries
            .asSequence()
            .filter { (date, _) ->
                !date.isBefore(payrollPeriodStartDate) && !date.isAfter(payrollPeriodEndDate)
            }
            .flatMap { (date, codes) ->
                codes.asSequence().map { code -> date to code }
            }
            .toList()
    }

    val periodShifts = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        effectivePayrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode,
        mainShiftDaysByDateAndCode
    ) {
        periodEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = effectivePayrollSettings.applyShortDayReduction,
                specialRule = shiftSpecialRulesSnapshot[code],
                shiftTiming = shiftTemplateTimingByCode[code],
                dayOverride = mainShiftDaysByDateAndCode[date to code]
            )
        }
    }

    val periodShiftsWithoutShortDayReduction = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode,
        mainShiftDaysByDateAndCode
    ) {
        periodEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = false,
                specialRule = shiftSpecialRulesSnapshot[code],
                shiftTiming = shiftTemplateTimingByCode[code],
                dayOverride = mainShiftDaysByDateAndCode[date to code]
            )
        }
    }

    val firstHalfShifts = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        effectivePayrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode,
        mainShiftDaysByDateAndCode
    ) {
        periodEntries
            .filter { (date, _) -> date.dayOfMonth <= 15 }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = effectivePayrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code],
                    dayOverride = mainShiftDaysByDateAndCode[date to code]
                )
            }
    }

    val firstHalfShiftsWithoutShortDayReduction = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode,
        mainShiftDaysByDateAndCode
    ) {
        periodEntries
            .filter { (date, _) -> date.dayOfMonth <= 15 }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = false,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code],
                    dayOverride = mainShiftDaysByDateAndCode[date to code]
                )
            }
    }

    val summary = remember(periodShifts) {
        calculateSummaryForShifts(periodShifts)
    }

    val paymentResolution = remember(
        additionalPaymentsForPayroll,
        payrollPeriodStartDate,
        payrollPeriodEndDate,
        periodShifts,
        effectivePayrollSettings.baseSalary
    ) {
        resolveAdditionalPaymentsForPeriod(
            configuredPayments = additionalPaymentsForPayroll,
            startDate = payrollPeriodStartDate,
            endDate = payrollPeriodEndDate,
            shifts = periodShifts,
            baseSalary = effectivePayrollSettings.baseSalary
        )
    }

    val housingPaymentPeriodMonths = remember(payrollPeriodStartDate, payrollPeriodEndDate) {
        calculateMonthlyPaymentMultiplierForDateRange(
            startDate = payrollPeriodStartDate,
            endDate = payrollPeriodEndDate
        )
    }

    val payrollCalculationSettings = remember(effectivePayrollSettings, housingPaymentPeriodMonths) {
        effectivePayrollSettings.copy(
            housingPayment = (effectivePayrollSettings.housingPayment * housingPaymentPeriodMonths).coerceAtLeast(0.0)
        )
    }

    val payroll = remember(
        periodShifts,
        firstHalfShifts,
        payrollCalculationSettings,
        paymentResolution,
        deductionsForPayroll
    ) {
        PayrollCalculator.calculate(
            shifts = periodShifts,
            firstHalfShifts = firstHalfShifts,
            settings = payrollCalculationSettings,
            additionalPayments = paymentResolution.asPayrollPayments(),
            deductions = deductionsForPayroll
        )
    }

    val paymentDates = remember(payrollPeriodAnchorMonth, effectivePayrollSettings, extraDayOffDates) {
        calculatePaymentDates(
            month = payrollPeriodAnchorMonth,
            settings = effectivePayrollSettings,
            extraDayOffDates = extraDayOffDates
        )
    }

    val overtimePeriodInfo = remember(payrollPeriodAnchorMonth, effectivePayrollSettings.overtimePeriod) {
        resolveOvertimePeriodInfo(payrollPeriodAnchorMonth, effectivePayrollSettings.overtimePeriod)
    }

    val annualOvertime = remember(
        payrollAssignmentCodesByDate,
        overtimePeriodInfo,
        effectivePayrollSettings,
        normMode,
        annualNormSourceMode,
        templateMap,
        shiftSpecialRulesSnapshot,
        resolvedHolidayMap,
        effectivePayrollSettings.applyShortDayReduction,
        shiftTemplateTimingByCode,
        mainShiftDaysByDateAndCode
    ) {
        val overtimeEntries = payrollAssignmentCodesByDate.entries
            .asSequence()
            .filter { (date, _) ->
                !date.isBefore(overtimePeriodInfo.startDate) && !date.isAfter(overtimePeriodInfo.endDate)
            }
            .flatMap { (date, codes) ->
                codes.asSequence().map { code -> date to code }
            }
            .toList()

        val periodShifts = overtimeEntries
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = effectivePayrollSettings.applyShortDayReduction,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code],
                    dayOverride = mainShiftDaysByDateAndCode[date to code]
                )
            }

        val basePeriodNormHours = calculateNormHoursForPeriod(
            periodInfo = overtimePeriodInfo,
            payrollSettings = effectivePayrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = resolvedHolidayMap
        )

        val adjustedPeriodNormHours = calculateAdjustedNormHoursForPeriod(
            basePeriodNormHours = basePeriodNormHours,
            shifts = periodShifts,
            holidayMap = resolvedHolidayMap,
            workdayHours = effectivePayrollSettings.workdayHours,
            applyShortDayReduction = effectivePayrollSettings.applyShortDayReduction
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

    val detailedShiftStats = remember(periodShifts, firstHalfShifts, paymentResolution, payroll, annualOvertime) {
        calculateDetailedShiftStats(
            shifts = periodShifts,
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

    val payrollDiagnosticsState = remember(
        effectivePayrollPeriodLabel,
        selectedPayrollWorkplaceName,
        payrollPeriodStartDate,
        payrollPeriodEndDate,
        payrollCalculationSettings,
        summary,
        payroll,
        resolvedAdditionalPaymentBreakdown,
        deductionsForPayroll,
        periodShiftsWithoutShortDayReduction,
        firstHalfShiftsWithoutShortDayReduction
    ) {
        PayrollDiagnosticsState(
            periodLabel = effectivePayrollPeriodLabel,
            workplaceLabel = selectedPayrollWorkplaceName,
            periodStartDate = payrollPeriodStartDate,
            periodEndDate = payrollPeriodEndDate,
            payrollSettings = payrollCalculationSettings,
            summary = summary,
            payroll = payroll,
            resolvedAdditionalPayments = resolvedAdditionalPaymentBreakdown,
            deductions = deductionsForPayroll,
            rawWorkedHoursBeforeShortReduction = periodShiftsWithoutShortDayReduction.sumOf { it.paidHours },
            rawFirstHalfHoursBeforeShortReduction = firstHalfShiftsWithoutShortDayReduction.sumOf { it.paidHours }
        )
    }

    val payrollPeriodNormHours = remember(
        payrollPeriodStartDate,
        payrollPeriodEndDate,
        effectivePayrollSettings,
        normMode,
        annualNormSourceMode,
        resolvedHolidayMap
    ) {
        calculateNormHoursForDateRange(
            startDate = payrollPeriodStartDate,
            endDate = payrollPeriodEndDate,
            payrollSettings = effectivePayrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = resolvedHolidayMap,
            applyShortDayReduction = effectivePayrollSettings.applyShortDayReduction
        )
    }

    LaunchedEffect(savedDays, templateMap, shiftAlarmSettings) {
        alarmRuntimeState.lastRescheduleResult = rescheduleShiftAlarms(
            context = context,
            settings = shiftAlarmSettings,
            savedDays = savedDays,
            templateMap = templateMap
        )
    }
    val upcomingShiftAlarms = remember(
        shiftAlarmSettings,
        savedDays,
        templateMap,
        alarmRuntimeState.permissionRefreshToken
    ) {
        ShiftAlarmScheduler.previewUpcomingAlarms(
            context = context,
            settings = shiftAlarmSettings,
            savedDays = savedDays,
            templateMap = templateMap,
            limit = 200
        )
    }
    val canScheduleExactShiftAlarms = remember(context, alarmRuntimeState.permissionRefreshToken) {
        ShiftAlarmScheduler.canScheduleExactShiftAlarms(context)
    }
    val shiftAlarmNotificationPermissionGranted = remember(context, alarmRuntimeState.permissionRefreshToken) {
        ShiftAlarmScheduler.hasNotificationPermission(context)
    }
    val shiftAlarmFullScreenIntentPermissionGranted = remember(context, alarmRuntimeState.permissionRefreshToken) {
        ShiftAlarmScheduler.hasFullScreenIntentPermission(context)
    }
    val appHealthItems = listOf(
        AppHealthCheckItem(
            title = "Уведомления будильника",
            message = if (shiftAlarmNotificationPermissionGranted) {
                "Разрешение на уведомления выдано."
            } else {
                "Без уведомлений будильник может не показать звонок поверх экрана."
            },
            severity = if (shiftAlarmNotificationPermissionGranted) AppHealthSeverity.OK else AppHealthSeverity.ERROR,
            actionLabel = if (shiftAlarmNotificationPermissionGranted) null else "Разрешить",
            onAction = if (shiftAlarmNotificationPermissionGranted) null else {
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        ),
        AppHealthCheckItem(
            title = "Полноэкранный будильник",
            message = if (shiftAlarmFullScreenIntentPermissionGranted) {
                "Полноэкранный режим разрешён."
            } else {
                "После обновлений Android может сбрасывать это разрешение. Лучше включить его вручную."
            },
            severity = if (shiftAlarmFullScreenIntentPermissionGranted) AppHealthSeverity.OK else AppHealthSeverity.WARNING,
            actionLabel = if (shiftAlarmFullScreenIntentPermissionGranted) null else "Открыть",
            onAction = if (shiftAlarmFullScreenIntentPermissionGranted) null else {
                { openFullScreenIntentPermissionSettings(context) }
            }
        ),
        AppHealthCheckItem(
            title = "Точные будильники",
            message = if (canScheduleExactShiftAlarms) {
                "Приложение может ставить точные срабатывания."
            } else {
                "Если запретить точные будильники, Android может сдвигать время звонка."
            },
            severity = if (canScheduleExactShiftAlarms) AppHealthSeverity.OK else AppHealthSeverity.WARNING,
            actionLabel = if (canScheduleExactShiftAlarms) null else "Открыть",
            onAction = if (canScheduleExactShiftAlarms) null else {
                { openExactAlarmPermissionSettings(context) }
            }
        ),
        AppHealthCheckItem(
            title = "Ближайший будильник",
            message = upcomingShiftAlarms.firstOrNull()?.let { alarm ->
                "${formatBackupTimestamp(alarm.triggerAtMillis)} • ${alarm.title}"
            } ?: "Активных будущих будильников не найдено.",
            severity = if (upcomingShiftAlarms.isNotEmpty()) AppHealthSeverity.OK else AppHealthSeverity.INFO,
            actionLabel = "Будильники",
            onAction = {
                navigationState = navigationState.closeScreen(AppScreen.APP_HEALTH_CHECK)
                navigationState = navigationState.selectTab(BottomTab.ALARMS)
            }
        ),
        AppHealthCheckItem(
            title = "Google Drive",
            message = if (googleSyncMeta.accountEmail.isNotBlank()) {
                "Подключён ${googleSyncMeta.accountEmail}. Последняя загрузка: ${formatBackupTimestamp(googleSyncMeta.lastUploadAt)}."
            } else {
                "Аккаунт не подключён. Резервные копии доступны только вручную."
            },
            severity = if (googleSyncMeta.accountEmail.isNotBlank()) AppHealthSeverity.OK else AppHealthSeverity.INFO,
            actionLabel = "Резервная копия",
            onAction = {
                navigationState = navigationState.replaceScreen(
                    from = AppScreen.APP_HEALTH_CHECK,
                    to = AppScreen.BACKUP_RESTORE
                )
            }
        ),
        AppHealthCheckItem(
            title = "Проверка месяца",
            message = "Пустых дней: ${calendarMonthAudit.emptyDayCount}; дней с несколькими работами: ${calendarMonthAudit.multiWorkDayCount}; пересечений по времени: ${calendarMonthAudit.overlappingWorkDayCount}.",
            severity = when {
                calendarMonthAudit.overlappingWorkDayCount > 0 -> AppHealthSeverity.WARNING
                calendarMonthAudit.emptyDayCount > 0 -> AppHealthSeverity.INFO
                else -> AppHealthSeverity.OK
            }
        ),
        AppHealthCheckItem(
            title = "История отчётов",
            message = if (reportHistoryItems.isEmpty()) {
                "Пока нет сохранённых снимков экспортов."
            } else {
                "В истории ${reportHistoryItems.size} экспортов."
            },
            severity = AppHealthSeverity.INFO,
            actionLabel = "Открыть",
            onAction = {
                navigationState = navigationState.replaceScreen(
                    from = AppScreen.APP_HEALTH_CHECK,
                    to = AppScreen.REPORT_HISTORY
                )
            }
        )
    )
    val todayWorkSummary = remember(allDayAssignmentsByDate, templateMap, workplaces) {
        buildCompactDayWorkSummary(
            date = LocalDate.now(),
            assignmentsByDate = allDayAssignmentsByDate,
            templateMap = templateMap,
            workplaces = workplaces
        )
    }
    val todayShiftPreviews = remember(
        allDayAssignmentsByDate,
        templateMap,
        workplaces,
        shiftAlarmTemplateConfigsByCode
    ) {
        buildTodayShiftPreviews(
            date = LocalDate.now(),
            assignmentsByDate = allDayAssignmentsByDate,
            templateMap = templateMap,
            workplaces = workplaces,
            templateAlarmConfigs = shiftAlarmTemplateConfigsByCode
        )
    }
    val tomorrowWorkSummary = remember(allDayAssignmentsByDate, templateMap, workplaces) {
        buildCompactDayWorkSummary(
            date = LocalDate.now().plusDays(1),
            assignmentsByDate = allDayAssignmentsByDate,
            templateMap = templateMap,
            workplaces = workplaces
        )
    }
    val nextAlarmSummary = remember(upcomingShiftAlarms) {
        upcomingShiftAlarms.firstOrNull()?.let { alarm ->
            "${formatBackupTimestamp(alarm.triggerAtMillis)} • ${alarm.title}"
        }.orEmpty()
    }
    val todayNotes = remember(appNotes) {
        appNotes
            .filter { it.date == LocalDate.now().toString() }
            .sortedByDescending { it.updatedAtMillis }
    }
    val upcomingPaymentItems = remember(
        payrollPeriodAnchorMonth,
        effectivePayrollSettings,
        extraDayOffDates,
        payroll,
        payrollAssignmentCodesByDate,
        templateMap
    ) {
        buildUpcomingPaymentItems(
            today = LocalDate.now(),
            anchorMonth = payrollPeriodAnchorMonth,
            settings = effectivePayrollSettings,
            extraDayOffDates = extraDayOffDates,
            currentPayroll = payroll,
            shiftCodesByDate = payrollAssignmentCodesByDate,
            templateMap = templateMap
        )
    }
    LaunchedEffect(
        savedDays,
        shiftTemplates,
        holidays,
        workAssignmentsState,
        shiftAlarmSettings,
        payroll,
        appNotes,
        assistantAiSettings
    ) {
        runCatching { WearSyncBridge.publishSnapshot(context) }
    }
    val payrollDetailedResult = remember(
        payrollPeriodAnchorMonth,
        effectivePayrollPeriodLabel,
        payrollPeriodNormHours,
        housingPaymentPeriodMonths,
        payroll,
        detailedShiftStats,
        payrollCalculationSettings,
        payrollSettings.housingPaymentLabel,
        payrollSettings.housingPaymentTaxable,
        resolvedAdditionalPaymentBreakdown
    ) {
        PayrollSheetDraftFactory.build(
            month = payrollPeriodAnchorMonth,
            periodLabel = effectivePayrollPeriodLabel,
            periodSummarySuffix = payrollPeriodSummarySuffix,
            periodNormHours = payrollPeriodNormHours,
            housingPaymentMonthsQuantity = housingPaymentPeriodMonths,
            summary = payroll,
            detailedShiftStats = detailedShiftStats,
            payrollSettings = payrollCalculationSettings,
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

    LaunchedEffect(activeWorkplaceId, shiftTemplates, systemStatusCodes) {
        if (activeWorkplaceId == WORKPLACE_MAIN_ID) return@LaunchedEffect

        val alreadyHasScopedTemplates = shiftTemplates.any { template ->
            isShiftCodeForWorkplace(template.code, activeWorkplaceId) &&
                    !isSystemStatusCode(template.code, systemStatusCodes)
        }
        if (alreadyHasScopedTemplates) {
            markWorkplaceTemplatesSeeded(workAssignmentsPrefs, activeWorkplaceId)
        }
    }

    LaunchedEffect(activeWorkplaceId, workAssignmentsState.extraAssignmentsByDate) {
        if (activeWorkplaceId == WORKPLACE_MAIN_ID) return@LaunchedEffect

        workAssignmentsState.extraAssignmentsByDate.forEach { (date, byWorkplace) ->
            val rawCode = byWorkplace[activeWorkplaceId] ?: return@forEach
            if (isWorkplaceScopedShiftCode(rawCode)) return@forEach
            val scopedCode = workplaceScopedShiftCode(activeWorkplaceId, rawCode)
            scheduleData.setWorkplaceShift(
                workplaceId = activeWorkplaceId,
                date = date,
                shiftCode = scopedCode
            )
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
            scheduleData.deleteShiftTemplate(template)
            shiftColorsPrefs.edit { remove(template.code) }
            shiftColors.remove(template.code)
            removeShiftSpecialRule(
                shiftSpecialRules = shiftSpecialRules,
                shiftSpecialPrefs = shiftSpecialPrefs,
                code = template.code
            )
            shiftAlarmStore.removeTemplateConfig(template.code)
        }

        if (calendarInteractionState.activeBrushCode != null && removableLegacyTemplates.any { it.code == calendarInteractionState.activeBrushCode }) {
            calendarInteractionState.activeBrushCode = null
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

        migrationPrefs.edit {
            putBoolean(KEY_MIGRATION_LEGACY_DEFAULTS_CLEANUP_V1, true)
        }
    }

    LaunchedEffect(shiftTemplates, savedDays, shiftSpecialRulesSnapshot, shiftAlarmSettings) {
        if (shiftTemplates.isEmpty()) return@LaunchedEffect
        if (migrationPrefs.getBoolean(KEY_MIGRATION_SYSTEM_STATUS_SCOPE_CLEANUP_V1, false)) return@LaunchedEffect

        val scopedSystemTemplates = shiftTemplates.filter { template ->
            isWorkplaceScopedShiftCode(template.code) && (
                    isProtectedSystemTemplate(template) ||
                            (shiftSpecialRulesSnapshot[template.code]?.isSystemStatus == true)
                    )
        }

        val templatesByCode = shiftTemplates.associateBy { it.code }.toMutableMap()

        scopedSystemTemplates
            .sortedWith(compareBy({ it.sortOrder }, { it.code }))
            .forEach { scopedTemplate ->
                val canonicalCode = stripWorkplaceScopeFromShiftCode(scopedTemplate.code)
                val existingCanonical = templatesByCode[canonicalCode]
                val canonicalIsSystem = existingCanonical?.let { template ->
                    isProtectedSystemTemplate(template) ||
                            (shiftSpecialRulesSnapshot[template.code]?.isSystemStatus == true)
                } ?: true
                if (!canonicalIsSystem) return@forEach

                val canonicalTemplate = existingCanonical ?: scopedTemplate.copy(
                    code = canonicalCode,
                    totalHours = 0.0,
                    breakHours = 0.0,
                    nightHours = 0.0,
                    isWeekendPaid = false
                ).also { normalized ->
                    scheduleData.upsertShiftTemplate(normalized)
                    templatesByCode[canonicalCode] = normalized
                }

                savedDays
                    .filter { day -> day.shiftCode == scopedTemplate.code }
                    .forEach { day ->
                        scheduleData.upsertShiftDay(day.copy(shiftCode = canonicalTemplate.code))
                    }

                scheduleData.replaceShiftCode(
                    oldShiftCode = scopedTemplate.code,
                    newShiftCode = canonicalTemplate.code
                )

                if (!shiftColorsPrefs.contains(canonicalTemplate.code)) {
                    val migratedColor = shiftColors[scopedTemplate.code]
                        ?: parseColorHex(scopedTemplate.colorHex, 0xFFE0E0E0.toInt())
                    saveShiftColor(
                        shiftColors = shiftColors,
                        shiftColorsPrefs = shiftColorsPrefs,
                        context = context,
                        key = canonicalTemplate.code,
                        colorValue = migratedColor
                    )
                }

                val scopedRule = shiftSpecialRulesSnapshot[scopedTemplate.code]
                val canonicalRule = shiftSpecialRulesSnapshot[canonicalTemplate.code]
                if (scopedRule != null && canonicalRule == null) {
                    val migratedRule = scopedRule.copy(isSystemStatus = true)
                    saveShiftSpecialRule(
                        shiftSpecialRules = shiftSpecialRules,
                        shiftSpecialPrefs = shiftSpecialPrefs,
                        code = canonicalTemplate.code,
                        rule = migratedRule
                    )
                    shiftSpecialRules[canonicalTemplate.code] = migratedRule
                }

                val scopedAlarmConfig = shiftAlarmSettings.templateConfigs.firstOrNull { config ->
                    config.shiftCode == scopedTemplate.code
                }
                val hasCanonicalAlarm = shiftAlarmSettings.templateConfigs.any { config ->
                    config.shiftCode == canonicalTemplate.code
                }
                if (scopedAlarmConfig != null && !hasCanonicalAlarm) {
                    shiftAlarmStore.upsertTemplateConfig(
                        scopedAlarmConfig.copy(shiftCode = canonicalTemplate.code)
                    )
                }

                scheduleData.deleteShiftTemplate(scopedTemplate)
                templatesByCode.remove(scopedTemplate.code)

                shiftColorsPrefs.edit { remove(scopedTemplate.code) }
                shiftColors.remove(scopedTemplate.code)

                removeShiftSpecialRule(
                    shiftSpecialRules = shiftSpecialRules,
                    shiftSpecialPrefs = shiftSpecialPrefs,
                    code = scopedTemplate.code
                )
                shiftSpecialRules.remove(scopedTemplate.code)

                shiftAlarmStore.removeTemplateConfig(scopedTemplate.code)
            }

        migrationPrefs.edit {
            putBoolean(KEY_MIGRATION_SYSTEM_STATUS_SCOPE_CLEANUP_V1, true)
        }
    }

    val backupPrefSnapshots = listOf(
        PREF_NAME_APPEARANCE_SETTINGS to appearanceSettingsPrefs,
        PREF_NAME_PAYROLL_SETTINGS to payrollSettingsPrefs,
        PREF_NAME_PAYROLL_YTD to payrollYtdPrefs,
        PREF_NAME_REPORT_VISIBILITY_SETTINGS to reportVisibilitySettingsPrefs,
        PREF_NAME_WORK_ASSIGNMENTS to workAssignmentsPrefs,
        PREF_NAME_WORKPLACE_PAYROLL_SETTINGS to workplacePayrollSettingsPrefs,
        PREF_NAME_WORKPLACE_PAYROLL_SALARIES_LEGACY to workplacePayrollLegacyPrefs,
        PREF_NAME_ADDITIONAL_PAYMENTS to additionalPaymentsPrefs,
        PREF_NAME_PAYROLL_DEDUCTIONS to payrollDeductionsPrefs,
        PREF_NAME_PATTERN_TEMPLATES to patternTemplatesPrefs,
        PREF_NAME_SHIFT_ALARM_SETTINGS to shiftAlarmSettingsPrefs,
        PREF_NAME_SHIFT_ALARM_SCHEDULER to shiftAlarmSchedulerPrefs,
        PREF_NAME_SHIFT_COLORS to shiftColorsPrefs,
        PREF_NAME_SHIFT_SPECIAL_RULES to shiftSpecialPrefs,
        PREF_NAME_MANUAL_HOLIDAYS to manualHolidayPrefs,
        PREF_NAME_CALENDAR_SYNC_META to calendarSyncPrefs,
        PREF_NAME_WIDGET_SETTINGS to widgetSettingsPrefs,
        PREF_NAME_GOOGLE_DRIVE_SYNC_META to googleDriveSyncMetaPrefs,
        AppEventLogStore.PREFS_NAME to appEventLogPrefs,
        ReportHistoryStore.PREFS_NAME to reportHistoryPrefs,
        AppWorkflowSettingsStore.PREFS_NAME to appWorkflowSettingsPrefs,
        TodayLayoutSettingsStore.PREFS_NAME to todayLayoutSettingsPrefs,
        AppNotesStore.PREFS_NAME to appNotesPrefs,
        PREF_NAME_SICK_LIMITS_CACHE to sickLimitsCachePrefs
    )

    val buildCurrentBackupJson: () -> String = {
        buildBackupJsonForExport(
            prefSnapshots = backupPrefSnapshots,
            shiftDays = savedDays,
            shiftTemplates = shiftTemplates
        )
    }
    val resolveGoogleAccount: () -> GoogleSignInAccount? = {
        val current = serviceWorkflowState.googleSignedInAccount
            ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
        if (current != null) {
            current
        } else {
            GoogleSignIn.getLastSignedInAccount(context)
                ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
                ?.also { account ->
                    serviceWorkflowState.setSignedInAccount(account)
                }
        }
    }
    val uploadBackupToCloud: (GoogleSignInAccount, Boolean) -> Unit = { account, auto ->
        scope.launch {
            serviceWorkflowState.backupRestoreStatusMessage = if (auto) {
                "Автозагрузка резервной копии в Google Drive..."
            } else {
                "Загружаем резервную копию в Google Drive..."
            }
            runCatching {
                val backupJson = buildCurrentBackupJson()
                withContext(Dispatchers.IO) {
                    uploadBackupToGoogleDriveAppData(
                        context = context,
                        account = account,
                        backupJson = backupJson
                    )
                }
            }.onSuccess { uploadResult ->
                googleDriveSyncStore.markUpload(
                    cloudModifiedAtMillis = uploadResult.remoteFile.modifiedAtMillis
                )
                serviceWorkflowState.backupRestoreStatusMessage = if (uploadResult.created) {
                    if (auto) "Автокопия загружена в Google Drive" else "Копия загружена в Google Drive"
                } else {
                    if (auto) "Автокопия в Google Drive обновлена" else "Копия в Google Drive обновлена"
                }
            }.onFailure { error ->
                serviceWorkflowState.backupRestoreStatusMessage =
                    "Ошибка загрузки в Google Drive: ${error.message ?: "неизвестно"}"
            }
        }
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
        runCatching {
            accountTask.getResult(ApiException::class.java)
        }.onSuccess { account ->
            if (GoogleSignIn.hasPermissions(account, googleDriveScope)) {
                serviceWorkflowState.setSignedInAccount(account)
                serviceWorkflowState.backupRestoreStatusMessage = "Google Drive подключён: ${account.email ?: "аккаунт"}"
                serviceWorkflowState.clearAutoUploadCheck()
            } else {
                serviceWorkflowState.backupRestoreStatusMessage = "Не выданы права для Google Drive"
            }
        }.onFailure { error ->
            serviceWorkflowState.backupRestoreStatusMessage = formatGoogleSignInFailureMessage(context, error)
        }
    }

    val backupJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = serviceWorkflowState.pendingBackupJsonContent
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
                serviceWorkflowState.backupRestoreStatusMessage = "Резервная копия сохранена"
            }.onFailure { error ->
                serviceWorkflowState.backupRestoreStatusMessage = "Не удалось сохранить копию: ${error.message ?: "неизвестно"}"
            }
        }
        serviceWorkflowState.clearBackupPayload()
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
                    upsertShiftTemplate = { template -> scheduleData.upsertShiftTemplate(template) },
                    deleteShiftTemplate = { template -> scheduleData.deleteShiftTemplate(template) },
                    upsertShiftDay = { day -> scheduleData.upsertShiftDay(day) },
                    deleteShiftDayByDate = { date -> scheduleData.deleteShiftDay(date) },
                    onStatus = { message -> serviceWorkflowState.backupRestoreStatusMessage = message },
                    onAfterImport = { (context as? Activity)?.recreate() }
                )
            }.onFailure { error ->
                serviceWorkflowState.backupRestoreStatusMessage =
                    "Не удалось восстановить копию: ${error.message ?: "неизвестно"}"
            }
        }
    }

    LaunchedEffect(
        serviceWorkflowState.googleSignedInAccount?.email,
        googleSyncMeta.autoUploadEnabled,
        googleSyncMeta.autoUploadIntervalHours,
        googleSyncMeta.lastUploadAt
    ) {
        val account = resolveGoogleAccount() ?: return@LaunchedEffect
        if (!googleSyncMeta.autoUploadEnabled) return@LaunchedEffect

        val accountKey = account.email ?: account.id ?: return@LaunchedEffect
        if (serviceWorkflowState.autoUploadCheckedForAccount == accountKey) return@LaunchedEffect
        serviceWorkflowState.markAutoUploadChecked(accountKey)

        val intervalMillis = googleSyncMeta.autoUploadIntervalHours * 60L * 60L * 1000L
        val now = System.currentTimeMillis()
        val shouldUpload = googleSyncMeta.lastUploadAt <= 0L ||
                now - googleSyncMeta.lastUploadAt >= intervalMillis

        if (shouldUpload) {
            uploadBackupToCloud(account, true)
        }
    }

    AppTabHostScaffold(
        isLandscape = isLandscape,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            navigationState = navigationState.selectTab(tab)
        },
        snackbarHost = {
            SnackbarHost(hostState = appSnackbarHostState)
        }
    ) { tab ->
        AppSectionTypography(
            fontMode = appearanceSettings.fontModeForSection(tab.appearanceFontSection())
        ) {
                when (tab) {
                    BottomTab.CALENDAR -> {
                        CalendarTab(
                            currentMonth = currentMonth,
                            onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                            onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                            onPickMonth = { pickedMonth ->
                                currentMonth = pickedMonth
                            },
                            profiles = profilesState.profiles,
                            activeProfileId = profilesState.activeProfileId,
                            onSwitchProfile = activateProfile,
                            onOpenProfiles = { navigationState = navigationState.openScreen(AppScreen.PROFILES) },
                            workplaces = workplaces,
                            calendarWorkplaceFilterId = calendarInteractionState.calendarWorkplaceFilterId,
                            onSwitchCalendarWorkplaceFilter = { selectedId ->
                                calendarInteractionState.calendarWorkplaceFilterId = selectedId
                                if (selectedId != CALENDAR_WORKPLACE_ALL_ID) {
                                    activeWorkplaceId = selectedId
                                }
                            },
                            activeWorkplaceId = activeWorkplaceId,
                            onOpenManageWorkplaces = { settingsFeatureState.openWorkplaceRename() },
                            shiftCodesByDate = calendarShiftCodesByDate,
                            dayAssignmentsByDate = calendarDayAssignmentsByDate,
                            noteDates = appNoteDates,
                            shiftOverrideDates = shiftOverrideDates,
                            todayNotes = todayNotes,
                            onAddTodayNote = {
                                notesFeatureState.openEditor(
                                    noteId = null,
                                    dateIso = LocalDate.now().toString(),
                                    workplaceId = null,
                                    shiftCode = null
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            onEditNote = { noteId ->
                                val note = appNotes.firstOrNull { it.id == noteId }
                                notesFeatureState.openEditor(
                                    noteId = noteId,
                                    dateIso = note?.date ?: LocalDate.now().toString(),
                                    workplaceId = note?.workplaceId,
                                    shiftCode = note?.shiftCode
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            monthAudit = calendarMonthAudit,
                            monthHistoryItems = calendarMonthHistoryItems,
                            onOpenMonthCheck = { navigationState = navigationState.openScreen(AppScreen.APP_HEALTH_CHECK) },
                            templateMap = templateMap,
                            legendShiftTemplates = quickShiftTemplates,
                            shiftColors = shiftColors,
                            quickShiftTemplates = quickShiftTemplates,
                            systemStatusCodes = systemStatusCodes,
                            quickPickerOpen = calendarInteractionState.quickPickerOpen,
                            activeBrushCode = calendarInteractionState.activeBrushCode,
                            holidayMap = resolvedHolidayMap,
                            isLegendExpanded = calendarInteractionState.isLegendExpanded,
                            onToggleLegend = { calendarInteractionState.isLegendExpanded = !calendarInteractionState.isLegendExpanded },
                            onOpenColorSettings = { navigationState = navigationState.selectTab(BottomTab.SHIFTS) },
                            onToggleQuickPicker = { calendarInteractionState.quickPickerOpen = !calendarInteractionState.quickPickerOpen },
                            onCloseQuickPicker = { calendarInteractionState.quickPickerOpen = false },
                            pendingPatternRangeStartDate = pendingPatternRangeStartDate,
                            pendingPatternRangeEndDate = pendingPatternRangeEndDate,
                            onOpenPatternPreview = {
                                if (pendingPatternRangeStartDate != null && pendingPatternRangeEndDate != null) {
                                    patternWorkflowState.showPatternPreviewDialog = true
                                }
                            },
                            onSelectBrush = { code ->
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                calendarInteractionState.activeBrushCode = code
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onClearBrush = {
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                calendarInteractionState.activeBrushCode = BRUSH_CLEAR
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onDisableBrush = {
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                calendarInteractionState.activeBrushCode = null
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onAddNewShift = {
                                shiftFeatureState.openNewShift()
                                navigationState = navigationState.openScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onOpenPatternEditor = {
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                patternWorkflowState.showPatternQuickPicker = true
                                calendarInteractionState.quickPickerOpen = false
                            },
                            clearRangeModeActive = patternWorkflowState.clearRangeModeActive,
                            clearRangeStartDate = clearRangeStartDate,
                            pendingClearRangeStartDate = pendingClearRangeStartDate,
                            pendingClearRangeEndDate = pendingClearRangeEndDate,
                            onConfirmClearRange = {
                                if (pendingClearRangeStartDate != null && pendingClearRangeEndDate != null) {
                                    val rangeStart = pendingClearRangeStartDate.toString()
                                    val rangeEnd = pendingClearRangeEndDate.toString()
                                    scope.launch {
                                        ShiftAlarmScheduler.clearSuppressedAlarmsForRange(
                                            context,
                                            pendingClearRangeStartDate,
                                            pendingClearRangeEndDate
                                        )
                                        scheduleData.deleteShiftDays(rangeStart, rangeEnd)
                                        scheduleData.clearWorkplaceAssignments(
                                            startDate = pendingClearRangeStartDate,
                                            endDate = pendingClearRangeEndDate
                                        )
                                    }
                                    showInfoSnackbar("Диапазон очищен")
                                }
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onCancelClearRangeMode = {
                                patternWorkflowState.clearRangeModeActive = false
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                            },
                            onClearCurrentMonth = {
                                patternWorkflowState.showClearMonthConfirm = true
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onStartRangeClearMode = {
                                calendarInteractionState.activeBrushCode = null
                                patternWorkflowState.activePatternId = null
                                patternWorkflowState.patternRangeStartIso = null
                                patternWorkflowState.pendingPatternRangeStartIso = null
                                patternWorkflowState.pendingPatternRangeEndIso = null
                                patternWorkflowState.clearRangeModeActive = true
                                patternWorkflowState.clearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeStartIso = null
                                patternWorkflowState.pendingClearRangeEndIso = null
                                calendarInteractionState.quickPickerOpen = false
                            },
                            onClearAllCalendar = {
                                patternWorkflowState.showClearAllCalendarConfirm = true
                                calendarInteractionState.quickPickerOpen = false
                            },
                            showQuickEraser = appWorkflowSettings.showQuickEraser,
                            showQuickNormal = appWorkflowSettings.showQuickNormal,
                            showQuickCycle = appWorkflowSettings.showQuickCycle,
                            showQuickNewTemplate = appWorkflowSettings.showQuickNewTemplate,
                            showQuickClearMonth = appWorkflowSettings.showQuickClearMonth,
                            showQuickClearRange = appWorkflowSettings.showQuickClearRange,
                            showQuickClearAll = appWorkflowSettings.showQuickClearAll,
                            onEraseDate = { date ->
                                scope.launch {
                                    clearAllAssignmentsForDate(date)
                                }
                                appEventLogStore.add(
                                    title = "День очищен",
                                    message = "${formatDate(date)} · ${formatYearMonthLabel(YearMonth.from(date))}",
                                    category = "CALENDAR"
                                )
                            },
                            activePattern = activePattern,
                            patternRangeStartDate = patternRangeStartDate,
                            onCancelPatternMode = {
                                patternWorkflowState.activePatternId = null
                                patternWorkflowState.patternRangeStartIso = null
                            },
                            onDayClick = { date ->
                                if (YearMonth.from(date) != currentMonth) {
                                    currentMonth = YearMonth.from(date)
                                }
                                when {
                                    patternWorkflowState.clearRangeModeActive -> {
                                        val start = patternWorkflowState.clearRangeStartIso?.let { LocalDate.parse(it) }
                                        if (start == null) {
                                            patternWorkflowState.clearRangeStartIso = date.toString()
                                            patternWorkflowState.pendingClearRangeStartIso = null
                                            patternWorkflowState.pendingClearRangeEndIso = null
                                        } else {
                                            val rangeStart = minOf(start, date)
                                            val rangeEnd = maxOf(start, date)
                                            patternWorkflowState.pendingClearRangeStartIso = rangeStart.toString()
                                            patternWorkflowState.pendingClearRangeEndIso = rangeEnd.toString()
                                            patternWorkflowState.clearRangeStartIso = null
                                        }
                                    }

                                    activePattern != null -> {
                                        val start = patternWorkflowState.patternRangeStartIso?.let { LocalDate.parse(it) }

                                        if (start == null) {
                                            patternWorkflowState.patternRangeStartIso = date.toString()
                                        } else {
                                            val rangeStart = minOf(start, date)
                                            val rangeEnd = maxOf(start, date)

                                            patternWorkflowState.pendingPatternRangeStartIso = rangeStart.toString()
                                            patternWorkflowState.pendingPatternRangeEndIso = rangeEnd.toString()
                                            patternWorkflowState.showPatternPreviewDialog = true
                                            patternWorkflowState.patternRangeStartIso = null
                                        }
                                    }

                                    calendarInteractionState.activeBrushCode == null -> {
                                        calendarInteractionState.selectedDate = date
                                    }

                                    calendarInteractionState.activeBrushCode == BRUSH_CLEAR -> {
                                        scope.launch {
                                            clearAllAssignmentsForDate(date)
                                        }
                                        appEventLogStore.add(
                                            title = "День очищен",
                                            message = "${formatDate(date)} · ${formatYearMonthLabel(YearMonth.from(date))}",
                                            category = "CALENDAR"
                                        )
                                    }

                                    else -> {
                                        scope.launch {
                                            ShiftAlarmScheduler.clearSuppressedAlarmsForDate(context, date)
                                            if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                                                scheduleData.upsertShiftDay(
                                                    ShiftDayEntity(
                                                        date = date.toString(),
                                                        shiftCode = calendarInteractionState.activeBrushCode!!
                                                    )
                                                )
                                            } else {
                                                scheduleData.setWorkplaceShift(
                                                    workplaceId = activeWorkplaceId,
                                                    date = date,
                                                    shiftCode = calendarInteractionState.activeBrushCode
                                                )
                                            }
                                        }
                                        appEventLogStore.add(
                                            title = "Смена внесена",
                                            message = "${formatDate(date)} · ${stripWorkplaceScopeFromShiftCode(calendarInteractionState.activeBrushCode!!)} · ${formatYearMonthLabel(YearMonth.from(date))}",
                                            category = "CALENDAR"
                                        )
                                    }
                                }
                            },
                            onDayLongPress = { date ->
                                if (calendarInteractionState.activeBrushCode == null && activePattern == null && !patternWorkflowState.clearRangeModeActive) {
                                    if (YearMonth.from(date) != currentMonth) {
                                        currentMonth = YearMonth.from(date)
                                    }
                                    calendarInteractionState.selectedDate = null
                                    calendarInteractionState.dayAssignmentsPreviewDate = date
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.TODAY -> {
                        TodayTabScreen(
                            todaySummary = todayWorkSummary,
                            tomorrowSummary = tomorrowWorkSummary,
                            nextAlarmSummary = nextAlarmSummary,
                            paymentDates = paymentDates,
                            payroll = payroll,
                            todayShifts = todayShiftPreviews,
                            upcomingPayments = upcomingPaymentItems,
                            notes = todayNotes,
                            onAddNote = {
                                notesFeatureState.openEditor(
                                    noteId = null,
                                    dateIso = LocalDate.now().toString(),
                                    workplaceId = null,
                                    shiftCode = null
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            onEditNote = { noteId ->
                                val note = appNotes.firstOrNull { it.id == noteId }
                                notesFeatureState.openEditor(
                                    noteId = noteId,
                                    dateIso = note?.date ?: LocalDate.now().toString(),
                                    workplaceId = note?.workplaceId,
                                    shiftCode = note?.shiftCode
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            monthAudit = calendarMonthAudit,
                            onOpenCalendar = { navigationState = navigationState.selectTab(BottomTab.CALENDAR) },
                            onOpenAlarms = { navigationState = navigationState.selectTab(BottomTab.ALARMS) },
                            onOpenFinance = {
                                navigationState = navigationState
                                    .selectTab(BottomTab.FINANCE)
                                    .selectFinanceSubTab(FinanceSubTab.SUMMARY)
                            },
                            onOpenMonthCheck = { navigationState = navigationState.openScreen(AppScreen.APP_HEALTH_CHECK) },
                            todayLayoutSettings = todayLayoutSettings,
                            onChangeTodayLayoutSettings = { todayLayoutSettingsStore.save(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.ASSISTANT -> {
                        val activeAssistantWorkplaceName = workplaces.firstOrNull { it.id == activeWorkplaceId }?.name
                            ?: "Работа"
                        val workplaceSortIndex = workplaces
                            .mapIndexed { index, workplace -> workplace.id to index }
                            .toMap()
                        val assistantShiftOptions = shiftTemplates
                            .asSequence()
                            .filter { template -> template.active }
                            .filterNot { template -> isSystemStatusCode(template.code, systemStatusCodes) }
                            .sortedWith(
                                compareBy<ShiftTemplateEntity>(
                                    { workplaceIdFromShiftCode(it.code) != activeWorkplaceId },
                                    { workplaceSortIndex[workplaceIdFromShiftCode(it.code)] ?: Int.MAX_VALUE },
                                    { it.sortOrder }
                                )
                            )
                            .map { template ->
                            val workplaceId = workplaceIdFromShiftCode(template.code)
                            val timing = shiftAlarmTemplateConfigsByCode[template.code]
                            AssistantShiftOption(
                                code = template.code,
                                displayCode = stripWorkplaceScopeFromShiftCode(template.code),
                                title = template.title,
                                workplaceId = workplaceId,
                                workplaceName = workplaces.firstOrNull { it.id == workplaceId }?.name
                                    ?: activeAssistantWorkplaceName,
                                totalHours = template.totalHours,
                                breakHours = template.breakHours,
                                nightHours = template.nightHours,
                                startHour = timing?.startHour,
                                startMinute = timing?.startMinute,
                                endHour = timing?.endHour,
                                endMinute = timing?.endMinute
                            )
                        }.toList()
                        val assistantShiftOptionsByCode = assistantShiftOptions.associateBy { it.code }
                        val assistantScheduledShifts = allDayAssignmentsByDate
                            .flatMap { (date, assignments) ->
                                assignments.mapNotNull { assignment ->
                                    assistantShiftOptionsByCode[assignment.shiftCode]
                                        ?.let { shift -> AssistantScheduledShift(date = date, shift = shift) }
                                }
                            }
                            .sortedBy { it.date }
                        AssistantTabScreen(
                            activeWorkplaceName = activeAssistantWorkplaceName,
                            activeWorkplaceId = activeWorkplaceId,
                            shiftOptions = assistantShiftOptions,
                            scheduledShifts = assistantScheduledShifts,
                            todaySummary = todayWorkSummary,
                            tomorrowSummary = tomorrowWorkSummary,
                            nextAlarmSummary = nextAlarmSummary,
                            upcomingPayments = upcomingPaymentItems,
                            financeContext = AssistantFinanceContext(
                                periodLabel = effectivePayrollPeriodLabel,
                                grossTotal = payroll.grossTotal,
                                netTotal = payroll.netTotal,
                                ndfl = payroll.ndfl,
                                netAdvance = payroll.netAdvanceAfterDeductions,
                                netSalary = payroll.netSalaryAfterDeductions,
                                actualAdvance = appWorkflowSettings.actualAdvanceNet,
                                actualSalary = appWorkflowSettings.actualSalaryNet,
                                paymentDifferenceToleranceRub = appWorkflowSettings.paymentDifferenceToleranceRub
                            ),
                            aiSettings = assistantAiSettings,
                            onAiSettingsChange = { updated -> assistantAiSettingsStore.save(updated) },
                            onAssignShift = { date, shift ->
                                scope.launch {
                                    ShiftAlarmScheduler.clearSuppressedAlarmsForDate(context, date)
                                    if (shift.workplaceId == WORKPLACE_MAIN_ID) {
                                        scheduleData.upsertShiftDay(
                                            ShiftDayEntity(
                                                date = date.toString(),
                                                shiftCode = shift.code
                                            )
                                        )
                                    } else {
                                        scheduleData.setWorkplaceShift(
                                            workplaceId = shift.workplaceId,
                                            date = date,
                                            shiftCode = shift.code
                                        )
                                    }
                                    appEventLogStore.add(
                                        title = "ИИ назначил смену",
                                        message = "${formatDate(date)} · ${shift.displayCode} · ${shift.workplaceName}",
                                        category = "ASSISTANT"
                                    )
                                }
                                showInfoSnackbar("Смена ${shift.displayCode} назначена на ${formatDate(date)}")
                            },
                            onConfigureShiftAlarm = { shift, hour, minute, minutesBefore ->
                                val template = shiftTemplates.firstOrNull { it.code == shift.code }
                                if (template == null) {
                                    showInfoSnackbar("Шаблон смены не найден")
                                } else {
                                    val currentConfig = shiftAlarmSettings.templateConfigs
                                        .firstOrNull { it.shiftCode == shift.code }
                                        ?: defaultShiftTemplateAlarmConfig(template)
                                    val trigger = if (hour != null && minute != null) {
                                        hour to minute
                                    } else {
                                        resolveAlarmClockFromShiftStart(
                                            startHour = currentConfig.startHour,
                                            startMinute = currentConfig.startMinute,
                                            minutesBefore = minutesBefore ?: if (template.nightHours > 0.0) 90 else 60
                                        )
                                    }
                                    val behavior = shiftAlarmSettings.behavior
                                    val alarm = ShiftAlarmConfig(
                                        title = "",
                                        manualTitle = false,
                                        triggerHour = trigger.first,
                                        triggerMinute = trigger.second,
                                        volumePercent = 100,
                                        soundUri = behavior.defaultSoundUri,
                                        soundLabel = behavior.defaultSoundLabel,
                                        enabled = true,
                                        vibrationEnabled = behavior.vibrationEnabled,
                                        vibrationType = behavior.vibrationType,
                                        vibrationDurationSeconds = behavior.vibrationDurationSeconds,
                                        customVibrationPattern = behavior.customVibrationPattern,
                                        snoozeIntervalMinutes = behavior.snoozeIntervalMinutes,
                                        snoozeCountLimit = behavior.snoozeCountLimit,
                                        ringDurationSeconds = behavior.ringDurationSeconds,
                                        rampUpDurationSeconds = behavior.rampUpDurationSeconds
                                    )
                                    val updatedConfig = currentConfig.copy(
                                        enabled = true,
                                        alarms = (currentConfig.alarms + alarm)
                                            .distinctBy { it.triggerHour to it.triggerMinute }
                                    )
                                    val updatedSettings = shiftAlarmSettings.copy(
                                        enabled = true,
                                        templateConfigs = (
                                            shiftAlarmSettings.templateConfigs
                                                .filterNot { it.shiftCode == shift.code } + updatedConfig
                                            ).sortedBy { stripWorkplaceScopeFromShiftCode(it.shiftCode) }
                                    )
                                    scope.launch {
                                        alarmRuntimeState.lastRescheduleResult = saveAndRescheduleShiftAlarms(
                                            store = shiftAlarmStore,
                                            context = context,
                                            settings = updatedSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap,
                                            mirrorToSystemClockApp = false,
                                            allowSystemClockUiFallback = false
                                        )
                                        appEventLogStore.add(
                                            title = "ИИ добавил будильник",
                                            message = "${shift.displayCode} · ${formatClockHm(trigger.first, trigger.second)}",
                                            category = "ASSISTANT"
                                        )
                                    }
                                    showInfoSnackbar("Будильник ${shift.displayCode}: ${formatClockHm(trigger.first, trigger.second)}")
                                }
                            },
                            onClearDay = { date ->
                                scope.launch {
                                    clearAllAssignmentsForDate(date)
                                    appEventLogStore.add(
                                        title = "ИИ очистил день",
                                        message = formatDate(date),
                                        category = "ASSISTANT"
                                    )
                                }
                                showInfoSnackbar("День очищен: ${formatDate(date)}")
                            },
                            onCreateNote = { date, title, body ->
                                appNotesStore.save(
                                    AppNote(
                                        date = date.toString(),
                                        title = title,
                                        body = body,
                                        colorHex = "#DDF6EE"
                                    )
                                )
                                appEventLogStore.add(
                                    title = "ИИ создал заметку",
                                    message = "${formatDate(date)} · $title",
                                    category = "ASSISTANT"
                                )
                                showInfoSnackbar("Заметка создана")
                            },
                            onOpenTab = { tab ->
                                navigationState = if (tab == BottomTab.FINANCE) {
                                    navigationState
                                        .selectTab(tab)
                                        .selectFinanceSubTab(FinanceSubTab.SUMMARY)
                                } else {
                                    navigationState.selectTab(tab)
                                }
                            },
                            onShowMessage = showInfoSnackbar,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.NOTES -> {
                        AppNotesTabScreen(
                            notes = appNotes,
                            onAddNote = { date ->
                                notesFeatureState.openEditor(
                                    noteId = null,
                                    dateIso = date.toString(),
                                    workplaceId = null,
                                    shiftCode = null
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            onEditNote = { noteId ->
                                val note = appNotes.firstOrNull { it.id == noteId }
                                notesFeatureState.openEditor(
                                    noteId = noteId,
                                    dateIso = note?.date ?: LocalDate.now().toString(),
                                    workplaceId = note?.workplaceId,
                                    shiftCode = note?.shiftCode
                                )
                                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.FINANCE -> {
                        FinanceTab(
                            selectedSubTab = financeSubTab,
                            onSelectSubTab = { tab ->
                                navigationState = navigationState.selectFinanceSubTab(tab)
                            },
                            summaryState = FinanceSummaryState(
                                periodLabel = effectivePayrollPeriodLabel,
                                workplaceLabel = "Работа: $selectedPayrollWorkplaceName",
                                payroll = payroll,
                                detailedShiftStats = detailedShiftStats,
                                paymentDates = paymentDates,
                                payMode = effectivePayrollSettings.payMode,
                                paymentScheduleMode = effectivePayrollSettings.paymentScheduleMode,
                                todaySummary = todayWorkSummary,
                                tomorrowSummary = tomorrowWorkSummary,
                                nextAlarmSummary = nextAlarmSummary,
                                actualAdvanceNet = appWorkflowSettings.actualAdvanceNet,
                                actualSalaryNet = appWorkflowSettings.actualSalaryNet,
                                paymentDifferenceToleranceRub = appWorkflowSettings.paymentDifferenceToleranceRub,
                                onSaveActualPayments = { advance, salary ->
                                    appWorkflowSettingsStore.save(
                                        appWorkflowSettings.copy(
                                            actualAdvanceNet = advance,
                                            actualSalaryNet = salary
                                        )
                                    )
                                    showInfoSnackbar("Фактические выплаты сохранены")
                                }
                            ),
                            payrollContent = {
                                PayrollTab(
                                    state = PayrollTabState(
                                        currentMonth = currentMonth,
                                        periodMode = payrollPeriodMode,
                                        selectedWorkplaceId = financeFeatureState.payrollWorkplaceFilterId,
                                        workplaceOptions = payrollWorkplaceOptions,
                                        periodStartDate = payrollPeriodStartDate,
                                        periodEndDate = payrollPeriodEndDate,
                                        periodLabel = effectivePayrollPeriodLabel,
                                        periodFileLabel = effectivePayrollPeriodFileLabel,
                                        summary = summary,
                                        payroll = payroll,
                                        payrollDetailedResult = payrollDetailedResult,
                                        annualOvertime = annualOvertime,
                                        paymentDates = paymentDates,
                                        payMode = effectivePayrollSettings.payMode,
                                        paymentScheduleMode = effectivePayrollSettings.paymentScheduleMode,
                                        housingPaymentLabel = payrollSettings.housingPaymentLabel,
                                        detailedShiftStats = detailedShiftStats,
                                        isSummaryExpanded = financeFeatureState.isSummaryExpanded,
                                        reportVisibilitySettings = reportVisibilitySettings
                                    ),
                                    actions = PayrollTabActions(
                                        onChangePeriodMode = { mode ->
                                            financeFeatureState.changePeriodMode(mode, payrollPeriodStartDate, payrollPeriodEndDate)
                                        },
                                        onChangeWorkplace = { workplaceId ->
                                            financeFeatureState.selectWorkplace(workplaceId)
                                        },
                                        onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                                        onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                                        onPrevYear = { financeFeatureState.previousYear() },
                                        onNextYear = { financeFeatureState.nextYear() },
                                        onPickYear = { year -> financeFeatureState.selectYear(year) },
                                        onShiftRangeBackward = {
                                            val daysInRange =
                                                kotlin.math.max(
                                                    1L,
                                                    java.time.temporal.ChronoUnit.DAYS.between(
                                                        payrollPeriodStartDate,
                                                        payrollPeriodEndDate
                                                    ) + 1L
                                                )
                                            financeFeatureState.shiftRange(payrollPeriodStartDate, payrollPeriodEndDate, -daysInRange)
                                        },
                                        onShiftRangeForward = {
                                            val daysInRange =
                                                kotlin.math.max(
                                                    1L,
                                                    java.time.temporal.ChronoUnit.DAYS.between(
                                                        payrollPeriodStartDate,
                                                        payrollPeriodEndDate
                                                    ) + 1L
                                                )
                                            financeFeatureState.shiftRange(payrollPeriodStartDate, payrollPeriodEndDate, daysInRange)
                                        },
                                        onPickRangeStart = { date ->
                                            financeFeatureState.pickRangeStart(date, payrollPeriodEndDate)
                                        },
                                        onPickRangeEnd = { date ->
                                            financeFeatureState.pickRangeEnd(date, payrollPeriodStartDate)
                                        },
                                        onToggleSummary = { financeFeatureState.toggleSummary() },
                                        onOpenSettings = {
                                            financeFeatureState.openSettingsFor(
                                                if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
                                                    WORKPLACE_MAIN_ID
                                                } else {
                                                    financeFeatureState.payrollWorkplaceFilterId
                                                }
                                            )
                                            navigationState = navigationState.openScreen(AppScreen.PAYROLL_SETTINGS)
                                        },
                                        onOpenDiagnostics = { navigationState = navigationState.openScreen(AppScreen.PAYROLL_DIAGNOSTICS) },
                                        onOpenVisibilitySettings = { navigationState = navigationState.openScreen(AppScreen.REPORT_VISIBILITY_SETTINGS) },
                                        onExportSheetPdf = { periodLabel, fileLabel, detailedResult ->
                                            financeFeatureState.pendingReportPdfBytes = buildPayrollSheetPdf(
                                                periodLabel = periodLabel,
                                                payrollDetailedResult = detailedResult
                                            )
                                            financeFeatureState.pendingReportPdfFileName = "payroll_sheet_$fileLabel.pdf"
                                            reportHistoryStore.add(
                                                ReportHistoryItem(
                                                    title = "Расчётный лист",
                                                    periodLabel = periodLabel,
                                                    workplaceLabel = selectedPayrollWorkplaceName,
                                                    gross = detailedResult.summary.grossTotal,
                                                    ndfl = detailedResult.summary.ndfl,
                                                    net = detailedResult.summary.netTotal,
                                                    fileName = financeFeatureState.pendingReportPdfFileName,
                                                    format = "pdf"
                                                )
                                            )
                                            appEventLogStore.add(
                                                title = "Экспортирован расчётный лист",
                                                message = periodLabel,
                                                category = "REPORT"
                                            )
                                            reportPdfLauncher.launch(financeFeatureState.pendingReportPdfFileName)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            paymentsContent = {
                                PaymentsTab(
                                    currentMonth = currentMonth,
                                    onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                                    onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                                    payroll = payroll,
                                    annualOvertime = annualOvertime,
                                    paymentDates = paymentDates,
                                    payMode = effectivePayrollSettings.payMode,
                                    paymentScheduleMode = effectivePayrollSettings.paymentScheduleMode,
                                    housingPaymentLabel = payrollSettings.housingPaymentLabel,
                                    additionalPayments = additionalPaymentsForPayroll,
                                    resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                                    detailedShiftStats = detailedShiftStats,
                                    onAddPayment = {
                                        financeFeatureState.openNewPayment(
                                            if (financeFeatureState.payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
                                                WORKPLACE_MAIN_ID
                                            } else {
                                                financeFeatureState.payrollWorkplaceFilterId
                                            }
                                        )
                                    },
                                    onEditPayment = { payment ->
                                        financeFeatureState.openPayment(payment.id, normalizeWorkplaceId(payment.workplaceId))
                                    },
                                    onDeletePayment = { payment ->
                                        additionalPaymentsStore.deleteById(payment.id)
                                        showUndoSnackbar("Начисление удалено") {
                                            additionalPaymentsStore.addOrUpdate(payment)
                                        }
                                    },
                                    onOpenMonthlyReport = {
                                        navigationState = navigationState.openScreen(AppScreen.MONTHLY_REPORT)
                                    },
                                    onOpenVisibilitySettings = {
                                        navigationState = navigationState.openScreen(AppScreen.REPORT_VISIBILITY_SETTINGS)
                                    },
                                    visibilitySettings = reportVisibilitySettings,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    BottomTab.ALARMS -> {
                        ShiftAlarmsTab(
                            state = ShiftAlarmsTabState(
                                settings = shiftAlarmSettings,
                                shiftTemplates = alarmEligibleTemplates,
                                lastRescheduleResult = alarmRuntimeState.lastRescheduleResult,
                                upcomingAlarms = upcomingShiftAlarms,
                                canScheduleExactAlarms = canScheduleExactShiftAlarms,
                                notificationPermissionGranted = shiftAlarmNotificationPermissionGranted,
                                fullScreenIntentPermissionGranted = shiftAlarmFullScreenIntentPermissionGranted,
                                alarmSwipeDuplicateEnabled = appWorkflowSettings.alarmSwipeDuplicateEnabled,
                                alarmSwipeDeleteEnabled = appWorkflowSettings.alarmSwipeDeleteEnabled
                            ),
                            actions = ShiftAlarmsTabActions(
                                onSave = { newSettings ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        alarmRuntimeState.lastRescheduleResult = saveAndRescheduleShiftAlarms(
                                            store = shiftAlarmStore,
                                            context = context,
                                            settings = newSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap,
                                            mirrorToSystemClockApp = false,
                                            allowSystemClockUiFallback = false,
                                            restoreSuppressed = true
                                        )
                                    }
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onOpenExactAlarmSettings = {
                                    openExactAlarmPermissionSettings(context)
                                },
                                onOpenFullScreenIntentSettings = {
                                    openFullScreenIntentPermissionSettings(context)
                                },
                                onOpenSystemClock = {
                                    startInAppAlarmPreview(
                                        context = context,
                                        behavior = shiftAlarmSettings.behavior
                                    )
                                },
                                onRescheduleNow = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        alarmRuntimeState.lastRescheduleResult = rescheduleShiftAlarms(
                                            context = context,
                                            settings = shiftAlarmSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap,
                                            mirrorToSystemClockApp = false,
                                            allowSystemClockUiFallback = false
                                        )
                                    }
                                },
                                onCancelUpcomingAlarm = { alarm ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        if (ShiftAlarmScheduler.suppressScheduledAlarm(context, alarm.alarmKey)) {
                                            alarmRuntimeState.lastRescheduleResult = rescheduleShiftAlarms(
                                                context = context,
                                                settings = shiftAlarmSettings,
                                                savedDays = savedDays,
                                                templateMap = templateMap,
                                                mirrorToSystemClockApp = false,
                                                allowSystemClockUiFallback = false
                                            )
                                            alarmRuntimeState.refreshPermissions()
                                            showInfoSnackbar("Будильник пропущен: ${alarm.title}")
                                        }
                                    }
                                },
                                onCancelUpcomingAlarms = { alarms ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        val count = ShiftAlarmScheduler.suppressScheduledAlarms(
                                            context = context,
                                            alarmKeys = alarms.map { it.alarmKey }
                                        )
                                        if (count > 0) {
                                            alarmRuntimeState.lastRescheduleResult = rescheduleShiftAlarms(
                                                context = context,
                                                settings = shiftAlarmSettings,
                                                savedDays = savedDays,
                                                templateMap = templateMap,
                                                mirrorToSystemClockApp = false,
                                                allowSystemClockUiFallback = false
                                            )
                                            alarmRuntimeState.refreshPermissions()
                                            showInfoSnackbar("Пропущено будильников: $count")
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
                                templates = editableShiftTemplatesForActiveWorkplace,
                                systemStatusCodes = systemStatusCodes,
                                specialRules = shiftSpecialRulesSnapshot,
                                patterns = patternTemplates,
                                workplaces = workplaces,
                                activeWorkplaceId = activeWorkplaceId,
                                shiftSwipeDuplicateEnabled = appWorkflowSettings.shiftSwipeDuplicateEnabled,
                                shiftSwipeDeleteEnabled = appWorkflowSettings.shiftSwipeDeleteEnabled
                            ),
                            actions = TemplatesScreenActions(
                                onModeChange = { shiftFeatureState.setMode(it) },
                                onBack = { navigationState = navigationState.selectTab(BottomTab.CALENDAR) },
                                onSwitchWorkplace = { activeWorkplaceId = it },
                                onOpenManageWorkplaces = { settingsFeatureState.openWorkplaceRename() },
                                onAddShift = {
                                    shiftFeatureState.openNewShift()
                                    navigationState = navigationState.openScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                                },
                                onAddSystemStatus = {
                                    shiftFeatureState.openNewSystemStatus()
                                    navigationState = navigationState.openScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                                },
                                onEditShift = { template ->
                                    shiftFeatureState.openExistingShift(
                                        template.code,
                                        isSystemStatusCode(template.code, systemStatusCodes)
                                    )
                                    navigationState = navigationState.openScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                                },
                                onDuplicateShift = { template ->
                                    scope.launch {
                                        val templatesInWorkplace = shiftTemplates.filter {
                                            isShiftCodeForWorkplace(it.code, activeWorkplaceId)
                                        }
                                        val existingCodes = templatesInWorkplace
                                            .map { stripWorkplaceScopeFromShiftCode(it.code) }
                                            .toSet()
                                        val baseCode = stripWorkplaceScopeFromShiftCode(template.code).ifBlank { "S" }
                                        var suffix = 2
                                        var duplicatedBaseCode = "$baseCode$suffix"
                                        while (duplicatedBaseCode in existingCodes) {
                                            suffix += 1
                                            duplicatedBaseCode = "$baseCode$suffix"
                                        }
                                        val duplicatedCode = workplaceScopedShiftCode(activeWorkplaceId, duplicatedBaseCode)
                                        val duplicatedLabelCode = if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                                            duplicatedCode
                                        } else {
                                            duplicatedBaseCode
                                        }

                                        val duplicatedTemplate = template.copy(
                                            code = duplicatedCode,
                                            title = "${template.title} (копия)",
                                            sortOrder = (shiftTemplates.maxOfOrNull { it.sortOrder } ?: template.sortOrder) + 10
                                        )
                                        scheduleData.upsertShiftTemplate(duplicatedTemplate)

                                        val duplicatedColor = shiftColors[template.code]
                                            ?: parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                                        saveShiftColor(
                                            shiftColors = shiftColors,
                                            shiftColorsPrefs = shiftColorsPrefs,
                                            context = context,
                                            key = duplicatedCode,
                                            colorValue = duplicatedColor
                                        )

                                        val sourceRule = shiftSpecialRulesSnapshot[template.code]
                                            ?: defaultShiftSpecialRule(template.isWeekendPaid)
                                        saveShiftSpecialRule(
                                            shiftSpecialRules = shiftSpecialRules,
                                            shiftSpecialPrefs = shiftSpecialPrefs,
                                            code = duplicatedCode,
                                            rule = sourceRule
                                        )

                                        val sourceAlarmConfig = shiftAlarmSettings.templateConfigs.firstOrNull { it.shiftCode == template.code }
                                            ?: defaultShiftTemplateAlarmConfig(template)
                                        shiftAlarmStore.upsertTemplateConfig(sourceAlarmConfig.copy(shiftCode = duplicatedCode))

                                        showInfoSnackbar("Смена \"$duplicatedLabelCode\" создана")
                                    }
                                },
                                onDeleteShift = { template ->
                                    scope.launch {
                                        val linkedDays = savedDays.filter { it.shiftCode == template.code }
                                        val removedExtraAssignments = scheduleData.removeShiftCode(template.code)
                                        val existingColor = shiftColors[template.code]
                                            ?: parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                                        val existingRule = shiftSpecialRulesSnapshot[template.code]
                                        val existingAlarm = shiftAlarmSettings.templateConfigs.firstOrNull { it.shiftCode == template.code }

                                        scheduleData.deleteShiftTemplate(template)
                                        linkedDays.forEach { day ->
                                            scheduleData.deleteShiftDay(day.date)
                                        }
                                        shiftColorsPrefs.edit { remove(template.code) }
                                        shiftColors.remove(template.code)
                                        removeShiftSpecialRule(
                                            shiftSpecialRules = shiftSpecialRules,
                                            shiftSpecialPrefs = shiftSpecialPrefs,
                                            code = template.code
                                        )
                                        shiftAlarmStore.removeTemplateConfig(template.code)

                                        val deletedDisplayCode = if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                                            template.code
                                        } else {
                                            stripWorkplaceScopeFromShiftCode(template.code)
                                        }

                                        showUndoSnackbar("Смена \"$deletedDisplayCode\" удалена") {
                                            scope.launch {
                                                scheduleData.upsertShiftTemplate(template)
                                                linkedDays.forEach { day ->
                                                    scheduleData.upsertShiftDay(day)
                                                }
                                                scheduleData.restoreAssignments(removedExtraAssignments)
                                                saveShiftColor(
                                                    shiftColors = shiftColors,
                                                    shiftColorsPrefs = shiftColorsPrefs,
                                                    context = context,
                                                    key = template.code,
                                                    colorValue = existingColor
                                                )
                                                if (existingRule != null) {
                                                    saveShiftSpecialRule(
                                                        shiftSpecialRules = shiftSpecialRules,
                                                        shiftSpecialPrefs = shiftSpecialPrefs,
                                                        code = template.code,
                                                        rule = existingRule
                                                    )
                                                }
                                                if (existingAlarm != null) {
                                                    shiftAlarmStore.upsertTemplateConfig(existingAlarm)
                                                } else {
                                                    shiftAlarmStore.upsertTemplateConfig(defaultShiftTemplateAlarmConfig(template))
                                                }
                                            }
                                        }
                                    }
                                },
                                onReorderShifts = { orderedTemplates ->
                                    scope.launch {
                                        orderedTemplates.forEachIndexed { index, template ->
                                            scheduleData.upsertShiftTemplate(template.copy(sortOrder = (index + 1) * 10))
                                        }
                                        appEventLogStore.add(
                                            title = "Порядок смен изменён",
                                            message = "Обновлён порядок шаблонов: ${orderedTemplates.size}",
                                            category = "SHIFTS"
                                        )
                                    }
                                },
                                onAddPattern = {
                                    patternWorkflowState.editingPatternId = null
                                    patternWorkflowState.showPatternEditDialog = true
                                },
                                onEditPattern = { pattern ->
                                    patternWorkflowState.editingPatternId = pattern.id
                                    patternWorkflowState.showPatternEditDialog = true
                                },
                                onApplyPattern = { pattern ->
                                    patternWorkflowState.applyingPatternId = pattern.id
                                    patternWorkflowState.showPatternApplyDialog = true
                                },
                                onDeletePattern = { pattern ->
                                    patternTemplatesStore.deleteById(pattern.id)
                                    showUndoSnackbar("Чередование удалено") {
                                        patternTemplatesStore.addOrUpdate(pattern)
                                    }
                                }
                            )
                        )
                    }
                    BottomTab.SETTINGS -> {
                        SettingsTab(
                            payrollSettings = payrollSettings,
                            appearanceSummary = appearanceSettingsSummary(appearanceSettings),
                            currentProfileLabel = activeProfileName,
                            additionalPaymentsCount = additionalPayments.size,
                            deductionsCount = deductions.size,
                            onOpenDeductions = {
                                financeFeatureState.openSettingsFor(activeWorkplaceId)
                                navigationState = navigationState.openScreen(AppScreen.DEDUCTIONS)
                            },
                            manualHolidayCount = manualHolidayRecords.size,
                            isHolidaySyncing = settingsFeatureState.isHolidaySyncing,
                            holidaySyncMessage = settingsFeatureState.holidaySyncMessage,
                            applyShortDayReduction = payrollSettings.applyShortDayReduction,
                            onOpenPayrollSettings = {
                                financeFeatureState.openSettingsFor(activeWorkplaceId)
                                navigationState = navigationState.openScreen(AppScreen.PAYROLL_SETTINGS)
                            },
                            onOpenAppearanceSettings = { navigationState = navigationState.openScreen(AppScreen.APPEARANCE_SETTINGS) },
                            onOpenReportVisibilitySettings = { navigationState = navigationState.openScreen(AppScreen.REPORT_VISIBILITY_SETTINGS) },
                            onOpenPayments = {
                                financeFeatureState.openSettingsFor(activeWorkplaceId)
                                navigationState = navigationState.openScreen(AppScreen.ADDITIONAL_PAYMENTS)
                            },
                            onOpenCurrentParameters = { navigationState = navigationState.openScreen(AppScreen.CURRENT_PARAMETERS) },
                            onOpenManualHolidays = { navigationState = navigationState.openScreen(AppScreen.MANUAL_HOLIDAYS) },
                            onOpenBackupRestore = { navigationState = navigationState.openScreen(AppScreen.BACKUP_RESTORE) },
                            onOpenQuickActionsSettings = { navigationState = navigationState.openScreen(AppScreen.QUICK_ACTIONS_SETTINGS) },
                            onOpenQuickStart = { navigationState = navigationState.openScreen(AppScreen.QUICK_START_GUIDE) },
                            onOpenReportCenter = { navigationState = navigationState.openScreen(AppScreen.REPORT_CENTER) },
                            onOpenHealthCheck = { navigationState = navigationState.openScreen(AppScreen.APP_HEALTH_CHECK) },
                            onOpenEventLog = { navigationState = navigationState.openScreen(AppScreen.APP_EVENT_LOG) },
                            onOpenReportHistory = { navigationState = navigationState.openScreen(AppScreen.REPORT_HISTORY) },
                            onOpenExcelImport = { navigationState = navigationState.openScreen(AppScreen.EXCEL_IMPORT) },
                            onOpenWidgetSettings = { navigationState = navigationState.openScreen(AppScreen.WIDGET_SETTINGS) },
                            onOpenProfiles = { navigationState = navigationState.openScreen(AppScreen.PROFILES) },
                            onChangeApplyShortDayReduction = { enabled ->
                                scope.launch {
                                    val updatedSettings = payrollSettings.copy(
                                        applyShortDayReduction = enabled
                                    )
                                    if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                                        payrollSettingsStore.save(updatedSettings)
                                    } else {
                                        val updated = workplacePayrollSettingsState.settingsByWorkplaceId.toMutableMap()
                                        updated[activeWorkplaceId] = updatedSettings
                                        workplacePayrollSettingsStore.save(
                                            WorkplacePayrollSettingsState(
                                                settingsByWorkplaceId = updated
                                            )
                                        )
                                    }
                                }
                            },
                            onSyncProductionCalendar = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lifecycleOwner.lifecycleScope.launch {
                                    settingsFeatureState.startHolidaySync()
                                    settingsFeatureState.updateHolidaySyncMessage("Проверка календаря ${currentMonth.year}...")
                                    try {
                                        val hasFederalYear = holidays.any { it.date.startsWith("${currentMonth.year}-") }
                                        val result = checkAndSyncFederalCalendarIfChanged(
                                            holidaySyncRepository = holidaySyncRepository,
                                            prefs = calendarSyncPrefs,
                                            year = currentMonth.year,
                                            hasLocalYear = hasFederalYear,
                                            forceNetworkCheck = true
                                        )
                                        settingsFeatureState.updateHolidaySyncMessage(result.message)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        settingsFeatureState.updateHolidaySyncMessage("Ошибка обновления: ${e.message ?: "неизвестно"}")
                                    } finally {
                                        settingsFeatureState.finishHolidaySync()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
        }
    }
    AnimatedFullscreenOverlay(visible = AppScreen.MONTHLY_REPORT in navigationState.screenStack) {
        MonthlyReportScreen(
            currentMonth = currentMonth,
            payrollSettings = effectivePayrollSettings,
            payroll = payroll,
            annualOvertime = annualOvertime,
            paymentDates = paymentDates,
            housingPaymentLabel = payrollSettings.housingPaymentLabel,
            additionalPayments = additionalPaymentsForPayroll,
            resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
            detailedShiftStats = detailedShiftStats,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.MONTHLY_REPORT) },
            onExportCsv = {
                financeFeatureState.pendingReportCsvContent = buildMonthlyReportCsv(
                    currentMonth = currentMonth,
                    payrollSettings = effectivePayrollSettings,
                    payroll = payroll,
                    annualOvertime = annualOvertime,
                    paymentDates = paymentDates,
                    housingPaymentLabel = payrollSettings.housingPaymentLabel,
                    additionalPayments = additionalPaymentsForPayroll,
                    resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                    detailedShiftStats = detailedShiftStats
                )
                financeFeatureState.pendingReportCsvFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.csv"
                reportHistoryStore.add(
                    ReportHistoryItem(
                        title = "Месячный отчёт",
                        periodLabel = formatYearMonthLabel(currentMonth),
                        workplaceLabel = selectedPayrollWorkplaceName,
                        gross = payroll.grossTotal,
                        ndfl = payroll.ndfl,
                        net = payroll.netTotal,
                        fileName = financeFeatureState.pendingReportCsvFileName,
                        format = "csv"
                    )
                )
                appEventLogStore.add(
                    title = "Экспортирован CSV-отчёт",
                    message = formatYearMonthLabel(currentMonth),
                    category = "REPORT"
                )
                reportCsvLauncher.launch(financeFeatureState.pendingReportCsvFileName)
            },
            onExportPdf = {
                financeFeatureState.pendingReportPdfBytes = buildMonthlyReportPdf(
                    currentMonth = currentMonth,
                    payrollSettings = effectivePayrollSettings,
                    payroll = payroll,
                    annualOvertime = annualOvertime,
                    paymentDates = paymentDates,
                    housingPaymentLabel = payrollSettings.housingPaymentLabel,
                    additionalPayments = additionalPaymentsForPayroll,
                    resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                    detailedShiftStats = detailedShiftStats
                )
                financeFeatureState.pendingReportPdfFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.pdf"
                reportHistoryStore.add(
                    ReportHistoryItem(
                        title = "Месячный отчёт",
                        periodLabel = formatYearMonthLabel(currentMonth),
                        workplaceLabel = selectedPayrollWorkplaceName,
                        gross = payroll.grossTotal,
                        ndfl = payroll.ndfl,
                        net = payroll.netTotal,
                        fileName = financeFeatureState.pendingReportPdfFileName,
                        format = "pdf"
                    )
                )
                appEventLogStore.add(
                    title = "Экспортирован PDF-отчёт",
                    message = formatYearMonthLabel(currentMonth),
                    category = "REPORT"
                )
                reportPdfLauncher.launch(financeFeatureState.pendingReportPdfFileName)
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.APP_HEALTH_CHECK in navigationState.screenStack) {
        AppHealthCheckScreen(
            items = appHealthItems,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.APP_HEALTH_CHECK) },
            onRunMonthCheck = {
                appEventLogStore.add(
                    title = "Проверка приложения выполнена",
                    message = "Пустых дней: ${calendarMonthAudit.emptyDayCount}; пересечений: ${calendarMonthAudit.overlappingWorkDayCount}.",
                    category = "HEALTH"
                )
                showInfoSnackbar("Проверка выполнена")
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.APP_EVENT_LOG in navigationState.screenStack) {
        AppEventLogScreen(
            events = appEventLogItems,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.APP_EVENT_LOG) },
            onClear = {
                appEventLogStore.clear()
                showInfoSnackbar("Журнал очищен")
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.REPORT_HISTORY in navigationState.screenStack) {
        ReportHistoryScreen(
            items = reportHistoryItems,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.REPORT_HISTORY) },
            onClear = {
                reportHistoryStore.clear()
                showInfoSnackbar("История отчётов очищена")
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.QUICK_ACTIONS_SETTINGS in navigationState.screenStack) {
        QuickActionsSettingsScreen(
            settings = appWorkflowSettings,
            onChange = { updated -> appWorkflowSettingsStore.save(updated) },
            onBack = { navigationState = navigationState.closeScreen(AppScreen.QUICK_ACTIONS_SETTINGS) }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.QUICK_START_GUIDE in navigationState.screenStack) {
        QuickStartGuideScreen(
            shiftTemplateCount = shiftTemplates.size,
            scheduledDaysCount = savedDays.size + workAssignmentsState.extraAssignmentsByDate.values.sumOf { it.size },
            payrollSettings = payrollSettings,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.QUICK_START_GUIDE) },
            onDismissGuide = {
                appWorkflowSettingsStore.save(appWorkflowSettings.copy(quickStartDismissed = true))
                navigationState = navigationState.closeScreen(AppScreen.QUICK_START_GUIDE)
            },
            onOpenShifts = {
                navigationState = navigationState
                    .selectTab(BottomTab.SHIFTS)
                    .replaceScreen(
                        from = AppScreen.QUICK_START_GUIDE,
                        to = AppScreen.SHIFT_TEMPLATE_EDITOR
                    )
                shiftFeatureState.openNewShift()
            },
            onOpenCalendar = {
                navigationState = navigationState.closeScreen(AppScreen.QUICK_START_GUIDE)
                navigationState = navigationState.selectTab(BottomTab.CALENDAR)
            },
            onOpenPayrollSettings = {
                financeFeatureState.openSettingsFor(activeWorkplaceId)
                navigationState = navigationState.replaceScreen(
                    from = AppScreen.QUICK_START_GUIDE,
                    to = AppScreen.PAYROLL_SETTINGS
                )
            },
            onOpenAlarms = {
                navigationState = navigationState.closeScreen(AppScreen.QUICK_START_GUIDE)
                navigationState = navigationState.selectTab(BottomTab.ALARMS)
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.REPORT_CENTER in navigationState.screenStack) {
        ReportCenterScreen(
            historyItems = reportHistoryItems,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.REPORT_CENTER) },
            onOpenMonthlyReport = {
                navigationState = navigationState.replaceScreen(
                    from = AppScreen.REPORT_CENTER,
                    to = AppScreen.MONTHLY_REPORT
                )
            },
            onOpenPayrollPdf = {
                financeFeatureState.pendingReportPdfBytes = buildPayrollSheetPdf(
                    periodLabel = effectivePayrollPeriodLabel,
                    payrollDetailedResult = payrollDetailedResult
                )
                financeFeatureState.pendingReportPdfFileName = "payroll_sheet_$effectivePayrollPeriodFileLabel.pdf"
                reportHistoryStore.add(
                    ReportHistoryItem(
                        title = "Расчётный лист",
                        periodLabel = effectivePayrollPeriodLabel,
                        workplaceLabel = selectedPayrollWorkplaceName,
                        gross = payrollDetailedResult.summary.grossTotal,
                        ndfl = payrollDetailedResult.summary.ndfl,
                        net = payrollDetailedResult.summary.netTotal,
                        fileName = financeFeatureState.pendingReportPdfFileName,
                        format = "pdf"
                    )
                )
                navigationState = navigationState.closeScreen(AppScreen.REPORT_CENTER)
                reportPdfLauncher.launch(financeFeatureState.pendingReportPdfFileName)
            },
            onOpenHistory = {
                navigationState = navigationState.replaceScreen(
                    from = AppScreen.REPORT_CENTER,
                    to = AppScreen.REPORT_HISTORY
                )
            }
        )
    }

    if (serviceWorkflowState.showPostUpdateCheckDialog) {
        AlertDialog(
            onDismissRequest = {
                serviceWorkflowState.closePostUpdateCheck()
            },
            title = { Text("Проверка после обновления") },
            text = {
                Text("Android может сбросить разрешения будильников после обновления. Проверим уведомления, полноэкранный режим, точные будильники, бэкап и виджеты.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appWorkflowSettingsStore.save(
                            appWorkflowSettings.copy(lastCheckedVersionCode = currentAppVersionCode(context))
                        )
                        serviceWorkflowState.closePostUpdateCheck()
                        navigationState = navigationState.openScreen(AppScreen.APP_HEALTH_CHECK)
                    }
                ) {
                    Text("Проверить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        appWorkflowSettingsStore.save(
                            appWorkflowSettings.copy(lastCheckedVersionCode = currentAppVersionCode(context))
                        )
                        serviceWorkflowState.closePostUpdateCheck()
                    }
                ) {
                    Text("Позже")
                }
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.PAYROLL_DIAGNOSTICS in navigationState.screenStack) {
        PayrollDiagnosticsScreen(
            state = payrollDiagnosticsState,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.PAYROLL_DIAGNOSTICS) }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.REPORT_VISIBILITY_SETTINGS in navigationState.screenStack) {
        ReportVisibilitySettingsScreen(
            settings = reportVisibilitySettings,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.REPORT_VISIBILITY_SETTINGS) },
            onChange = { updated ->
                reportVisibilitySettingsStore.save(updated)
            }
        )
    }

    calendarInteractionState.selectedDate?.let { date ->
        ShiftPickerDialog(
            date = date,
            currentShiftCode = activeWorkplaceShiftCodesByDate[date],
            shiftTemplates = dayPickerShiftTemplates,
            workplaces = workplaces,
            systemStatusCodes = systemStatusCodes,
            templateMap = templateMap,
            holidayMap = resolvedHolidayMap,
            onDismiss = { calendarInteractionState.selectedDate = null },
            onSelectShiftCode = { code ->
                scope.launch {
                    ShiftAlarmScheduler.clearSuppressedAlarmsForDate(context, date)
                    val targetWorkplaceId = if (isSystemStatusCode(code, systemStatusCodes)) {
                        activeWorkplaceId
                    } else {
                        workplaceIdFromShiftCode(code)
                    }
                    if (targetWorkplaceId == WORKPLACE_MAIN_ID) {
                        scheduleData.upsertShiftDay(
                            ShiftDayEntity(
                                date = date.toString(),
                                shiftCode = code
                            )
                        )
                    } else {
                        scheduleData.setWorkplaceShift(
                            workplaceId = targetWorkplaceId,
                            date = date,
                            shiftCode = code
                        )
                    }
                }
                calendarInteractionState.selectedDate = null
            },
            onClearShift = {
                scope.launch {
                    clearAllAssignmentsForDate(date)
                }
                calendarInteractionState.selectedDate = null
            }
        )
    }

    calendarInteractionState.dayAssignmentsPreviewDate?.let { date ->
        DayAssignmentsDialog(
            date = date,
            assignments = calendarDayAssignmentsByDate[date].orEmpty(),
            workplaces = workplaces,
            templateMap = templateMap,
            templateAlarmConfigs = shiftAlarmTemplateConfigsByCode,
            shiftColors = shiftColors,
            shiftDayRecordsByCode = savedDays
                .filter { it.date == date.toString() }
                .associateBy { it.shiftCode },
            notes = appNotesStore.notesForDate(date),
            onAddNote = { date, assignment ->
                notesFeatureState.openEditor(
                    noteId = null,
                    dateIso = date.toString(),
                    workplaceId = assignment?.workplaceId,
                    shiftCode = assignment?.shiftCode
                )
                calendarInteractionState.dayAssignmentsPreviewDate = null
                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
            },
            onEditNote = { noteId ->
                val note = appNotes.firstOrNull { it.id == noteId }
                notesFeatureState.openEditor(
                    noteId = noteId,
                    dateIso = note?.date ?: date.toString(),
                    workplaceId = note?.workplaceId,
                    shiftCode = note?.shiftCode
                )
                calendarInteractionState.dayAssignmentsPreviewDate = null
                navigationState = navigationState.openScreen(AppScreen.NOTE_EDITOR)
            },
            onSaveShiftDayOverride = { day ->
                scope.launch {
                    scheduleData.upsertShiftDay(day)
                    showInfoSnackbar("Правка смены сохранена")
                }
            },
            onDismiss = { calendarInteractionState.dayAssignmentsPreviewDate = null }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.NOTE_EDITOR in navigationState.screenStack) {
        val editingNote = appNotes.firstOrNull { it.id == notesFeatureState.editingNoteId }
        val noteDate = runCatching { LocalDate.parse(notesFeatureState.noteDraftDateIso) }.getOrDefault(LocalDate.now())
        val noteWorkplaceName = notesFeatureState.noteDraftWorkplaceId?.let { workplaceId ->
            workplaces.firstOrNull { it.id == workplaceId }?.name
        }
        val noteShiftTitle = notesFeatureState.noteDraftShiftCode?.let { code ->
            templateMap[code]?.title ?: stripWorkplaceScopeFromShiftCode(code)
        }
        AppNoteEditorScreen(
            note = editingNote,
            date = noteDate,
            workplaceName = noteWorkplaceName,
            shiftTitle = noteShiftTitle,
            workplaceId = notesFeatureState.noteDraftWorkplaceId,
            shiftCode = notesFeatureState.noteDraftShiftCode,
            onBack = {
                navigationState = navigationState.closeScreen(AppScreen.NOTE_EDITOR)
                notesFeatureState.clearEditor()
            },
            onSave = { note ->
                appNotesStore.save(note)
                appEventLogStore.add(
                    title = "Заметка сохранена",
                    message = formatDate(noteDate),
                    category = "NOTE"
                )
            },
            onDelete = { noteId ->
                appNotesStore.delete(noteId)
                appEventLogStore.add(
                    title = "Заметка удалена",
                    message = formatDate(noteDate),
                    category = "NOTE"
                )
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.PAYROLL_SETTINGS in navigationState.screenStack) {
        key(financeFeatureState.settingsWorkplaceId) {
            PayrollSettingsDialog(
                currentSettings = payrollSettingsForEditorWorkplace,
                workplaces = workplaces,
                selectedWorkplaceId = financeFeatureState.settingsWorkplaceId,
                onChangeWorkplace = { financeFeatureState.openSettingsFor(it) },
                onDismiss = { navigationState = navigationState.closeScreen(AppScreen.PAYROLL_SETTINGS) },
                onSave = { newSettings ->
                    scope.launch {
                        if (financeFeatureState.settingsWorkplaceId == WORKPLACE_MAIN_ID) {
                            payrollSettingsStore.save(newSettings)
                        } else {
                            val updated = workplacePayrollSettingsState.settingsByWorkplaceId.toMutableMap()
                            updated[financeFeatureState.settingsWorkplaceId] = newSettings
                            workplacePayrollSettingsStore.save(
                                WorkplacePayrollSettingsState(
                                    settingsByWorkplaceId = updated
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    AnimatedFullscreenOverlay(visible = AppScreen.APPEARANCE_SETTINGS in navigationState.screenStack) {
        AppearanceSettingsScreen(
            settings = appearanceSettings,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.APPEARANCE_SETTINGS) },
            onChange = { newSettings ->
                onSaveAppearanceSettings(newSettings)
            },
            onPickCustomFont = {
                customFontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "*/*"))
            },
            onClearCustomFont = {
                onSaveAppearanceSettings(
                    appearanceSettings.copy(
                        fontMode = AppFontMode.SYSTEM,
                        customFontUri = "",
                        customFontDisplayName = ""
                    )
                )
                settingsFeatureState.setCustomFontStatus("Свой шрифт отключен")
            },
            customFontStatusMessage = settingsFeatureState.customFontStatusMessage
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.CURRENT_PARAMETERS in navigationState.screenStack) {
        CurrentParametersScreen(
            payrollSettings = effectivePayrollSettings,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.CURRENT_PARAMETERS) }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.PROFILES in navigationState.screenStack) {
        ProfilesScreen(
            state = profilesState,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.PROFILES) },
            onActivateProfile = activateProfile,
            onCreateProfile = { name ->
                val created = profileStore.createProfile(name)
                showInfoSnackbar("Профиль «${created.name}» создан")
                (context as? Activity)?.recreate()
            },
            onRenameProfile = { profileId, name ->
                if (profileStore.renameProfile(profileId, name)) {
                    showInfoSnackbar("Профиль переименован")
                }
            },
            onDeleteProfile = { profileId ->
                if (profileStore.deleteProfile(profileId)) {
                    profileStore.clearProfileData(profileId)
                    showInfoSnackbar("Профиль удалён")
                    (context as? Activity)?.recreate()
                }
            }
        )
    }
    if (settingsFeatureState.showWorkplaceRenameDialog) {
        WorkplacesRenameDialog(
            workplaces = workplaces,
            onDismiss = { settingsFeatureState.closeWorkplaceRename() },
            onSave = { namesById ->
                var changedCount = 0
                namesById.forEach { (workplaceId, name) ->
                    if (scheduleData.renameWorkplace(workplaceId, name)) {
                        changedCount += 1
                    }
                }
                if (changedCount > 0) {
                    showInfoSnackbar("Названия работ обновлены")
                }
                settingsFeatureState.closeWorkplaceRename()
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.MANUAL_HOLIDAYS in navigationState.screenStack) {
        ManualHolidaysScreen(
            records = manualHolidayRecords.sortedBy { it.date },
            onBack = { navigationState = navigationState.closeScreen(AppScreen.MANUAL_HOLIDAYS) },
            onAdd = {
                settingsFeatureState.openNewManualHoliday()
            },
            onEdit = { record ->
                settingsFeatureState.openManualHoliday(record.date)
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

    AnimatedFullscreenOverlay(visible = AppScreen.BACKUP_RESTORE in navigationState.screenStack) {
        BackupRestoreScreen(
            shiftDaysCount = savedDays.size,
            shiftTemplatesCount = shiftTemplates.size,
            additionalPaymentsCount = additionalPayments.size,
            patternTemplatesCount = patternTemplates.size,
            manualHolidayCount = manualHolidayRecords.size,
            googleAccountEmail = googleSyncMeta.accountEmail,
            lastUploadAtMillis = googleSyncMeta.lastUploadAt,
            lastRestoreAtMillis = googleSyncMeta.lastRestoreAt,
            lastCloudModifiedAtMillis = googleSyncMeta.lastCloudModifiedAt,
            autoUploadEnabled = googleSyncMeta.autoUploadEnabled,
            autoUploadIntervalHours = googleSyncMeta.autoUploadIntervalHours,
            statusMessage = serviceWorkflowState.backupRestoreStatusMessage,
            oauthPackageName = appSigningDiagnostics.packageName,
            oauthSha1 = appSigningDiagnostics.sha1.orEmpty(),
            oauthSha256 = appSigningDiagnostics.sha256.orEmpty(),
            onBack = { navigationState = navigationState.closeScreen(AppScreen.BACKUP_RESTORE) },
            onExport = {
                val fileName = "ShiftSalaryPlanner_backup_${LocalDate.now()}.json"
                serviceWorkflowState.stageBackupExport(buildCurrentBackupJson(), fileName)
                backupJsonLauncher.launch(fileName)
            },
            onImport = {
                backupImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onGoogleSignIn = {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            onGoogleSignOut = {
                googleSignInClient.signOut().addOnCompleteListener {
                    serviceWorkflowState.disconnectSignedInAccount("Google-аккаунт отключён")
                }
            },
            onUploadToCloud = {
                val account = resolveGoogleAccount()
                if (account == null) {
                    serviceWorkflowState.backupRestoreStatusMessage = "Сначала войди в Google-аккаунт"
                    return@BackupRestoreScreen
                }
                uploadBackupToCloud(account, false)
            },
            onRestoreFromCloud = {
                val account = resolveGoogleAccount()
                if (account == null) {
                    serviceWorkflowState.backupRestoreStatusMessage = "Сначала войди в Google-аккаунт"
                    return@BackupRestoreScreen
                }
                scope.launch {
                    serviceWorkflowState.backupRestoreStatusMessage = "Загружаем копию из Google Drive..."
                    runCatching {
                        val downloaded = withContext(Dispatchers.IO) {
                            downloadBackupFromGoogleDriveAppData(
                                context = context,
                                account = account
                            )
                        }
                        restoreBackupFromRawJson(
                            context = context,
                            rawJson = downloaded.backupJson,
                            existingShiftTemplates = shiftTemplates,
                            existingSavedDays = savedDays,
                            manualHolidayPrefs = manualHolidayPrefs,
                            shiftColorsPrefs = shiftColorsPrefs,
                            manualHolidayRecords = manualHolidayRecords,
                            shiftColors = shiftColors,
                            upsertShiftTemplate = { template -> scheduleData.upsertShiftTemplate(template) },
                            deleteShiftTemplate = { template -> scheduleData.deleteShiftTemplate(template) },
                            upsertShiftDay = { day -> scheduleData.upsertShiftDay(day) },
                            deleteShiftDayByDate = { date -> scheduleData.deleteShiftDay(date) },
                            onStatus = { message -> serviceWorkflowState.backupRestoreStatusMessage = message },
                            onAfterImport = {
                                googleDriveSyncStore.markRestore(
                                    cloudModifiedAtMillis = downloaded.remoteFile.modifiedAtMillis
                                )
                                (context as? Activity)?.recreate()
                            }
                        )
                    }.onFailure { error ->
                        serviceWorkflowState.backupRestoreStatusMessage =
                            "Ошибка восстановления из Google Drive: ${error.message ?: "неизвестно"}"
                    }
                }
            },
            onAutoUploadEnabledChange = { enabled ->
                googleDriveSyncStore.setAutoUploadEnabled(enabled)
                serviceWorkflowState.backupRestoreStatusMessage = if (enabled) {
                    "Автозагрузка включена"
                } else {
                    "Автозагрузка отключена"
                }
                serviceWorkflowState.clearAutoUploadCheck()
            },
            onAutoUploadIntervalHoursChange = { hours ->
                googleDriveSyncStore.setAutoUploadIntervalHours(hours)
                val intervalLabel = if (hours % 24 == 0) {
                    val days = hours / 24
                    if (days == 1) "24ч" else "${days}д"
                } else {
                    "${hours}ч"
                }
                serviceWorkflowState.backupRestoreStatusMessage = "Интервал автозагрузки: $intervalLabel"
                serviceWorkflowState.clearAutoUploadCheck()
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.EXCEL_IMPORT in navigationState.screenStack) {
        ExcelImportScreen(
            fileName = serviceWorkflowState.pendingExcelFileName,
            preview = serviceWorkflowState.excelImportPreview,
            candidates = serviceWorkflowState.excelImportCandidates,
            statusMessage = serviceWorkflowState.excelImportStatusMessage,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.EXCEL_IMPORT) },
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
                val bytes = serviceWorkflowState.pendingExcelFileBytes
                if (bytes == null) {
                    serviceWorkflowState.excelImportStatusMessage = "Сначала выбери Excel-файл"
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
                                    serviceWorkflowState.stageExcelCandidates(
                                        result.candidates,
                                        "Найдено несколько сотрудников с этой фамилией. Выбери нужного."
                                    )
                                }
                                is ExcelImportParseResult.Preview -> {
                                    val statusMessage = buildString {
                                        append("Готово к импорту: ")
                                        append(result.preview.importedDays.size)
                                        append(" дней • месяцев: ")
                                        append(result.preview.selectedMonths.joinToString())
                                        if (result.preview.templatesToCreate.isNotEmpty()) {
                                            append(" • новых шаблонов: ")
                                            append(result.preview.templatesToCreate.size)
                                        }
                                    }
                                    serviceWorkflowState.stageExcelPreview(result.preview, statusMessage)
                                }
                            }
                        }.onFailure { error ->
                            serviceWorkflowState.clearExcelParseState()
                            serviceWorkflowState.updateExcelStatus(
                                "Ошибка анализа: ${error.message ?: "неизвестно"}"
                            )
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
                        serviceWorkflowState.finishExcelImport(
                            "Импорт завершён: ${preview.importedDays.size} дней"
                        )
                    }.onFailure { error ->
                        serviceWorkflowState.excelImportStatusMessage = "Ошибка импорта: ${error.message ?: "неизвестно"}"
                    }
                }
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.WIDGET_SETTINGS in navigationState.screenStack) {
        WidgetSettingsScreen(
            prefs = widgetSettingsPrefs,
            refreshToken = widgetSettingsRuntimeState.refreshToken,
            shiftTemplates = shiftTemplates.sortedBy { it.sortOrder },
            shiftColors = shiftColors,
            onBack = { navigationState = navigationState.closeScreen(AppScreen.WIDGET_SETTINGS) },
            onSaveThemeMode = { themeMode ->
                writeWidgetThemeMode(widgetSettingsPrefs, themeMode)
                widgetSettingsRuntimeState.refresh()
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onSaveDisplaySettings = { settings ->
                writeWidgetDisplaySettings(widgetSettingsPrefs, settings)
                widgetSettingsRuntimeState.refresh()
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onSaveShiftOverride = { shiftCode, override ->
                writeWidgetShiftOverride(widgetSettingsPrefs, shiftCode, override)
                widgetSettingsRuntimeState.refresh()
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onResetShiftOverride = { shiftCode ->
                clearWidgetShiftOverride(widgetSettingsPrefs, shiftCode)
                widgetSettingsRuntimeState.refresh()
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            }
        )
    }

    if (settingsFeatureState.showManualHolidayDialog) {
        ManualHolidayDialog(
            currentRecord = editingManualHoliday,
            onDismiss = {
                settingsFeatureState.closeManualHoliday()
            },
            onSave = { record ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (settingsFeatureState.editingManualHolidayDate != null && settingsFeatureState.editingManualHolidayDate != record.date) {
                    deleteManualHoliday(
                        manualHolidayRecords = manualHolidayRecords,
                        manualHolidayPrefs = manualHolidayPrefs,
                        date = settingsFeatureState.editingManualHolidayDate!!
                    )
                }
                saveManualHoliday(
                    manualHolidayRecords = manualHolidayRecords,
                    manualHolidayPrefs = manualHolidayPrefs,
                    record = record
                )
                settingsFeatureState.closeManualHoliday()
            }
        )
    }

    AnimatedFullscreenOverlay(visible = AppScreen.ADDITIONAL_PAYMENTS in navigationState.screenStack) {
        AdditionalPaymentsManagementScreen(
            payments = additionalPaymentsForSettingsWorkplace,
            workplaces = workplaces,
            selectedWorkplaceId = financeFeatureState.settingsWorkplaceId,
            onSwitchWorkplace = { financeFeatureState.openSettingsFor(it) },
            onBack = { navigationState = navigationState.closeScreen(AppScreen.ADDITIONAL_PAYMENTS) },
            onAddPayment = {
                financeFeatureState.editingAdditionalPaymentId = null
                financeFeatureState.showAdditionalPaymentDialog = true
            },
            onEditPayment = { payment ->
                financeFeatureState.editingAdditionalPaymentId = payment.id
                financeFeatureState.showAdditionalPaymentDialog = true
            },
            onDeletePayment = { payment ->
                additionalPaymentsStore.deleteById(payment.id)
                showUndoSnackbar("Начисление удалено") {
                    additionalPaymentsStore.addOrUpdate(payment)
                }
            }
        )
    }
    AnimatedFullscreenOverlay(visible = AppScreen.DEDUCTIONS in navigationState.screenStack) {
        DeductionsManagementScreen(
            deductions = deductionsForSettingsWorkplace,
            workplaces = workplaces,
            selectedWorkplaceId = financeFeatureState.settingsWorkplaceId,
            onSwitchWorkplace = { financeFeatureState.openSettingsFor(it) },
            onBack = { navigationState = navigationState.closeScreen(AppScreen.DEDUCTIONS) },
            onAddDeduction = {
                financeFeatureState.clearDeductionEdit()
                navigationState = navigationState.openScreen(AppScreen.DEDUCTION_EDITOR)
            },
            onEditDeduction = { deduction ->
                financeFeatureState.startDeductionEdit(deduction.id)
                navigationState = navigationState.openScreen(AppScreen.DEDUCTION_EDITOR)
            },
            onDeleteDeduction = { deduction ->
                deductionsStore.deleteById(deduction.id)
                showUndoSnackbar("Удержание удалено") {
                    deductionsStore.addOrUpdate(deduction)
                }
            },
            onToggleActive = { deduction, active ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    deductionsStore.setActive(deduction.id, active)
                }
            }
        )
    }
    if (financeFeatureState.showAdditionalPaymentDialog) {
        AdditionalPaymentDialog(
            currentPayment = editingAdditionalPayment,
            currentMonth = currentMonth,
            onDismiss = {
                financeFeatureState.closePaymentDialog()
            },
            onSave = { payment ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    additionalPaymentsStore.addOrUpdate(
                        payment.copy(workplaceId = financeFeatureState.settingsWorkplaceId)
                    )
                }
                showInfoSnackbar("Начисление сохранено")
                financeFeatureState.closePaymentDialog()
            }
        )
    }
    AnimatedFullscreenOverlay(visible = AppScreen.DEDUCTION_EDITOR in navigationState.screenStack) {
        DeductionEditorScreen(
            currentDeduction = editingDeduction,
            onBack = {
                navigationState = navigationState.closeScreen(AppScreen.DEDUCTION_EDITOR)
                financeFeatureState.clearDeductionEdit()
            },
            onSave = { deduction ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                deductionsStore.addOrUpdate(
                    deduction.copy(workplaceId = financeFeatureState.settingsWorkplaceId)
                )
                showInfoSnackbar("Удержание сохранено")
                navigationState = navigationState.closeScreen(AppScreen.DEDUCTION_EDITOR)
                financeFeatureState.clearDeductionEdit()
            }
        )
    }
    AnimatedFullscreenOverlay(visible = AppScreen.SHIFT_TEMPLATE_EDITOR in navigationState.screenStack) {
        ShiftTemplateEditorScreen(
            currentTemplate = editingShiftTemplate,
            workplaces = workplaces,
            defaultWorkplaceId = if (shiftFeatureState.creatingSystemStatus) WORKPLACE_MAIN_ID else activeWorkplaceId,
            isSystemStatusEditor = shiftFeatureState.creatingSystemStatus ||
                    isSystemStatusCode(editingShiftTemplate?.code.orEmpty(), systemStatusCodes),
            currentSpecialRule = editingShiftSpecialRule,
            currentAlarmTemplateConfig = editingShiftAlarmTemplateConfig,
            onBack = {
                navigationState = navigationState.closeScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                shiftFeatureState.clearEditor()
            },
            onSave = { template, alarmTemplateConfig, _ ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val oldTemplate = editingShiftTemplate
                val oldCode = oldTemplate?.code
                val saveAsSystemStatus =
                    shiftFeatureState.creatingSystemStatus || isSystemStatusCode(oldTemplate?.code.orEmpty(), systemStatusCodes)
                val normalizedTemplate = if (saveAsSystemStatus) {
                    template.copy(
                        code = stripWorkplaceScopeFromShiftCode(template.code),
                        totalHours = 0.0,
                        breakHours = 0.0,
                        nightHours = 0.0,
                        isWeekendPaid = false
                    )
                } else {
                    template
                }

                scope.launch {
                    scheduleData.upsertShiftTemplate(normalizedTemplate)

                    if (oldTemplate != null && oldCode != null && oldCode != normalizedTemplate.code) {
                        savedDays
                            .filter { it.shiftCode == oldCode }
                            .forEach { day ->
                                scheduleData.upsertShiftDay(day.copy(shiftCode = normalizedTemplate.code))
                            }
                        scheduleData.replaceShiftCode(
                            oldShiftCode = oldCode,
                            newShiftCode = normalizedTemplate.code
                        )

                        scheduleData.deleteShiftTemplate(oldTemplate)

                        shiftColorsPrefs.edit { remove(oldCode) }
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
                        key = normalizedTemplate.code,
                        colorValue = parseColorHex(normalizedTemplate.colorHex, 0xFFE0E0E0.toInt())
                    )
                    shiftAlarmStore.upsertTemplateConfig(alarmTemplateConfig.copy(shiftCode = normalizedTemplate.code))
                }

                showInfoSnackbar("Смена сохранена")
                navigationState = navigationState.closeScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                shiftFeatureState.clearEditingCode()
            },
            onSaveSpecialRule = { code, rule, _ ->
                val saveAsSystemStatus =
                    shiftFeatureState.creatingSystemStatus ||
                            isSystemStatusCode(editingShiftTemplate?.code.orEmpty(), systemStatusCodes)
                saveShiftSpecialRule(
                    shiftSpecialRules = shiftSpecialRules,
                    shiftSpecialPrefs = shiftSpecialPrefs,
                    code = code,
                    rule = rule.copy(isSystemStatus = saveAsSystemStatus)
                )
                shiftFeatureState.finishSpecialRuleSave()
            },
            onDelete = { template ->
                scope.launch {
                    val templateWorkplaceId = workplaceIdFromShiftCode(template.code)
                    if (
                        templateWorkplaceId != WORKPLACE_MAIN_ID &&
                        !isSystemStatusCode(template.code, systemStatusCodes)
                    ) {
                        // Keep the legacy bootstrap marker set so older builds do not restore deleted templates.
                        markWorkplaceTemplatesSeeded(workAssignmentsPrefs, templateWorkplaceId)
                    }
                    scheduleData.deleteShiftTemplate(template)

                    savedDays
                        .filter { it.shiftCode == template.code }
                        .forEach { day ->
                            scheduleData.deleteShiftDay(day.date)
                        }
                    scheduleData.removeShiftCode(template.code)

                    shiftColorsPrefs.edit { remove(template.code) }
                    shiftColors.remove(template.code)
                    removeShiftSpecialRule(
                        shiftSpecialRules = shiftSpecialRules,
                        shiftSpecialPrefs = shiftSpecialPrefs,
                        code = template.code
                    )
                    shiftAlarmStore.removeTemplateConfig(template.code)
                }

                showInfoSnackbar("Смена удалена")
                navigationState = navigationState.closeScreen(AppScreen.SHIFT_TEMPLATE_EDITOR)
                shiftFeatureState.clearEditor()
            }
        )
    }
    if (patternWorkflowState.showPatternListDialog) {
        PatternListDialog(
            patterns = patternTemplates,
            onDismiss = { patternWorkflowState.showPatternListDialog = false },
            onAddNew = {
                patternWorkflowState.editingPatternId = null
                patternWorkflowState.showPatternEditDialog = true
            },
            onEdit = { pattern ->
                patternWorkflowState.editingPatternId = pattern.id
                patternWorkflowState.showPatternEditDialog = true
            },
            onApply = { pattern ->
                patternWorkflowState.applyingPatternId = pattern.id
                patternWorkflowState.showPatternApplyDialog = true
            },
            onDelete = { pattern ->
                patternTemplatesStore.deleteById(pattern.id)
                showUndoSnackbar("Чередование удалено") {
                    patternTemplatesStore.addOrUpdate(pattern)
                }
            }
        )
    }
    if (patternWorkflowState.showPatternApplyDialog && applyingPattern != null) {
        PatternApplyDialog(
            currentPattern = applyingPattern,
            currentMonth = currentMonth,
            onDismiss = {
                patternWorkflowState.showPatternApplyDialog = false
                patternWorkflowState.applyingPatternId = null
            },
            onApply = { cycleStartDate ->
                scope.launch {
                    val validCodes = shiftTemplates.map { it.code }.toSet()
                    val monthStartDate = currentMonth.atDay(1)
                    val monthEndDate = currentMonth.atEndOfMonth()
                    ShiftAlarmScheduler.clearSuppressedAlarmsForRange(
                        context,
                        monthStartDate,
                        monthEndDate
                    )
                    if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                        applyPatternToMonth(
                            pattern = applyingPattern,
                            cycleStartDate = cycleStartDate,
                            month = currentMonth,
                            validShiftCodes = validCodes,
                            upsertShiftDay = scheduleData::upsertShiftDay,
                            deleteShiftDay = scheduleData::deleteShiftDay
                        )
                    } else {
                        val cycle = applyingPattern.normalizedSteps().take(applyingPattern.usedLength())
                        if (cycle.isNotEmpty()) {
                            var date = monthStartDate
                            while (!date.isAfter(monthEndDate)) {
                                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(cycleStartDate, date).toInt()
                                val cycleIndex = ((diffDays % cycle.size) + cycle.size) % cycle.size
                                val code = cycle[cycleIndex]
                                if (code.isBlank()) {
                                    scheduleData.setWorkplaceShift(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = null
                                    )
                                } else if (code in validCodes) {
                                    scheduleData.setWorkplaceShift(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = code
                                    )
                                }
                                date = date.plusDays(1)
                            }
                        }
                    }
                }
                patternWorkflowState.showPatternApplyDialog = false
                patternWorkflowState.applyingPatternId = null
                navigationState = navigationState.selectTab(BottomTab.CALENDAR)
            }
        )
    }
    if (patternWorkflowState.showPatternEditDialog) {
        PatternEditDialog(
            currentPattern = editingPattern,
            shiftTemplates = shiftTemplates.filter { it.active }.sortedBy { it.sortOrder },
            onDismiss = {
                patternWorkflowState.showPatternEditDialog = false
                patternWorkflowState.editingPatternId = null
            },
            onSave = { pattern ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    patternTemplatesStore.addOrUpdate(pattern)
                }
                showInfoSnackbar("Чередование сохранено")
                patternWorkflowState.showPatternEditDialog = false
                patternWorkflowState.editingPatternId = null
            }
        )
    }
    if (patternWorkflowState.showPatternQuickPicker) {
        PatternQuickPickerDialog(
            patterns = patternTemplates,
            onDismiss = { patternWorkflowState.showPatternQuickPicker = false },
            onSelect = { pattern ->
                patternWorkflowState.activePatternId = pattern.id
                patternWorkflowState.patternRangeStartIso = null
                calendarInteractionState.activeBrushCode = null
                patternWorkflowState.showPatternQuickPicker = false
                navigationState = navigationState.selectTab(BottomTab.CALENDAR)
            },
            onOpenManager = {
                patternWorkflowState.showPatternQuickPicker = false
                patternWorkflowState.showPatternListDialog = true
            }
        )
    }
    if (
        patternWorkflowState.showPatternPreviewDialog &&
        activePattern != null &&
        pendingPatternRangeStartDate != null &&
        pendingPatternRangeEndDate != null
    ) {
        PatternApplyPreviewDialog(
            currentPattern = activePattern,
            rangeStart = pendingPatternRangeStartDate,
            rangeEnd = pendingPatternRangeEndDate,
            onDismiss = {
                patternWorkflowState.showPatternPreviewDialog = false
                patternWorkflowState.pendingPatternRangeStartIso = null
                patternWorkflowState.pendingPatternRangeEndIso = null
            },
            onApply = { phaseOffset ->
                scope.launch {
                    val validCodes = shiftTemplates.map { it.code }.toSet()
                    val rangeStartDate = requireNotNull(pendingPatternRangeStartDate)
                    val rangeEndDate = requireNotNull(pendingPatternRangeEndDate)
                    ShiftAlarmScheduler.clearSuppressedAlarmsForRange(
                        context,
                        rangeStartDate,
                        rangeEndDate
                    )
                    if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                        applyPatternToRange(
                            pattern = activePattern,
                            rangeStart = rangeStartDate,
                            rangeEnd = rangeEndDate,
                            validShiftCodes = validCodes,
                            phaseOffset = phaseOffset,
                            upsertShiftDay = scheduleData::upsertShiftDay,
                            deleteShiftDay = scheduleData::deleteShiftDay
                        )
                    } else {
                        val cycle = activePattern.normalizedSteps().take(activePattern.usedLength())
                        if (cycle.isNotEmpty()) {
                            var date = rangeStartDate
                            while (!date.isAfter(rangeEndDate)) {
                                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(rangeStartDate, date).toInt()
                                val rawIndex = diffDays + phaseOffset
                                val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
                                val code = cycle[cycleIndex]
                                if (code.isBlank()) {
                                    scheduleData.setWorkplaceShift(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = null
                                    )
                                } else if (code in validCodes) {
                                    scheduleData.setWorkplaceShift(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = code
                                    )
                                }
                                date = date.plusDays(1)
                            }
                        }
                    }
                }

                patternWorkflowState.showPatternPreviewDialog = false
                patternWorkflowState.pendingPatternRangeStartIso = null
                patternWorkflowState.pendingPatternRangeEndIso = null
                patternWorkflowState.activePatternId = null
                navigationState = navigationState.selectTab(BottomTab.CALENDAR)
            }
        )
    }

    if (patternWorkflowState.showClearMonthConfirm) {
        AlertDialog(
            onDismissRequest = { patternWorkflowState.showClearMonthConfirm = false },
            title = { Text("Очистить текущий месяц?") },
            text = {
                Text("Будут удалены все смены за ${currentMonth.monthValue.toString().padStart(2, '0')}.${currentMonth.year}.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        patternWorkflowState.showClearMonthConfirm = false
                        val monthStartDate = currentMonth.atDay(1)
                        val monthEndDate = currentMonth.atEndOfMonth()
                        val monthStart = monthStartDate.toString()
                        val monthEnd = monthEndDate.toString()
                        scope.launch {
                            ShiftAlarmScheduler.clearSuppressedAlarmsForRange(
                                context,
                                monthStartDate,
                                monthEndDate
                            )
                            scheduleData.deleteShiftDays(monthStart, monthEnd)
                            scheduleData.clearWorkplaceAssignments(
                                startDate = monthStartDate,
                                endDate = monthEndDate
                            )
                        }
                        patternWorkflowState.clearRangeModeActive = false
                        patternWorkflowState.clearRangeStartIso = null
                        patternWorkflowState.pendingClearRangeStartIso = null
                        patternWorkflowState.pendingClearRangeEndIso = null
                        showInfoSnackbar("Текущий месяц очищен")
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { patternWorkflowState.showClearMonthConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (patternWorkflowState.showClearAllCalendarConfirm) {
        AlertDialog(
            onDismissRequest = { patternWorkflowState.showClearAllCalendarConfirm = false },
            title = { Text("Очистить весь календарь?") },
            text = { Text("Будут удалены все назначенные смены во всех месяцах.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        patternWorkflowState.showClearAllCalendarConfirm = false
                        scope.launch {
                            ShiftAlarmScheduler.clearSuppressedAlarms(context)
                            scheduleData.clearAllShiftDays()
                            scheduleData.clearAllWorkplaceAssignments()
                        }
                        patternWorkflowState.clearRangeModeActive = false
                        patternWorkflowState.clearRangeStartIso = null
                        patternWorkflowState.pendingClearRangeStartIso = null
                        patternWorkflowState.pendingClearRangeEndIso = null
                        calendarInteractionState.activeBrushCode = null
                        showInfoSnackbar("Календарь полностью очищен")
                    }
                ) {
                    Text("Очистить всё")
                }
            },
            dismissButton = {
                TextButton(onClick = { patternWorkflowState.showClearAllCalendarConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
