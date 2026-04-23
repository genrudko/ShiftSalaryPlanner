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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.DefaultShiftTemplates
import com.vigilante.shiftsalaryplanner.data.FederalHolidaySeed
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.excel.ExcelImportParseResult
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelPersonCandidate
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleParser
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.payroll.AnnualNormSourceMode
import com.vigilante.shiftsalaryplanner.payroll.NormMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollCalculator
import com.vigilante.shiftsalaryplanner.payroll.PayrollSheetDraftFactory
import com.vigilante.shiftsalaryplanner.payroll.calculateDefaultSickCalculationPeriodDays
import com.vigilante.shiftsalaryplanner.payroll.calculatePaymentDates
import com.vigilante.shiftsalaryplanner.payroll.calculateSickAverageDailyFromInputs
import com.vigilante.shiftsalaryplanner.payroll.calculateVacationAverageDailyFromAccruals
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.AppearanceSettingsStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncMeta
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettings
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsState
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsState
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.defaultWorkplaces
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.ui.theme.AppColorSchemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppFontMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.CalendarDefaultWorkplaceMode
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.widget.EXTRA_OPEN_TAB
import com.vigilante.shiftsalaryplanner.widget.PREFS_WIDGET_SETTINGS
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProviderV2
import com.vigilante.shiftsalaryplanner.widget.clearWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetShiftOverride
import com.vigilante.shiftsalaryplanner.widget.writeWidgetDisplaySettings
import com.vigilante.shiftsalaryplanner.widget.writeWidgetThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialRawTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        val initialWidgetTab = parseInitialWidgetTab(initialRawTab)
        val initialFinanceSubTabName = parseInitialWidgetFinanceSubTab(initialRawTab)
        intent?.removeExtra(EXTRA_OPEN_TAB)
        setContent {
            val appearanceSettingsStore = remember { AppearanceSettingsStore(this@MainActivity) }
            val appearanceSettings by appearanceSettingsStore.settingsFlow.collectAsState(
                initial = AppearanceSettings()
            )

            ShiftSalaryPlannerTheme(
                appearanceSettings = appearanceSettings
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShiftSalaryApp(
                        initialTabName = initialWidgetTab,
                        initialFinanceSubTabName = initialFinanceSubTabName,
                        appearanceSettings = appearanceSettings,
                        onSaveAppearanceSettings = { updated ->
                            appearanceSettingsStore.save(updated)
                        }
                    )
                }
            }
        }
    }
}

private fun parseInitialWidgetTab(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = when (raw) {
        "PAYROLL", "PAYMENTS" -> BottomTab.FINANCE.name
        else -> raw
    }
    return runCatching { BottomTab.valueOf(normalized).name }.getOrNull()
}

private fun parseInitialWidgetFinanceSubTab(raw: String?): String? {
    return when (raw) {
        "PAYROLL" -> FinanceSubTab.PAYROLL.name
        "PAYMENTS" -> FinanceSubTab.PAYMENTS.name
        else -> null
    }
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
    initialTabName: String? = null,
    initialFinanceSubTabName: String? = null,
    appearanceSettings: AppearanceSettings,
    onSaveAppearanceSettings: (AppearanceSettings) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var dayAssignmentsPreviewDate by remember { mutableStateOf<LocalDate?>(null) }
    var quickPickerOpen by rememberSaveable { mutableStateOf(false) }
    var activeBrushCode by rememberSaveable { mutableStateOf<String?>(null) }
    var showPayrollSettings by rememberSaveable { mutableStateOf(false) }
    var showCurrentParameters by rememberSaveable { mutableStateOf(false) }
    var showPayrollDiagnostics by rememberSaveable { mutableStateOf(false) }
    var showAdditionalPaymentsScreen by rememberSaveable { mutableStateOf(false) }
    var showAdditionalPaymentDialog by rememberSaveable { mutableStateOf(false) }
    var editingAdditionalPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeductionsScreen by rememberSaveable { mutableStateOf(false) }
    var showDeductionEditorScreen by rememberSaveable { mutableStateOf(false) }
    var editingDeductionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showShiftTemplateEditDialog by rememberSaveable { mutableStateOf(false) }
    var editingShiftTemplateCode by rememberSaveable { mutableStateOf<String?>(null) }
    var creatingSystemStatus by rememberSaveable { mutableStateOf(false) }
    var isSummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var payrollPeriodModeName by rememberSaveable { mutableStateOf(PayrollPeriodMode.MONTH.name) }
    var payrollWorkplaceFilterId by rememberSaveable { mutableStateOf(PAYROLL_WORKPLACE_ALL_ID) }
    var settingsWorkplaceId by rememberSaveable { mutableStateOf(WORKPLACE_MAIN_ID) }
    var payrollSelectedYear by rememberSaveable { mutableIntStateOf(currentMonth.year) }
    var payrollRangeStartIso by rememberSaveable { mutableStateOf(currentMonth.atDay(1).toString()) }
    var payrollRangeEndIso by rememberSaveable { mutableStateOf(currentMonth.atEndOfMonth().toString()) }
    var isLegendExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedTabName by rememberSaveable(initialTabName) {
        mutableStateOf(initialTabName ?: BottomTab.CALENDAR.name)
    }
    var financeSubTabName by rememberSaveable(initialFinanceSubTabName) {
        mutableStateOf(initialFinanceSubTabName ?: FinanceSubTab.SUMMARY.name)
    }
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
    var clearRangeModeActive by rememberSaveable { mutableStateOf(false) }
    var clearRangeStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingClearRangeStartIso by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingClearRangeEndIso by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearMonthConfirm by rememberSaveable { mutableStateOf(false) }
    var showClearAllCalendarConfirm by rememberSaveable { mutableStateOf(false) }
    var activeWorkplaceId by rememberSaveable { mutableStateOf(WORKPLACE_MAIN_ID) }
    var calendarWorkplaceFilterId by rememberSaveable(appearanceSettings.calendarDefaultWorkplaceMode.name) {
        mutableStateOf(
            if (appearanceSettings.calendarDefaultWorkplaceMode == CalendarDefaultWorkplaceMode.ALL_WORKPLACES) {
                CALENDAR_WORKPLACE_ALL_ID
            } else {
                activeWorkplaceId
            }
        )
    }
    var templateModeName by rememberSaveable { mutableStateOf(TemplateMode.SHIFTS.name) }
    var isHolidaySyncing by rememberSaveable { mutableStateOf(false) }
    var holidaySyncMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showManualHolidaysScreen by rememberSaveable { mutableStateOf(false) }
    var showManualHolidayDialog by rememberSaveable { mutableStateOf(false) }
    var editingManualHolidayDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showMonthlyReport by rememberSaveable { mutableStateOf(false) }
    var showBackupRestoreScreen by rememberSaveable { mutableStateOf(false) }
    var showWidgetSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showAppearanceSettings by rememberSaveable { mutableStateOf(false) }
    var showProfilesScreen by rememberSaveable { mutableStateOf(false) }
    var showWorkplaceRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showReportVisibilitySettings by rememberSaveable { mutableStateOf(false) }
    var showExcelImportScreen by rememberSaveable { mutableStateOf(false) }
    var excelImportStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExcelFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var backupRestoreStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var customFontStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBackupJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingBackupFileName by remember { mutableStateOf("ShiftSalaryPlanner_backup.json") }
    var pendingReportCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingReportCsvFileName by remember { mutableStateOf("report.csv") }
    var pendingReportPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingReportPdfFileName by remember { mutableStateOf("report.pdf") }
    var pendingExcelFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var excelImportPreview by remember { mutableStateOf<ExcelImportPreview?>(null) }
    var excelImportCandidates by remember { mutableStateOf<List<ExcelPersonCandidate>>(emptyList()) }
    var autoUploadCheckedForAccount by rememberSaveable { mutableStateOf("") }

    val selectedTab = remember(selectedTabName) {
        when (selectedTabName) {
            "PAYROLL", "PAYMENTS" -> BottomTab.FINANCE
            else -> runCatching { BottomTab.valueOf(selectedTabName) }.getOrElse { BottomTab.CALENDAR }
        }
    }
    val financeSubTab = remember(financeSubTabName) {
        runCatching { FinanceSubTab.valueOf(financeSubTabName) }.getOrElse { FinanceSubTab.SUMMARY }
    }
    val templateMode = TemplateMode.valueOf(templateModeName)
    val payrollPeriodMode = remember(payrollPeriodModeName) {
        runCatching { PayrollPeriodMode.valueOf(payrollPeriodModeName) }.getOrElse { PayrollPeriodMode.MONTH }
    }
    val parsedRangeStartDate = remember(payrollRangeStartIso) {
        payrollRangeStartIso?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }
    val parsedRangeEndDate = remember(payrollRangeEndIso) {
        payrollRangeEndIso?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
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
        payrollSelectedYear,
        normalizedRangeBounds
    ) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> currentMonth.atDay(1)
            PayrollPeriodMode.YEAR -> LocalDate.of(payrollSelectedYear, 1, 1)
            PayrollPeriodMode.RANGE -> normalizedRangeBounds.first
        }
    }
    val payrollPeriodEndDate = remember(
        payrollPeriodMode,
        currentMonth,
        payrollSelectedYear,
        normalizedRangeBounds
    ) {
        when (payrollPeriodMode) {
            PayrollPeriodMode.MONTH -> currentMonth.atEndOfMonth()
            PayrollPeriodMode.YEAR -> LocalDate.of(payrollSelectedYear, 12, 31)
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
    val appSigningDiagnostics = remember(context) { readAppSigningDiagnostics(context) }
    val profileStore = remember { AppProfileStore(context) }
    val profilesState by profileStore.stateFlow.collectAsState(
        initial = com.vigilante.shiftsalaryplanner.settings.AppProfilesState(
            activeProfileId = AppProfileStore.resolveActiveProfileId(context),
            profiles = listOf(
                com.vigilante.shiftsalaryplanner.settings.AppProfile(
                    id = AppProfileStore.DEFAULT_PROFILE_ID,
                    name = AppProfileStore.DEFAULT_PROFILE_NAME
                )
            )
        )
    )
    val activeProfileId = profilesState.activeProfileId
    val activeProfileName = profilesState.activeProfile?.name ?: AppProfileStore.DEFAULT_PROFILE_NAME

    val payrollSettingsStore = remember(activeProfileId) { PayrollSettingsStore(context) }
    val reportVisibilitySettingsStore = remember(activeProfileId) { ReportVisibilitySettingsStore(context) }
    val workAssignmentsStore = remember(activeProfileId) { WorkAssignmentsStore(context) }
    val workplacePayrollSettingsStore = remember(activeProfileId) { WorkplacePayrollSettingsStore(context) }
    val shiftAlarmStore = remember(activeProfileId) { ShiftAlarmStore(context) }
    val patternTemplatesStore = remember(activeProfileId) { PatternTemplatesStore(context) }
    val additionalPaymentsStore = remember(activeProfileId) { AdditionalPaymentsStore(context) }
    val deductionsStore = remember(activeProfileId) { DeductionsStore(context) }
    val googleDriveSyncStore = remember(activeProfileId) { GoogleDriveSyncStore(context) }
    val googleDriveScope = remember { Scope(DriveScopes.DRIVE_APPDATA) }
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(googleDriveScope)
                .build()
        )
    }
    var googleSignedInAccount by remember {
        mutableStateOf(
            GoogleSignIn.getLastSignedInAccount(context)
                ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
        )
    }
    val googleSyncMeta by googleDriveSyncStore.metaFlow.collectAsState(initial = GoogleDriveSyncMeta())
    val db = remember(activeProfileId) { AppDatabase.getDatabase(context, activeProfileId) }
    val shiftDayDao = remember(db) { db.shiftDayDao() }
    val shiftTemplateDao = remember(db) { db.shiftTemplateDao() }
    val holidayDao = remember(db) { db.holidayDao() }
    val holidaySyncRepository = remember { HolidaySyncRepository(holidayDao) }
    val excelScheduleParser = remember { ExcelScheduleParser() }
    val excelScheduleImporter = remember(shiftTemplateDao, shiftDayDao) { ExcelScheduleImporter(shiftTemplateDao, shiftDayDao) }
    val scope = rememberCoroutineScope()
    val appSnackbarHostState = remember { SnackbarHostState() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val showInfoSnackbar: (String) -> Unit = { message ->
        scope.launch {
            appSnackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }
    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
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

    LaunchedEffect(googleSignedInAccount?.email) {
        googleDriveSyncStore.setAccountEmail(googleSignedInAccount?.email.orEmpty())
    }

    setCurrencySymbol(appearanceSettings.currencySymbolMode.symbol)

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

    val reportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingReportPdfBytes
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                }
            }
        }
        pendingReportPdfBytes = null
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
            customFontStatusMessage = "Загружен шрифт: $fontName"
        }.onFailure { error ->
            customFontStatusMessage = "Ошибка загрузки шрифта: ${error.message ?: "неизвестно"}"
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
    val reportVisibilitySettings by reportVisibilitySettingsStore.settingsFlow.collectAsState(
        initial = ReportVisibilitySettings()
    )
    val workAssignmentsState by workAssignmentsStore.stateFlow.collectAsState(
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
    var shiftAlarmRescheduleResult by remember(activeProfileId) { mutableStateOf<ShiftAlarmRescheduleResult?>(null) }
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
    val manualHolidayRecords = remember(activeProfileId) { mutableStateListOf<ManualHolidayRecord>() }
    var widgetSettingsRefreshToken by remember(activeProfileId) { mutableIntStateOf(0) }

    LaunchedEffect(savedDays, shiftTemplates) {
            ShiftMonthWidgetProviderV2.requestUpdate(context)
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
    val clearRangeStartDate = remember(clearRangeStartIso) {
        clearRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingClearRangeStartDate = remember(pendingClearRangeStartIso) {
        pendingClearRangeStartIso?.let { LocalDate.parse(it) }
    }
    val pendingClearRangeEndDate = remember(pendingClearRangeEndIso) {
        pendingClearRangeEndIso?.let { LocalDate.parse(it) }
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
    val payrollSettingsForSelectedWorkplace = remember(
        payrollSettings,
        payrollWorkplaceFilterId,
        payrollSettingsOverridesByWorkplace
    ) {
        when (payrollWorkplaceFilterId) {
            PAYROLL_WORKPLACE_ALL_ID,
            WORKPLACE_MAIN_ID -> payrollSettings
            else -> payrollSettingsOverridesByWorkplace[payrollWorkplaceFilterId] ?: payrollSettings
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
    LaunchedEffect(activeWorkplaceId, calendarWorkplaceFilterId) {
        if (
            calendarWorkplaceFilterId != CALENDAR_WORKPLACE_ALL_ID &&
            calendarWorkplaceFilterId != activeWorkplaceId
        ) {
            calendarWorkplaceFilterId = activeWorkplaceId
        }
    }
    LaunchedEffect(workplaces, calendarWorkplaceFilterId, activeWorkplaceId) {
        if (calendarWorkplaceFilterId == CALENDAR_WORKPLACE_ALL_ID) return@LaunchedEffect
        if (workplaces.none { it.id == calendarWorkplaceFilterId }) {
            calendarWorkplaceFilterId = if (workplaces.any { it.id == activeWorkplaceId }) {
                activeWorkplaceId
            } else {
                workplaces.firstOrNull()?.id ?: WORKPLACE_MAIN_ID
            }
        }
    }
    LaunchedEffect(workplaces, settingsWorkplaceId) {
        if (workplaces.none { it.id == settingsWorkplaceId }) {
            settingsWorkplaceId = workplaces.firstOrNull()?.id ?: WORKPLACE_MAIN_ID
        }
    }
    val mainShiftCodesByDate = remember(savedDays) {
        savedDays.associate { LocalDate.parse(it.date) to it.shiftCode }
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
    val calendarDayAssignmentsByDate = remember(allDayAssignmentsByDate, calendarWorkplaceFilterId) {
        if (calendarWorkplaceFilterId == CALENDAR_WORKPLACE_ALL_ID) {
            allDayAssignmentsByDate
        } else {
            allDayAssignmentsByDate.mapNotNull { (date, assignments) ->
                val filtered = assignments.filter { it.workplaceId == calendarWorkplaceFilterId }
                if (filtered.isNotEmpty()) date to filtered else null
            }.toMap()
        }
    }
    val calendarShiftCodesByDate = remember(calendarDayAssignmentsByDate) {
        calendarDayAssignmentsByDate.mapNotNull { (date, assignments) ->
            assignments.firstOrNull()?.let { assignment -> date to assignment.shiftCode }
        }.toMap()
    }
    suspend fun clearAllAssignmentsForDate(date: LocalDate) {
        shiftDayDao.deleteByDate(date.toString())
        allDayAssignmentsByDate[date]
            .orEmpty()
            .asSequence()
            .map { assignment -> assignment.workplaceId }
            .filter { workplaceId -> workplaceId != WORKPLACE_MAIN_ID }
            .distinct()
            .forEach { workplaceId ->
                workAssignmentsStore.setShiftForDate(
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
    LaunchedEffect(payrollWorkplaceOptions, payrollWorkplaceFilterId) {
        if (payrollWorkplaceOptions.none { it.id == payrollWorkplaceFilterId }) {
            payrollWorkplaceFilterId = PAYROLL_WORKPLACE_ALL_ID
        }
    }
    val selectedPayrollWorkplaceName = remember(payrollWorkplaceFilterId, workplaces) {
        if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            "Все работы"
        } else {
            workplaces.firstOrNull { it.id == payrollWorkplaceFilterId }?.name ?: "Работа"
        }
    }
    val payrollSettingsForEditorWorkplace = remember(
        payrollSettings,
        settingsWorkplaceId,
        payrollSettingsOverridesByWorkplace
    ) {
        if (settingsWorkplaceId == WORKPLACE_MAIN_ID) {
            payrollSettings
        } else {
            payrollSettingsOverridesByWorkplace[settingsWorkplaceId] ?: payrollSettings
        }
    }
    val effectivePayrollPeriodLabel = remember(payrollPeriodLabel, selectedPayrollWorkplaceName, payrollWorkplaceFilterId) {
        if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            payrollPeriodLabel
        } else {
            "$payrollPeriodLabel · $selectedPayrollWorkplaceName"
        }
    }
    val effectivePayrollPeriodFileLabel = remember(payrollPeriodFileLabel, payrollWorkplaceFilterId) {
        if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            payrollPeriodFileLabel
        } else {
            "${payrollPeriodFileLabel}_$payrollWorkplaceFilterId"
        }
    }
    val payrollAssignmentCodesByDate = remember(allDayAssignmentsByDate, payrollWorkplaceFilterId) {
        allDayAssignmentsByDate.mapValues { (_, assignments) ->
            when (payrollWorkplaceFilterId) {
                PAYROLL_WORKPLACE_ALL_ID -> assignments.map { it.shiftCode }
                else -> assignments
                    .filter { it.workplaceId == payrollWorkplaceFilterId }
                    .map { it.shiftCode }
            }
        }
    }
    val shiftTemplateTimingByCode = remember(shiftAlarmSettings.templateConfigs) {
        shiftAlarmSettings.templateConfigs.associateBy { it.shiftCode }
    }
    val additionalPaymentsForSettingsWorkplace = remember(additionalPayments, settingsWorkplaceId) {
        additionalPayments.filter { payment ->
            belongsToWorkplace(payment.workplaceId, settingsWorkplaceId)
        }
    }
    val deductionsForSettingsWorkplace = remember(deductions, settingsWorkplaceId) {
        deductions.filter { deduction ->
            belongsToWorkplace(deduction.workplaceId, settingsWorkplaceId)
        }
    }
    val additionalPaymentsForPayroll = remember(additionalPayments, payrollWorkplaceFilterId) {
        if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            additionalPayments
        } else {
            additionalPayments.filter { payment ->
                belongsToWorkplace(payment.workplaceId, payrollWorkplaceFilterId)
            }
        }
    }
    val deductionsForPayroll = remember(deductions, payrollWorkplaceFilterId) {
        if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
            deductions
        } else {
            deductions.filter { deduction ->
                belongsToWorkplace(deduction.workplaceId, payrollWorkplaceFilterId)
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
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        periodEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = payrollSettings.applyShortDayReduction,
                specialRule = shiftSpecialRulesSnapshot[code],
                shiftTiming = shiftTemplateTimingByCode[code]
            )
        }
    }

    val periodShiftsWithoutShortDayReduction = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        periodEntries.mapNotNull { (date, code) ->
            templateMap[code]?.toWorkShiftItemForDate(
                date = date,
                holidayMap = resolvedHolidayMap,
                applyShortDayReduction = false,
                specialRule = shiftSpecialRulesSnapshot[code],
                shiftTiming = shiftTemplateTimingByCode[code]
            )
        }
    }

    val firstHalfShifts = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        periodEntries
            .filter { (date, _) -> date.dayOfMonth <= 15 }
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

    val firstHalfShiftsWithoutShortDayReduction = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        periodEntries
            .filter { (date, _) -> date.dayOfMonth <= 15 }
            .mapNotNull { (date, code) ->
                templateMap[code]?.toWorkShiftItemForDate(
                    date = date,
                    holidayMap = resolvedHolidayMap,
                    applyShortDayReduction = false,
                    specialRule = shiftSpecialRulesSnapshot[code],
                    shiftTiming = shiftTemplateTimingByCode[code]
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

    val overtimePeriodInfo = remember(payrollPeriodAnchorMonth, payrollSettings.overtimePeriod) {
        resolveOvertimePeriodInfo(payrollPeriodAnchorMonth, payrollSettings.overtimePeriod)
    }

    val annualOvertime = remember(
        payrollAssignmentCodesByDate,
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
        payrollSettings,
        normMode,
        annualNormSourceMode,
        resolvedHolidayMap
    ) {
        calculateNormHoursForDateRange(
            startDate = payrollPeriodStartDate,
            endDate = payrollPeriodEndDate,
            payrollSettings = payrollSettings,
            normMode = normMode,
            annualNormSourceMode = annualNormSourceMode,
            holidayMap = resolvedHolidayMap,
            applyShortDayReduction = payrollSettings.applyShortDayReduction
        )
    }

    LaunchedEffect(savedDays, templateMap, shiftAlarmSettings) {
        shiftAlarmRescheduleResult = rescheduleShiftAlarms(
            context = context,
            settings = shiftAlarmSettings,
            savedDays = savedDays,
            templateMap = templateMap
        )
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

    LaunchedEffect(activeWorkplaceId, workplaces, shiftTemplates, systemStatusCodes) {
        if (activeWorkplaceId == WORKPLACE_MAIN_ID) return@LaunchedEffect
        val seededWorkplaceIds = readSeededWorkplaceTemplateIds(workAssignmentsPrefs)
        if (activeWorkplaceId in seededWorkplaceIds) return@LaunchedEffect

        val alreadyHasScopedTemplates = shiftTemplates.any { template ->
            isShiftCodeForWorkplace(template.code, activeWorkplaceId) &&
                    !isSystemStatusCode(template.code, systemStatusCodes)
        }
        if (alreadyHasScopedTemplates) {
            markWorkplaceTemplatesSeeded(workAssignmentsPrefs, activeWorkplaceId)
            return@LaunchedEffect
        }

        val baseTemplates = shiftTemplates
            .filter { template ->
                !isWorkplaceScopedShiftCode(template.code) &&
                        !isSystemStatusCode(template.code, systemStatusCodes)
            }
            .sortedBy { it.sortOrder }
        if (baseTemplates.isEmpty()) {
            markWorkplaceTemplatesSeeded(workAssignmentsPrefs, activeWorkplaceId)
            return@LaunchedEffect
        }

        baseTemplates.forEach { baseTemplate ->
            val scopedCode = workplaceScopedShiftCode(activeWorkplaceId, baseTemplate.code)
            val scopedTemplate = baseTemplate.copy(
                code = scopedCode,
                title = baseTemplate.title
            )
            shiftTemplateDao.upsert(scopedTemplate)

            val baseColor = shiftColors[baseTemplate.code]
                ?: parseColorHex(baseTemplate.colorHex, 0xFFE0E0E0.toInt())
            saveShiftColor(
                shiftColors = shiftColors,
                shiftColorsPrefs = shiftColorsPrefs,
                context = context,
                key = scopedCode,
                colorValue = baseColor
            )

            val baseSpecialRule = shiftSpecialRulesSnapshot[baseTemplate.code]
            if (baseSpecialRule != null) {
                saveShiftSpecialRule(
                    shiftSpecialRules = shiftSpecialRules,
                    shiftSpecialPrefs = shiftSpecialPrefs,
                    code = scopedCode,
                    rule = baseSpecialRule
                )
            }
        }
        markWorkplaceTemplatesSeeded(workAssignmentsPrefs, activeWorkplaceId)
    }

    LaunchedEffect(activeWorkplaceId, workAssignmentsState.extraAssignmentsByDate) {
        if (activeWorkplaceId == WORKPLACE_MAIN_ID) return@LaunchedEffect

        workAssignmentsState.extraAssignmentsByDate.forEach { (date, byWorkplace) ->
            val rawCode = byWorkplace[activeWorkplaceId] ?: return@forEach
            if (isWorkplaceScopedShiftCode(rawCode)) return@forEach
            val scopedCode = workplaceScopedShiftCode(activeWorkplaceId, rawCode)
            workAssignmentsStore.setShiftForDate(
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
            shiftTemplateDao.delete(template)
            shiftColorsPrefs.edit { remove(template.code) }
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
                    shiftTemplateDao.upsert(normalized)
                    templatesByCode[canonicalCode] = normalized
                }

                savedDays
                    .filter { day -> day.shiftCode == scopedTemplate.code }
                    .forEach { day ->
                        shiftDayDao.upsert(day.copy(shiftCode = canonicalTemplate.code))
                    }

                workAssignmentsStore.replaceShiftCode(
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

                shiftTemplateDao.delete(scopedTemplate)
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
        val current = googleSignedInAccount
            ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
        if (current != null) {
            current
        } else {
            GoogleSignIn.getLastSignedInAccount(context)
                ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
                ?.also { account ->
                    googleSignedInAccount = account
                }
        }
    }
    val uploadBackupToCloud: (GoogleSignInAccount, Boolean) -> Unit = { account, auto ->
        scope.launch {
            backupRestoreStatusMessage = if (auto) {
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
                backupRestoreStatusMessage = if (uploadResult.created) {
                    if (auto) "Автокопия загружена в Google Drive" else "Копия загружена в Google Drive"
                } else {
                    if (auto) "Автокопия в Google Drive обновлена" else "Копия в Google Drive обновлена"
                }
            }.onFailure { error ->
                backupRestoreStatusMessage =
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
                googleSignedInAccount = account
                backupRestoreStatusMessage = "Google Drive подключён: ${account.email ?: "аккаунт"}"
                autoUploadCheckedForAccount = ""
            } else {
                backupRestoreStatusMessage = "Не выданы права для Google Drive"
            }
        }.onFailure { error ->
            backupRestoreStatusMessage = formatGoogleSignInFailureMessage(context, error)
        }
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

    LaunchedEffect(
        googleSignedInAccount?.email,
        googleSyncMeta.autoUploadEnabled,
        googleSyncMeta.autoUploadIntervalHours,
        googleSyncMeta.lastUploadAt
    ) {
        val account = resolveGoogleAccount() ?: return@LaunchedEffect
        if (!googleSyncMeta.autoUploadEnabled) return@LaunchedEffect

        val accountKey = account.email ?: account.id ?: return@LaunchedEffect
        if (autoUploadCheckedForAccount == accountKey) return@LaunchedEffect
        autoUploadCheckedForAccount = accountKey

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
        onTabSelected = { selectedTabName = it.name },
        snackbarHost = {
            SnackbarHost(hostState = appSnackbarHostState)
        }
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
                            profiles = profilesState.profiles,
                            activeProfileId = profilesState.activeProfileId,
                            onSwitchProfile = activateProfile,
                            onOpenProfiles = { showProfilesScreen = true },
                            workplaces = workplaces,
                            calendarWorkplaceFilterId = calendarWorkplaceFilterId,
                            onSwitchCalendarWorkplaceFilter = { selectedId ->
                                calendarWorkplaceFilterId = selectedId
                                if (selectedId != CALENDAR_WORKPLACE_ALL_ID) {
                                    activeWorkplaceId = selectedId
                                }
                            },
                            activeWorkplaceId = activeWorkplaceId,
                            onOpenManageWorkplaces = { showWorkplaceRenameDialog = true },
                            shiftCodesByDate = calendarShiftCodesByDate,
                            dayAssignmentsByDate = calendarDayAssignmentsByDate,
                            templateMap = templateMap,
                            legendShiftTemplates = quickShiftTemplates,
                            shiftColors = shiftColors,
                            quickShiftTemplates = quickShiftTemplates,
                            systemStatusCodes = systemStatusCodes,
                            quickPickerOpen = quickPickerOpen,
                            activeBrushCode = activeBrushCode,
                            holidayMap = resolvedHolidayMap,
                            isLegendExpanded = isLegendExpanded,
                            onToggleLegend = { isLegendExpanded = !isLegendExpanded },
                            onOpenColorSettings = { selectedTabName = BottomTab.SHIFTS.name },
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
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                activeBrushCode = code
                                quickPickerOpen = false
                            },
                            onClearBrush = {
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                activeBrushCode = BRUSH_CLEAR
                                quickPickerOpen = false
                            },
                            onDisableBrush = {
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                activeBrushCode = null
                                quickPickerOpen = false
                            },
                            onAddNewShift = {
                                creatingSystemStatus = false
                                editingShiftTemplateCode = null
                                showShiftTemplateEditDialog = true
                                quickPickerOpen = false
                            },
                            onOpenPatternEditor = {
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                showPatternQuickPicker = true
                                quickPickerOpen = false
                            },
                            clearRangeModeActive = clearRangeModeActive,
                            clearRangeStartDate = clearRangeStartDate,
                            pendingClearRangeStartDate = pendingClearRangeStartDate,
                            pendingClearRangeEndDate = pendingClearRangeEndDate,
                            onConfirmClearRange = {
                                if (pendingClearRangeStartDate != null && pendingClearRangeEndDate != null) {
                                    val rangeStart = pendingClearRangeStartDate.toString()
                                    val rangeEnd = pendingClearRangeEndDate.toString()
                                    scope.launch {
                                        shiftDayDao.deleteByDateRange(rangeStart, rangeEnd)
                                        workAssignmentsStore.clearDateRange(
                                            startDate = pendingClearRangeStartDate,
                                            endDate = pendingClearRangeEndDate
                                        )
                                    }
                                    showInfoSnackbar("Диапазон очищен")
                                }
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                quickPickerOpen = false
                            },
                            onCancelClearRangeMode = {
                                clearRangeModeActive = false
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                            },
                            onClearCurrentMonth = {
                                showClearMonthConfirm = true
                                quickPickerOpen = false
                            },
                            onStartRangeClearMode = {
                                activeBrushCode = null
                                activePatternId = null
                                patternRangeStartIso = null
                                pendingPatternRangeStartIso = null
                                pendingPatternRangeEndIso = null
                                clearRangeModeActive = true
                                clearRangeStartIso = null
                                pendingClearRangeStartIso = null
                                pendingClearRangeEndIso = null
                                quickPickerOpen = false
                            },
                            onClearAllCalendar = {
                                showClearAllCalendarConfirm = true
                                quickPickerOpen = false
                            },
                            onEraseDate = { date ->
                                scope.launch {
                                    clearAllAssignmentsForDate(date)
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
                                    clearRangeModeActive -> {
                                        val start = clearRangeStartIso?.let { LocalDate.parse(it) }
                                        if (start == null) {
                                            clearRangeStartIso = date.toString()
                                            pendingClearRangeStartIso = null
                                            pendingClearRangeEndIso = null
                                        } else {
                                            val rangeStart = minOf(start, date)
                                            val rangeEnd = maxOf(start, date)
                                            pendingClearRangeStartIso = rangeStart.toString()
                                            pendingClearRangeEndIso = rangeEnd.toString()
                                            clearRangeStartIso = null
                                        }
                                    }

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
                                            clearAllAssignmentsForDate(date)
                                        }
                                    }

                                    else -> {
                                        scope.launch {
                                            if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                                                shiftDayDao.upsert(
                                                    ShiftDayEntity(
                                                        date = date.toString(),
                                                        shiftCode = activeBrushCode!!
                                                    )
                                                )
                                            } else {
                                                workAssignmentsStore.setShiftForDate(
                                                    workplaceId = activeWorkplaceId,
                                                    date = date,
                                                    shiftCode = activeBrushCode
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onDayLongPress = { date ->
                                if (activeBrushCode == null && activePattern == null && !clearRangeModeActive) {
                                    if (YearMonth.from(date) != currentMonth) {
                                        currentMonth = YearMonth.from(date)
                                    }
                                    selectedDate = null
                                    dayAssignmentsPreviewDate = date
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomTab.FINANCE -> {
                        FinanceTab(
                            selectedSubTab = financeSubTab,
                            onSelectSubTab = { tab -> financeSubTabName = tab.name },
                            summaryState = FinanceSummaryState(
                                periodLabel = effectivePayrollPeriodLabel,
                                workplaceLabel = "Работа: $selectedPayrollWorkplaceName",
                                payroll = payroll,
                                detailedShiftStats = detailedShiftStats,
                                paymentDates = paymentDates
                            ),
                            payrollContent = {
                                PayrollTab(
                                    state = PayrollTabState(
                                        currentMonth = currentMonth,
                                        periodMode = payrollPeriodMode,
                                        selectedWorkplaceId = payrollWorkplaceFilterId,
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
                                        housingPaymentLabel = payrollSettings.housingPaymentLabel,
                                        detailedShiftStats = detailedShiftStats,
                                        isSummaryExpanded = isSummaryExpanded,
                                        reportVisibilitySettings = reportVisibilitySettings
                                    ),
                                    actions = PayrollTabActions(
                                        onChangePeriodMode = { mode ->
                                            payrollPeriodModeName = mode.name
                                            when (mode) {
                                                PayrollPeriodMode.MONTH -> Unit
                                                PayrollPeriodMode.YEAR -> {
                                                    payrollSelectedYear = payrollPeriodEndDate.year
                                                }
                                                PayrollPeriodMode.RANGE -> {
                                                    payrollRangeStartIso = payrollPeriodStartDate.toString()
                                                    payrollRangeEndIso = payrollPeriodEndDate.toString()
                                                }
                                            }
                                        },
                                        onChangeWorkplace = { workplaceId ->
                                            payrollWorkplaceFilterId = workplaceId
                                        },
                                        onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                                        onPickMonth = { pickedMonth -> currentMonth = pickedMonth },
                                        onPrevYear = { payrollSelectedYear -= 1 },
                                        onNextYear = { payrollSelectedYear += 1 },
                                        onPickYear = { year -> payrollSelectedYear = year },
                                        onShiftRangeBackward = {
                                            val daysInRange =
                                                kotlin.math.max(
                                                    1L,
                                                    java.time.temporal.ChronoUnit.DAYS.between(
                                                        payrollPeriodStartDate,
                                                        payrollPeriodEndDate
                                                    ) + 1L
                                                )
                                            payrollRangeStartIso = payrollPeriodStartDate.minusDays(daysInRange).toString()
                                            payrollRangeEndIso = payrollPeriodEndDate.minusDays(daysInRange).toString()
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
                                            payrollRangeStartIso = payrollPeriodStartDate.plusDays(daysInRange).toString()
                                            payrollRangeEndIso = payrollPeriodEndDate.plusDays(daysInRange).toString()
                                        },
                                        onPickRangeStart = { date ->
                                            if (date.isAfter(payrollPeriodEndDate)) {
                                                payrollRangeStartIso = payrollPeriodEndDate.toString()
                                                payrollRangeEndIso = date.toString()
                                            } else {
                                                payrollRangeStartIso = date.toString()
                                            }
                                        },
                                        onPickRangeEnd = { date ->
                                            if (date.isBefore(payrollPeriodStartDate)) {
                                                payrollRangeStartIso = date.toString()
                                                payrollRangeEndIso = payrollPeriodStartDate.toString()
                                            } else {
                                                payrollRangeEndIso = date.toString()
                                            }
                                        },
                                        onToggleSummary = { isSummaryExpanded = !isSummaryExpanded },
                                        onOpenSettings = {
                                            settingsWorkplaceId = if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
                                                WORKPLACE_MAIN_ID
                                            } else {
                                                payrollWorkplaceFilterId
                                            }
                                            showPayrollSettings = true
                                        },
                                        onOpenDiagnostics = { showPayrollDiagnostics = true },
                                        onOpenVisibilitySettings = { showReportVisibilitySettings = true },
                                        onExportSheetPdf = { periodLabel, fileLabel, detailedResult ->
                                            pendingReportPdfBytes = buildPayrollSheetPdf(
                                                periodLabel = periodLabel,
                                                payrollDetailedResult = detailedResult
                                            )
                                            pendingReportPdfFileName = "payroll_sheet_$fileLabel.pdf"
                                            reportPdfLauncher.launch(pendingReportPdfFileName)
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
                                    housingPaymentLabel = payrollSettings.housingPaymentLabel,
                                    additionalPayments = additionalPaymentsForPayroll,
                                    resolvedAdditionalPaymentsBreakdown = resolvedAdditionalPaymentBreakdown,
                                    detailedShiftStats = detailedShiftStats,
                                    onAddPayment = {
                                        settingsWorkplaceId = if (payrollWorkplaceFilterId == PAYROLL_WORKPLACE_ALL_ID) {
                                            WORKPLACE_MAIN_ID
                                        } else {
                                            payrollWorkplaceFilterId
                                        }
                                        editingAdditionalPaymentId = null
                                        showAdditionalPaymentDialog = true
                                    },
                                    onEditPayment = { payment ->
                                        settingsWorkplaceId = normalizeWorkplaceId(payment.workplaceId)
                                        editingAdditionalPaymentId = payment.id
                                        showAdditionalPaymentDialog = true
                                    },
                                    onDeletePayment = { payment ->
                                        additionalPaymentsStore.deleteById(payment.id)
                                        showUndoSnackbar("Начисление удалено") {
                                            additionalPaymentsStore.addOrUpdate(payment)
                                        }
                                    },
                                    onOpenMonthlyReport = {
                                        showMonthlyReport = true
                                    },
                                    onOpenVisibilitySettings = {
                                        showReportVisibilitySettings = true
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
                                lastRescheduleResult = shiftAlarmRescheduleResult,
                                canScheduleExactAlarms = ShiftAlarmScheduler.canScheduleExactShiftAlarms(context),
                                notificationPermissionGranted = ShiftAlarmScheduler.hasNotificationPermission(context),
                                fullScreenIntentPermissionGranted = ShiftAlarmScheduler.hasFullScreenIntentPermission(context)
                            ),
                            actions = ShiftAlarmsTabActions(
                                onSave = { newSettings ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        shiftAlarmRescheduleResult = saveAndRescheduleShiftAlarms(
                                            store = shiftAlarmStore,
                                            context = context,
                                            settings = newSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap,
                                            mirrorToSystemClockApp = false,
                                            allowSystemClockUiFallback = false
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
                                        shiftAlarmRescheduleResult = rescheduleShiftAlarms(
                                            context = context,
                                            settings = shiftAlarmSettings,
                                            savedDays = savedDays,
                                            templateMap = templateMap,
                                            mirrorToSystemClockApp = false,
                                            allowSystemClockUiFallback = false
                                        )
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
                                activeWorkplaceId = activeWorkplaceId
                            ),
                            actions = TemplatesScreenActions(
                                onModeChange = { templateModeName = it.name },
                                onBack = { selectedTabName = BottomTab.CALENDAR.name },
                                onSwitchWorkplace = { activeWorkplaceId = it },
                                onOpenManageWorkplaces = { showWorkplaceRenameDialog = true },
                                onAddShift = {
                                    creatingSystemStatus = false
                                    editingShiftTemplateCode = null
                                    showShiftTemplateEditDialog = true
                                },
                                onAddSystemStatus = {
                                    creatingSystemStatus = true
                                    editingShiftTemplateCode = null
                                    showShiftTemplateEditDialog = true
                                },
                                onEditShift = { template ->
                                    creatingSystemStatus = isSystemStatusCode(template.code, systemStatusCodes)
                                    editingShiftTemplateCode = template.code
                                    showShiftTemplateEditDialog = true
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
                                        shiftTemplateDao.upsert(duplicatedTemplate)

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
                                        val removedExtraAssignments = workAssignmentsStore.removeShiftCode(template.code)
                                        val existingColor = shiftColors[template.code]
                                            ?: parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                                        val existingRule = shiftSpecialRulesSnapshot[template.code]
                                        val existingAlarm = shiftAlarmSettings.templateConfigs.firstOrNull { it.shiftCode == template.code }

                                        shiftTemplateDao.delete(template)
                                        linkedDays.forEach { day ->
                                            shiftDayDao.deleteByDate(day.date)
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
                                                shiftTemplateDao.upsert(template)
                                                linkedDays.forEach { day ->
                                                    shiftDayDao.upsert(day)
                                                }
                                                workAssignmentsStore.restoreAssignments(removedExtraAssignments)
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
                                settingsWorkplaceId = activeWorkplaceId
                                showDeductionsScreen = true
                            },
                            manualHolidayCount = manualHolidayRecords.size,
                            isHolidaySyncing = isHolidaySyncing,
                            holidaySyncMessage = holidaySyncMessage,
                            applyShortDayReduction = payrollSettings.applyShortDayReduction,
                            onOpenPayrollSettings = {
                                settingsWorkplaceId = activeWorkplaceId
                                showPayrollSettings = true
                            },
                            onOpenAppearanceSettings = { showAppearanceSettings = true },
                            onOpenReportVisibilitySettings = { showReportVisibilitySettings = true },
                            onOpenPayments = {
                                settingsWorkplaceId = activeWorkplaceId
                                showAdditionalPaymentsScreen = true
                            },
                            onOpenCurrentParameters = { showCurrentParameters = true },
                            onOpenManualHolidays = { showManualHolidaysScreen = true },
                            onOpenBackupRestore = { showBackupRestoreScreen = true },
                            onOpenExcelImport = { showExcelImportScreen = true },
                            onOpenWidgetSettings = { showWidgetSettingsScreen = true },
                            onOpenProfiles = { showProfilesScreen = true },
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
    AnimatedFullscreenOverlay(visible = showMonthlyReport) {
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
            onBack = { showMonthlyReport = false },
            onExportCsv = {
                pendingReportCsvContent = buildMonthlyReportCsv(
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
                pendingReportCsvFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.csv"
                reportCsvLauncher.launch(pendingReportCsvFileName)
            },
            onExportPdf = {
                pendingReportPdfBytes = buildMonthlyReportPdf(
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
                pendingReportPdfFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.pdf"
                reportPdfLauncher.launch(pendingReportPdfFileName)
            }
        )
    }

    AnimatedFullscreenOverlay(visible = showPayrollDiagnostics) {
        PayrollDiagnosticsScreen(
            state = payrollDiagnosticsState,
            onBack = { showPayrollDiagnostics = false }
        )
    }

    AnimatedFullscreenOverlay(visible = showReportVisibilitySettings) {
        ReportVisibilitySettingsScreen(
            settings = reportVisibilitySettings,
            onBack = { showReportVisibilitySettings = false },
            onChange = { updated ->
                reportVisibilitySettingsStore.save(updated)
            }
        )
    }

    selectedDate?.let { date ->
        ShiftPickerDialog(
            date = date,
            currentShiftCode = activeWorkplaceShiftCodesByDate[date],
            shiftTemplates = dayPickerShiftTemplates,
            workplaces = workplaces,
            systemStatusCodes = systemStatusCodes,
            templateMap = templateMap,
            holidayMap = resolvedHolidayMap,
            onDismiss = { selectedDate = null },
            onSelectShiftCode = { code ->
                scope.launch {
                    val targetWorkplaceId = if (isSystemStatusCode(code, systemStatusCodes)) {
                        activeWorkplaceId
                    } else {
                        workplaceIdFromShiftCode(code)
                    }
                    if (targetWorkplaceId == WORKPLACE_MAIN_ID) {
                        shiftDayDao.upsert(
                            ShiftDayEntity(
                                date = date.toString(),
                                shiftCode = code
                            )
                        )
                    } else {
                        workAssignmentsStore.setShiftForDate(
                            workplaceId = targetWorkplaceId,
                            date = date,
                            shiftCode = code
                        )
                    }
                }
                selectedDate = null
            },
            onClearShift = {
                scope.launch {
                    clearAllAssignmentsForDate(date)
                }
                selectedDate = null
            }
        )
    }

    dayAssignmentsPreviewDate?.let { date ->
        DayAssignmentsDialog(
            date = date,
            assignments = calendarDayAssignmentsByDate[date].orEmpty(),
            workplaces = workplaces,
            templateMap = templateMap,
            templateAlarmConfigs = shiftAlarmTemplateConfigsByCode,
            shiftColors = shiftColors,
            onDismiss = { dayAssignmentsPreviewDate = null }
        )
    }

    AnimatedFullscreenOverlay(visible = showPayrollSettings) {
        key(settingsWorkplaceId) {
            PayrollSettingsDialog(
                currentSettings = payrollSettingsForEditorWorkplace,
                workplaces = workplaces,
                selectedWorkplaceId = settingsWorkplaceId,
                onChangeWorkplace = { settingsWorkplaceId = it },
                onDismiss = { showPayrollSettings = false },
                onSave = { newSettings ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        if (settingsWorkplaceId == WORKPLACE_MAIN_ID) {
                            payrollSettingsStore.save(newSettings)
                        } else {
                            val updated = workplacePayrollSettingsState.settingsByWorkplaceId.toMutableMap()
                            updated[settingsWorkplaceId] = newSettings
                            workplacePayrollSettingsStore.save(
                                WorkplacePayrollSettingsState(
                                    settingsByWorkplaceId = updated
                                )
                            )
                        }
                    }
                    showPayrollSettings = false
                }
            )
        }
    }

    AnimatedFullscreenOverlay(visible = showAppearanceSettings) {
        AppearanceSettingsScreen(
            settings = appearanceSettings,
            onBack = { showAppearanceSettings = false },
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
                customFontStatusMessage = "Свой шрифт отключен"
            },
            customFontStatusMessage = customFontStatusMessage
        )
    }

    AnimatedFullscreenOverlay(visible = showCurrentParameters) {
        CurrentParametersScreen(
            payrollSettings = effectivePayrollSettings,
            onBack = { showCurrentParameters = false }
        )
    }

    AnimatedFullscreenOverlay(visible = showProfilesScreen) {
        ProfilesScreen(
            state = profilesState,
            onBack = { showProfilesScreen = false },
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
    if (showWorkplaceRenameDialog) {
        WorkplacesRenameDialog(
            workplaces = workplaces,
            onDismiss = { showWorkplaceRenameDialog = false },
            onSave = { namesById ->
                var changedCount = 0
                namesById.forEach { (workplaceId, name) ->
                    if (workAssignmentsStore.renameWorkplace(workplaceId, name)) {
                        changedCount += 1
                    }
                }
                if (changedCount > 0) {
                    showInfoSnackbar("Названия работ обновлены")
                }
                showWorkplaceRenameDialog = false
            }
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
            googleAccountEmail = googleSyncMeta.accountEmail,
            lastUploadAtMillis = googleSyncMeta.lastUploadAt,
            lastRestoreAtMillis = googleSyncMeta.lastRestoreAt,
            lastCloudModifiedAtMillis = googleSyncMeta.lastCloudModifiedAt,
            autoUploadEnabled = googleSyncMeta.autoUploadEnabled,
            autoUploadIntervalHours = googleSyncMeta.autoUploadIntervalHours,
            statusMessage = backupRestoreStatusMessage,
            oauthPackageName = appSigningDiagnostics.packageName,
            oauthSha1 = appSigningDiagnostics.sha1.orEmpty(),
            oauthSha256 = appSigningDiagnostics.sha256.orEmpty(),
            onBack = { showBackupRestoreScreen = false },
            onExport = {
                pendingBackupJsonContent = buildCurrentBackupJson()
                pendingBackupFileName = "ShiftSalaryPlanner_backup_${LocalDate.now()}.json"
                backupJsonLauncher.launch(pendingBackupFileName)
            },
            onImport = {
                backupImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onGoogleSignIn = {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            onGoogleSignOut = {
                googleSignInClient.signOut().addOnCompleteListener {
                    googleSignedInAccount = null
                    autoUploadCheckedForAccount = ""
                    backupRestoreStatusMessage = "Google-аккаунт отключён"
                }
            },
            onUploadToCloud = {
                val account = resolveGoogleAccount()
                if (account == null) {
                    backupRestoreStatusMessage = "Сначала войди в Google-аккаунт"
                    return@BackupRestoreScreen
                }
                uploadBackupToCloud(account, false)
            },
            onRestoreFromCloud = {
                val account = resolveGoogleAccount()
                if (account == null) {
                    backupRestoreStatusMessage = "Сначала войди в Google-аккаунт"
                    return@BackupRestoreScreen
                }
                scope.launch {
                    backupRestoreStatusMessage = "Загружаем копию из Google Drive..."
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
                            upsertShiftTemplate = { template -> shiftTemplateDao.upsert(template) },
                            deleteShiftTemplate = { template -> shiftTemplateDao.delete(template) },
                            upsertShiftDay = { day -> shiftDayDao.upsert(day) },
                            deleteShiftDayByDate = { date -> shiftDayDao.deleteByDate(date) },
                            onStatus = { message -> backupRestoreStatusMessage = message },
                            onAfterImport = {
                                googleDriveSyncStore.markRestore(
                                    cloudModifiedAtMillis = downloaded.remoteFile.modifiedAtMillis
                                )
                                (context as? Activity)?.recreate()
                            }
                        )
                    }.onFailure { error ->
                        backupRestoreStatusMessage =
                            "Ошибка восстановления из Google Drive: ${error.message ?: "неизвестно"}"
                    }
                }
            },
            onAutoUploadEnabledChange = { enabled ->
                googleDriveSyncStore.setAutoUploadEnabled(enabled)
                backupRestoreStatusMessage = if (enabled) {
                    "Автозагрузка включена"
                } else {
                    "Автозагрузка отключена"
                }
                autoUploadCheckedForAccount = ""
            },
            onAutoUploadIntervalHoursChange = { hours ->
                googleDriveSyncStore.setAutoUploadIntervalHours(hours)
                val intervalLabel = if (hours % 24 == 0) {
                    val days = hours / 24
                    if (days == 1) "24ч" else "${days}д"
                } else {
                    "${hours}ч"
                }
                backupRestoreStatusMessage = "Интервал автозагрузки: $intervalLabel"
                autoUploadCheckedForAccount = ""
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
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onSaveDisplaySettings = { settings ->
                writeWidgetDisplaySettings(widgetSettingsPrefs, settings)
                widgetSettingsRefreshToken++
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onSaveShiftOverride = { shiftCode, override ->
                writeWidgetShiftOverride(widgetSettingsPrefs, shiftCode, override)
                widgetSettingsRefreshToken++
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
            },
            onResetShiftOverride = { shiftCode ->
                clearWidgetShiftOverride(widgetSettingsPrefs, shiftCode)
                widgetSettingsRefreshToken++
                    ShiftMonthWidgetProviderV2.requestUpdate(context)
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            payments = additionalPaymentsForSettingsWorkplace,
            workplaces = workplaces,
            selectedWorkplaceId = settingsWorkplaceId,
            onSwitchWorkplace = { settingsWorkplaceId = it },
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
                additionalPaymentsStore.deleteById(payment.id)
                showUndoSnackbar("Начисление удалено") {
                    additionalPaymentsStore.addOrUpdate(payment)
                }
            }
        )
    }
    AnimatedFullscreenOverlay(visible = showDeductionsScreen) {
        DeductionsManagementScreen(
            deductions = deductionsForSettingsWorkplace,
            workplaces = workplaces,
            selectedWorkplaceId = settingsWorkplaceId,
            onSwitchWorkplace = { settingsWorkplaceId = it },
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
    if (showAdditionalPaymentDialog) {
        AdditionalPaymentDialog(
            currentPayment = editingAdditionalPayment,
            currentMonth = currentMonth,
            onDismiss = {
                showAdditionalPaymentDialog = false
                editingAdditionalPaymentId = null
            },
            onSave = { payment ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    additionalPaymentsStore.addOrUpdate(
                        payment.copy(workplaceId = settingsWorkplaceId)
                    )
                }
                showInfoSnackbar("Начисление сохранено")
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                deductionsStore.addOrUpdate(
                    deduction.copy(workplaceId = settingsWorkplaceId)
                )
                showInfoSnackbar("Удержание сохранено")
                showDeductionEditorScreen = false
                editingDeductionId = null
            }
        )
    }
    AnimatedFullscreenOverlay(visible = showShiftTemplateEditDialog) {
        ShiftTemplateEditorScreen(
            currentTemplate = editingShiftTemplate,
            workplaces = workplaces,
            defaultWorkplaceId = if (creatingSystemStatus) WORKPLACE_MAIN_ID else activeWorkplaceId,
            isSystemStatusEditor = creatingSystemStatus ||
                    isSystemStatusCode(editingShiftTemplate?.code.orEmpty(), systemStatusCodes),
            currentSpecialRule = editingShiftSpecialRule,
            currentAlarmTemplateConfig = editingShiftAlarmTemplateConfig,
            onBack = {
                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
                creatingSystemStatus = false
            },
            onSave = { template, alarmTemplateConfig, _ ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val oldTemplate = editingShiftTemplate
                val oldCode = oldTemplate?.code
                val saveAsSystemStatus =
                    creatingSystemStatus || isSystemStatusCode(oldTemplate?.code.orEmpty(), systemStatusCodes)
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
                    shiftTemplateDao.upsert(normalizedTemplate)

                    if (oldTemplate != null && oldCode != null && oldCode != normalizedTemplate.code) {
                        savedDays
                            .filter { it.shiftCode == oldCode }
                            .forEach { day ->
                                shiftDayDao.upsert(day.copy(shiftCode = normalizedTemplate.code))
                            }
                        workAssignmentsStore.replaceShiftCode(
                            oldShiftCode = oldCode,
                            newShiftCode = normalizedTemplate.code
                        )

                        shiftTemplateDao.delete(oldTemplate)

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
                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
            },
            onSaveSpecialRule = { code, rule, _ ->
                val saveAsSystemStatus =
                    creatingSystemStatus ||
                            isSystemStatusCode(editingShiftTemplate?.code.orEmpty(), systemStatusCodes)
                saveShiftSpecialRule(
                    shiftSpecialRules = shiftSpecialRules,
                    shiftSpecialPrefs = shiftSpecialPrefs,
                    code = code,
                    rule = rule.copy(isSystemStatus = saveAsSystemStatus)
                )
                creatingSystemStatus = false
            },
            onDelete = { template ->
                scope.launch {
                    val templateWorkplaceId = workplaceIdFromShiftCode(template.code)
                    if (
                        templateWorkplaceId != WORKPLACE_MAIN_ID &&
                        !isSystemStatusCode(template.code, systemStatusCodes)
                    ) {
                        // Prevent auto-bootstrap from restoring templates after manual cleanup.
                        markWorkplaceTemplatesSeeded(workAssignmentsPrefs, templateWorkplaceId)
                    }
                    shiftTemplateDao.delete(template)

                    savedDays
                        .filter { it.shiftCode == template.code }
                        .forEach { day ->
                            shiftDayDao.deleteByDate(day.date)
                        }
                    workAssignmentsStore.removeShiftCode(template.code)

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
                showShiftTemplateEditDialog = false
                editingShiftTemplateCode = null
                creatingSystemStatus = false
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
                patternTemplatesStore.deleteById(pattern.id)
                showUndoSnackbar("Чередование удалено") {
                    patternTemplatesStore.addOrUpdate(pattern)
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
                    val validCodes = shiftTemplates.map { it.code }.toSet()
                    if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                        applyPatternToMonth(
                            shiftDayDao = shiftDayDao,
                            pattern = applyingPattern,
                            cycleStartDate = cycleStartDate,
                            month = currentMonth,
                            validShiftCodes = validCodes
                        )
                    } else {
                        val cycle = applyingPattern.normalizedSteps().take(applyingPattern.usedLength())
                        if (cycle.isNotEmpty()) {
                            var date = currentMonth.atDay(1)
                            val endDate = currentMonth.atEndOfMonth()
                            while (!date.isAfter(endDate)) {
                                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(cycleStartDate, date).toInt()
                                val cycleIndex = ((diffDays % cycle.size) + cycle.size) % cycle.size
                                val code = cycle[cycleIndex]
                                if (code.isBlank()) {
                                    workAssignmentsStore.setShiftForDate(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = null
                                    )
                                } else if (code in validCodes) {
                                    workAssignmentsStore.setShiftForDate(
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    patternTemplatesStore.addOrUpdate(pattern)
                }
                showInfoSnackbar("Чередование сохранено")
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
                    val validCodes = shiftTemplates.map { it.code }.toSet()
                    if (activeWorkplaceId == WORKPLACE_MAIN_ID) {
                        applyPatternToRange(
                            shiftDayDao = shiftDayDao,
                            pattern = activePattern,
                            rangeStart = pendingPatternRangeStartDate,
                            rangeEnd = pendingPatternRangeEndDate,
                            validShiftCodes = validCodes,
                            phaseOffset = phaseOffset
                        )
                    } else {
                        val cycle = activePattern.normalizedSteps().take(activePattern.usedLength())
                        if (cycle.isNotEmpty()) {
                            val rangeStartDate = requireNotNull(pendingPatternRangeStartDate)
                            val rangeEndDate = requireNotNull(pendingPatternRangeEndDate)
                            var date = rangeStartDate
                            while (!date.isAfter(rangeEndDate)) {
                                val diffDays = java.time.temporal.ChronoUnit.DAYS.between(rangeStartDate, date).toInt()
                                val rawIndex = diffDays + phaseOffset
                                val cycleIndex = ((rawIndex % cycle.size) + cycle.size) % cycle.size
                                val code = cycle[cycleIndex]
                                if (code.isBlank()) {
                                    workAssignmentsStore.setShiftForDate(
                                        workplaceId = activeWorkplaceId,
                                        date = date,
                                        shiftCode = null
                                    )
                                } else if (code in validCodes) {
                                    workAssignmentsStore.setShiftForDate(
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

                showPatternPreviewDialog = false
                pendingPatternRangeStartIso = null
                pendingPatternRangeEndIso = null
                activePatternId = null
                selectedTabName = BottomTab.CALENDAR.name
            }
        )
    }

    if (showClearMonthConfirm) {
        AlertDialog(
            onDismissRequest = { showClearMonthConfirm = false },
            title = { Text("Очистить текущий месяц?") },
            text = {
                Text("Будут удалены все смены за ${currentMonth.monthValue.toString().padStart(2, '0')}.${currentMonth.year}.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearMonthConfirm = false
                        val monthStart = currentMonth.atDay(1).toString()
                        val monthEnd = currentMonth.atEndOfMonth().toString()
                        scope.launch {
                            shiftDayDao.deleteByDateRange(monthStart, monthEnd)
                            workAssignmentsStore.clearDateRange(
                                startDate = currentMonth.atDay(1),
                                endDate = currentMonth.atEndOfMonth()
                            )
                        }
                        clearRangeModeActive = false
                        clearRangeStartIso = null
                        pendingClearRangeStartIso = null
                        pendingClearRangeEndIso = null
                        showInfoSnackbar("Текущий месяц очищен")
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMonthConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearAllCalendarConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllCalendarConfirm = false },
            title = { Text("Очистить весь календарь?") },
            text = { Text("Будут удалены все назначенные смены во всех месяцах.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllCalendarConfirm = false
                        scope.launch {
                            shiftDayDao.clearAll()
                            workAssignmentsStore.clearAll()
                        }
                        clearRangeModeActive = false
                        clearRangeStartIso = null
                        pendingClearRangeStartIso = null
                        pendingClearRangeEndIso = null
                        activeBrushCode = null
                        showInfoSnackbar("Календарь полностью очищен")
                    }
                ) {
                    Text("Очистить всё")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllCalendarConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

