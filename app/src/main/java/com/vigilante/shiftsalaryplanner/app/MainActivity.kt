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
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.ui.theme.AppColorSchemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppFontMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
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
        val initialWidgetTab = parseInitialWidgetTab(intent?.getStringExtra(EXTRA_OPEN_TAB))
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
    return runCatching { BottomTab.valueOf(raw).name }.getOrNull()
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
    appearanceSettings: AppearanceSettings,
    onSaveAppearanceSettings: (AppearanceSettings) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var quickPickerOpen by rememberSaveable { mutableStateOf(false) }
    var activeBrushCode by rememberSaveable { mutableStateOf<String?>(null) }
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
    var payrollPeriodModeName by rememberSaveable { mutableStateOf(PayrollPeriodMode.MONTH.name) }
    var payrollSelectedYear by rememberSaveable { mutableIntStateOf(currentMonth.year) }
    var payrollRangeStartIso by rememberSaveable { mutableStateOf(currentMonth.atDay(1).toString()) }
    var payrollRangeEndIso by rememberSaveable { mutableStateOf(currentMonth.atEndOfMonth().toString()) }
    var isLegendExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedTabName by rememberSaveable(initialTabName) {
        mutableStateOf(initialTabName ?: BottomTab.CALENDAR.name)
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

    val selectedTab = BottomTab.valueOf(selectedTabName)
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
    val shiftAlarmSettingsPrefs = remember(activeProfileId) {
        context.profileSharedPreferences("shift_alarm_settings")
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

        val normalizedNightPercent = payrollSettings.nightPercent
            .coerceAtLeast(0.0)
            .let { value ->
                if (value > 3.0 && value <= 100.0) value / 100.0 else value
            }
        val normalizedNdflPercent = payrollSettings.ndflPercent
            .coerceAtLeast(0.0)
            .let { value ->
                if (value > 1.0 && value <= 100.0) value / 100.0 else value
            }

        payrollSettings.copy(
            monthlyNormHours = effectiveNormHours,
            vacationAverageDaily = computedVacationAverageDaily,
            sickAverageDaily = computedSickAverageDaily,
            sickCalculationPeriodDays = resolvedSickCalculationPeriodDays,
            nightPercent = normalizedNightPercent,
            ndflPercent = normalizedNdflPercent
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

    val periodEntries = remember(shiftCodesByDate, payrollPeriodStartDate, payrollPeriodEndDate) {
        shiftCodesByDate.filterKeys { date ->
            !date.isBefore(payrollPeriodStartDate) && !date.isAfter(payrollPeriodEndDate)
        }
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

    val firstHalfShifts = remember(
        periodEntries,
        templateMap,
        resolvedHolidayMap,
        payrollSettings.applyShortDayReduction,
        shiftSpecialRulesSnapshot,
        shiftTemplateTimingByCode
    ) {
        periodEntries
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

    val summary = remember(periodShifts) {
        calculateSummaryForShifts(periodShifts)
    }

    val paymentResolution = remember(
        additionalPayments,
        payrollPeriodStartDate,
        payrollPeriodEndDate,
        periodShifts
    ) {
        resolveAdditionalPaymentsForPeriod(
            configuredPayments = additionalPayments,
            startDate = payrollPeriodStartDate,
            endDate = payrollPeriodEndDate,
            shifts = periodShifts
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
        deductions
    ) {
        PayrollCalculator.calculate(
            shifts = periodShifts,
            firstHalfShifts = firstHalfShifts,
            settings = payrollCalculationSettings,
            additionalPayments = paymentResolution.asPayrollPayments(),
            deductions = deductions
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
        payrollPeriodLabel,
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
            periodLabel = payrollPeriodLabel,
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

    val backupPrefSnapshots = listOf(
        PREF_NAME_APPEARANCE_SETTINGS to appearanceSettingsPrefs,
        PREF_NAME_PAYROLL_SETTINGS to payrollSettingsPrefs,
        PREF_NAME_PAYROLL_YTD to payrollYtdPrefs,
        PREF_NAME_REPORT_VISIBILITY_SETTINGS to reportVisibilitySettingsPrefs,
        PREF_NAME_ADDITIONAL_PAYMENTS to additionalPaymentsPrefs,
        PREF_NAME_PAYROLL_DEDUCTIONS to payrollDeductionsPrefs,
        PREF_NAME_PATTERN_TEMPLATES to patternTemplatesPrefs,
        PREF_NAME_SHIFT_ALARM_SETTINGS to shiftAlarmSettingsPrefs,
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
                            shiftCodesByDate = shiftCodesByDate,
                            templateMap = templateMap,
                            shiftColors = shiftColors,
                            quickShiftTemplates = quickShiftTemplates,
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
                                periodMode = payrollPeriodMode,
                                periodStartDate = payrollPeriodStartDate,
                                periodEndDate = payrollPeriodEndDate,
                                periodLabel = payrollPeriodLabel,
                                periodFileLabel = payrollPeriodFileLabel,
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
                                onOpenSettings = { showPayrollSettings = true },
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
                                    startInAppAlarmPreview(context)
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
                                onDuplicateShift = { template ->
                                    scope.launch {
                                        val existingCodes = shiftTemplates.map { it.code }.toSet()
                                        val baseCode = template.code.ifBlank { "S" }
                                        var suffix = 2
                                        var duplicatedCode = "$baseCode$suffix"
                                        while (duplicatedCode in existingCodes) {
                                            suffix += 1
                                            duplicatedCode = "$baseCode$suffix"
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

                                        showInfoSnackbar("Смена \"$duplicatedCode\" создана")
                                    }
                                },
                                onDeleteShift = { template ->
                                    scope.launch {
                                        val linkedDays = savedDays.filter { it.shiftCode == template.code }
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

                                        showUndoSnackbar("Смена \"${template.code}\" удалена") {
                                            scope.launch {
                                                shiftTemplateDao.upsert(template)
                                                linkedDays.forEach { day ->
                                                    shiftDayDao.upsert(day)
                                                }
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
                            onOpenDeductions = { showDeductionsScreen = true },
                            manualHolidayCount = manualHolidayRecords.size,
                            isHolidaySyncing = isHolidaySyncing,
                            holidaySyncMessage = holidaySyncMessage,
                            onOpenPayrollSettings = { showPayrollSettings = true },
                            onOpenAppearanceSettings = { showAppearanceSettings = true },
                            onOpenReportVisibilitySettings = { showReportVisibilitySettings = true },
                            onOpenColorSettings = { selectedTabName = BottomTab.SHIFTS.name },
                            onOpenPayments = { showAdditionalPaymentsScreen = true },
                            onOpenCurrentParameters = { showCurrentParameters = true },
                            onOpenManualHolidays = { showManualHolidaysScreen = true },
                            onOpenBackupRestore = { showBackupRestoreScreen = true },
                            onOpenExcelImport = { showExcelImportScreen = true },
                            onOpenWidgetSettings = { showWidgetSettingsScreen = true },
                            onOpenProfiles = { showProfilesScreen = true },
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
            },
            onExportPdf = {
                pendingReportPdfBytes = buildMonthlyReportPdf(
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
                pendingReportPdfFileName =
                    "report_${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}.pdf"
                reportPdfLauncher.launch(pendingReportPdfFileName)
            }
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

    AnimatedFullscreenOverlay(visible = showPayrollSettings) {
        PayrollSettingsDialog(
            currentSettings = payrollSettings,
            onDismiss = { showPayrollSettings = false },
            onSave = { newSettings ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    payrollSettingsStore.save(newSettings)
                }
                showPayrollSettings = false
            }
        )
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
            onActivateProfile = { profileId ->
                if (profilesState.activeProfileId != profileId && profileStore.setActiveProfile(profileId)) {
                    showInfoSnackbar("Профиль переключён")
                    (context as? Activity)?.recreate()
                }
            },
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
                additionalPaymentsStore.deleteById(payment.id)
                showUndoSnackbar("Начисление удалено") {
                    additionalPaymentsStore.addOrUpdate(payment)
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
                    additionalPaymentsStore.addOrUpdate(payment)
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
                deductionsStore.addOrUpdate(deduction)
                showInfoSnackbar("Удержание сохранено")
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        key = template.code,
                        colorValue = parseColorHex(template.colorHex, 0xFFE0E0E0.toInt())
                    )
                    shiftAlarmStore.upsertTemplateConfig(alarmTemplateConfig.copy(shiftCode = template.code))
                }

                showInfoSnackbar("Смена сохранена")
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

