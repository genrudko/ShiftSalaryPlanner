package com.vigilante.shiftsalaryplanner.app.ports

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.vigilante.shiftsalaryplanner.CalendarSyncCheckResult
import com.vigilante.shiftsalaryplanner.ManualHolidayRecord
import com.vigilante.shiftsalaryplanner.PREFS_CALENDAR_SYNC
import com.vigilante.shiftsalaryplanner.buildBackupJsonForExport
import com.vigilante.shiftsalaryplanner.checkAndSyncFederalCalendarIfChanged
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.downloadBackupFromGoogleDriveAppData
import com.vigilante.shiftsalaryplanner.excel.ExcelImportParseResult
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelImportRequest
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleParser
import com.vigilante.shiftsalaryplanner.restoreBackupFromRawJson as restoreBackupFromRawJsonHelper
import com.vigilante.shiftsalaryplanner.restoreBackupFromUri as restoreBackupFromUriHelper
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncMeta
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncStore
import com.vigilante.shiftsalaryplanner.settings.profileSharedPreferences
import com.vigilante.shiftsalaryplanner.uploadBackupToGoogleDriveAppData
import com.vigilante.shiftsalaryplanner.GoogleDriveBackupDownloadResult
import com.vigilante.shiftsalaryplanner.GoogleDriveBackupUploadResult
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ServiceOperationsPort {
    val googleSyncMeta: Flow<GoogleDriveSyncMeta>

    suspend fun syncFederalCalendar(
        year: Int,
        hasLocalYear: Boolean,
        forceNetworkCheck: Boolean
    ): CalendarSyncCheckResult

    fun parseExcelSchedule(
        bytes: ByteArray,
        request: ExcelImportRequest,
        existingTemplates: List<ShiftTemplateEntity>
    ): ExcelImportParseResult

    suspend fun importExcelSchedule(preview: ExcelImportPreview)

    fun buildBackupJson(
        prefSnapshots: List<Pair<String, SharedPreferences>>,
        shiftDays: List<ShiftDayEntity>,
        shiftTemplates: List<ShiftTemplateEntity>
    ): String

    suspend fun restoreBackupFromUri(
        uri: Uri,
        existingShiftTemplates: List<ShiftTemplateEntity>,
        existingSavedDays: List<ShiftDayEntity>,
        manualHolidayPrefs: SharedPreferences,
        shiftColorsPrefs: SharedPreferences,
        manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
        shiftColors: SnapshotStateMap<String, Int>,
        onStatus: (String) -> Unit,
        onAfterImport: () -> Unit
    )

    suspend fun restoreBackupFromRawJson(
        rawJson: String,
        existingShiftTemplates: List<ShiftTemplateEntity>,
        existingSavedDays: List<ShiftDayEntity>,
        manualHolidayPrefs: SharedPreferences,
        shiftColorsPrefs: SharedPreferences,
        manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
        shiftColors: SnapshotStateMap<String, Int>,
        onStatus: (String) -> Unit,
        onAfterImport: () -> Unit
    )

    fun resolveGoogleAccount(current: GoogleSignInAccount?): GoogleSignInAccount?
    fun hasGoogleDrivePermission(account: GoogleSignInAccount): Boolean
    fun googleSignInIntent(): Intent
    fun parseGoogleSignInResult(intent: Intent): GoogleSignInAccount
    fun signOutGoogleAccount(onComplete: () -> Unit)
    suspend fun uploadBackupToCloud(account: GoogleSignInAccount, backupJson: String): GoogleDriveBackupUploadResult
    suspend fun downloadBackupFromCloud(account: GoogleSignInAccount): GoogleDriveBackupDownloadResult
    fun setGoogleAccountEmail(email: String)
    fun markGoogleUpload(cloudModifiedAtMillis: Long)
    fun markGoogleRestore(cloudModifiedAtMillis: Long)
    fun setAutoUploadEnabled(enabled: Boolean)
    fun setAutoUploadIntervalHours(hours: Int)
}

@Suppress("DEPRECATION")
class DefaultServiceOperationsPort(
    context: Context,
    private val holidaySyncRepository: HolidaySyncRepository,
    private val excelScheduleParser: ExcelScheduleParser,
    private val excelScheduleImporter: ExcelScheduleImporter,
    private val googleDriveSyncStore: GoogleDriveSyncStore,
    private val scheduleData: ScheduleDataPort
) : ServiceOperationsPort {
    private val appContext = context.applicationContext
    private val calendarSyncPrefs = appContext.profileSharedPreferences(PREFS_CALENDAR_SYNC)
    private val googleDriveScope = Scope(DriveScopes.DRIVE_APPDATA)
    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(
        appContext,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(googleDriveScope)
            .build()
    )

    override val googleSyncMeta: Flow<GoogleDriveSyncMeta> = googleDriveSyncStore.metaFlow

    override suspend fun syncFederalCalendar(
        year: Int,
        hasLocalYear: Boolean,
        forceNetworkCheck: Boolean
    ): CalendarSyncCheckResult = checkAndSyncFederalCalendarIfChanged(
        holidaySyncRepository = holidaySyncRepository,
        prefs = calendarSyncPrefs,
        year = year,
        hasLocalYear = hasLocalYear,
        forceNetworkCheck = forceNetworkCheck
    )

    override fun parseExcelSchedule(
        bytes: ByteArray,
        request: ExcelImportRequest,
        existingTemplates: List<ShiftTemplateEntity>
    ): ExcelImportParseResult = excelScheduleParser.parse(
        inputStream = bytes.inputStream(),
        request = request,
        existingTemplates = existingTemplates
    )

    override suspend fun importExcelSchedule(preview: ExcelImportPreview) {
        preview.selectedMonths.sorted().forEach { month ->
            val start = LocalDate.of(preview.year, month, 1)
            val end = YearMonth.of(preview.year, month).atEndOfMonth()
            excelScheduleImporter.clearPeriod(start, end)
        }
        excelScheduleImporter.import(preview)
    }

    override fun buildBackupJson(
        prefSnapshots: List<Pair<String, SharedPreferences>>,
        shiftDays: List<ShiftDayEntity>,
        shiftTemplates: List<ShiftTemplateEntity>
    ): String = buildBackupJsonForExport(
        prefSnapshots = prefSnapshots,
        shiftDays = shiftDays,
        shiftTemplates = shiftTemplates
    )

    override suspend fun restoreBackupFromUri(
        uri: Uri,
        existingShiftTemplates: List<ShiftTemplateEntity>,
        existingSavedDays: List<ShiftDayEntity>,
        manualHolidayPrefs: SharedPreferences,
        shiftColorsPrefs: SharedPreferences,
        manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
        shiftColors: SnapshotStateMap<String, Int>,
        onStatus: (String) -> Unit,
        onAfterImport: () -> Unit
    ) = restoreBackupFromUriHelper(
        context = appContext,
        uri = uri,
        existingShiftTemplates = existingShiftTemplates,
        existingSavedDays = existingSavedDays,
        manualHolidayPrefs = manualHolidayPrefs,
        shiftColorsPrefs = shiftColorsPrefs,
        manualHolidayRecords = manualHolidayRecords,
        shiftColors = shiftColors,
        upsertShiftTemplate = scheduleData::upsertShiftTemplate,
        deleteShiftTemplate = scheduleData::deleteShiftTemplate,
        upsertShiftDay = scheduleData::upsertShiftDay,
        deleteShiftDayByDate = scheduleData::deleteShiftDay,
        onStatus = onStatus,
        onAfterImport = onAfterImport
    )

    override suspend fun restoreBackupFromRawJson(
        rawJson: String,
        existingShiftTemplates: List<ShiftTemplateEntity>,
        existingSavedDays: List<ShiftDayEntity>,
        manualHolidayPrefs: SharedPreferences,
        shiftColorsPrefs: SharedPreferences,
        manualHolidayRecords: SnapshotStateList<ManualHolidayRecord>,
        shiftColors: SnapshotStateMap<String, Int>,
        onStatus: (String) -> Unit,
        onAfterImport: () -> Unit
    ) = restoreBackupFromRawJsonHelper(
        context = appContext,
        rawJson = rawJson,
        existingShiftTemplates = existingShiftTemplates,
        existingSavedDays = existingSavedDays,
        manualHolidayPrefs = manualHolidayPrefs,
        shiftColorsPrefs = shiftColorsPrefs,
        manualHolidayRecords = manualHolidayRecords,
        shiftColors = shiftColors,
        upsertShiftTemplate = scheduleData::upsertShiftTemplate,
        deleteShiftTemplate = scheduleData::deleteShiftTemplate,
        upsertShiftDay = scheduleData::upsertShiftDay,
        deleteShiftDayByDate = scheduleData::deleteShiftDay,
        onStatus = onStatus,
        onAfterImport = onAfterImport
    )

    override fun resolveGoogleAccount(current: GoogleSignInAccount?): GoogleSignInAccount? {
        return current?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
            ?: GoogleSignIn.getLastSignedInAccount(appContext)
                ?.takeIf { GoogleSignIn.hasPermissions(it, googleDriveScope) }
    }

    override fun hasGoogleDrivePermission(account: GoogleSignInAccount): Boolean =
        GoogleSignIn.hasPermissions(account, googleDriveScope)

    override fun googleSignInIntent(): Intent = googleSignInClient.signInIntent

    override fun parseGoogleSignInResult(intent: Intent): GoogleSignInAccount {
        return GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException::class.java)
    }

    override fun signOutGoogleAccount(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener { onComplete() }
    }

    override suspend fun uploadBackupToCloud(
        account: GoogleSignInAccount,
        backupJson: String
    ): GoogleDriveBackupUploadResult = withContext(Dispatchers.IO) {
        uploadBackupToGoogleDriveAppData(appContext, account, backupJson)
    }

    override suspend fun downloadBackupFromCloud(
        account: GoogleSignInAccount
    ): GoogleDriveBackupDownloadResult = withContext(Dispatchers.IO) {
        downloadBackupFromGoogleDriveAppData(appContext, account)
    }

    override fun setGoogleAccountEmail(email: String) = googleDriveSyncStore.setAccountEmail(email)

    override fun markGoogleUpload(cloudModifiedAtMillis: Long) {
        googleDriveSyncStore.markUpload(cloudModifiedAtMillis = cloudModifiedAtMillis)
    }

    override fun markGoogleRestore(cloudModifiedAtMillis: Long) {
        googleDriveSyncStore.markRestore(cloudModifiedAtMillis = cloudModifiedAtMillis)
    }

    override fun setAutoUploadEnabled(enabled: Boolean) = googleDriveSyncStore.setAutoUploadEnabled(enabled)

    override fun setAutoUploadIntervalHours(hours: Int) = googleDriveSyncStore.setAutoUploadIntervalHours(hours)
}
