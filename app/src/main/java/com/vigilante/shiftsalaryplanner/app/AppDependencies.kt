package com.vigilante.shiftsalaryplanner

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.HolidayDao
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
import com.vigilante.shiftsalaryplanner.data.ShiftDayDao
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateDao
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleParser
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplatesStore
import com.vigilante.shiftsalaryplanner.settings.AdditionalPaymentsStore
import com.vigilante.shiftsalaryplanner.settings.AppEventLogStore
import com.vigilante.shiftsalaryplanner.settings.AppNotesStore
import com.vigilante.shiftsalaryplanner.settings.AppProfileStore
import com.vigilante.shiftsalaryplanner.settings.AppWorkflowSettingsStore
import com.vigilante.shiftsalaryplanner.settings.AssistantAiSettingsStore
import com.vigilante.shiftsalaryplanner.settings.DeductionsStore
import com.vigilante.shiftsalaryplanner.settings.GoogleDriveSyncStore
import com.vigilante.shiftsalaryplanner.settings.PayrollSettingsStore
import com.vigilante.shiftsalaryplanner.settings.ReportHistoryStore
import com.vigilante.shiftsalaryplanner.settings.ReportVisibilitySettingsStore
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.settings.TodayLayoutSettingsStore
import com.vigilante.shiftsalaryplanner.settings.WorkAssignmentsStore
import com.vigilante.shiftsalaryplanner.settings.WorkplacePayrollSettingsStore

data class AppDependencies(
    val profileStore: AppProfileStore,
    val googleDriveScope: Scope,
    val googleSignInClient: GoogleSignInClient,
    val excelScheduleParser: ExcelScheduleParser
)

data class ProfileDependencies(
    val payrollSettingsStore: PayrollSettingsStore,
    val reportVisibilitySettingsStore: ReportVisibilitySettingsStore,
    val workAssignmentsStore: WorkAssignmentsStore,
    val workplacePayrollSettingsStore: WorkplacePayrollSettingsStore,
    val shiftAlarmStore: ShiftAlarmStore,
    val patternTemplatesStore: PatternTemplatesStore,
    val additionalPaymentsStore: AdditionalPaymentsStore,
    val deductionsStore: DeductionsStore,
    val appEventLogStore: AppEventLogStore,
    val reportHistoryStore: ReportHistoryStore,
    val appWorkflowSettingsStore: AppWorkflowSettingsStore,
    val assistantAiSettingsStore: AssistantAiSettingsStore,
    val appNotesStore: AppNotesStore,
    val todayLayoutSettingsStore: TodayLayoutSettingsStore,
    val googleDriveSyncStore: GoogleDriveSyncStore,
    val database: AppDatabase,
    val shiftDayDao: ShiftDayDao,
    val shiftTemplateDao: ShiftTemplateDao,
    val holidayDao: HolidayDao,
    val holidaySyncRepository: HolidaySyncRepository,
    val excelScheduleImporter: ExcelScheduleImporter
)

fun createAppDependencies(context: Context): AppDependencies {
    val appContext = context.applicationContext
    val googleDriveScope = Scope(DriveScopes.DRIVE_APPDATA)
    val googleSignInClient = GoogleSignIn.getClient(
        appContext,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(googleDriveScope)
            .build()
    )

    return AppDependencies(
        profileStore = AppProfileStore(appContext),
        googleDriveScope = googleDriveScope,
        googleSignInClient = googleSignInClient,
        excelScheduleParser = ExcelScheduleParser()
    )
}

fun createProfileDependencies(
    context: Context,
    activeProfileId: String
): ProfileDependencies {
    val appContext = context.applicationContext
    val database = AppDatabase.getDatabase(appContext, activeProfileId)
    val shiftDayDao = database.shiftDayDao()
    val shiftTemplateDao = database.shiftTemplateDao()
    val holidayDao = database.holidayDao()

    return ProfileDependencies(
        payrollSettingsStore = PayrollSettingsStore(appContext),
        reportVisibilitySettingsStore = ReportVisibilitySettingsStore(appContext),
        workAssignmentsStore = WorkAssignmentsStore(appContext),
        workplacePayrollSettingsStore = WorkplacePayrollSettingsStore(appContext),
        shiftAlarmStore = ShiftAlarmStore(appContext),
        patternTemplatesStore = PatternTemplatesStore(appContext),
        additionalPaymentsStore = AdditionalPaymentsStore(appContext),
        deductionsStore = DeductionsStore(appContext),
        appEventLogStore = AppEventLogStore(appContext),
        reportHistoryStore = ReportHistoryStore(appContext),
        appWorkflowSettingsStore = AppWorkflowSettingsStore(appContext),
        assistantAiSettingsStore = AssistantAiSettingsStore(appContext),
        appNotesStore = AppNotesStore(appContext),
        todayLayoutSettingsStore = TodayLayoutSettingsStore(appContext),
        googleDriveSyncStore = GoogleDriveSyncStore(appContext),
        database = database,
        shiftDayDao = shiftDayDao,
        shiftTemplateDao = shiftTemplateDao,
        holidayDao = holidayDao,
        holidaySyncRepository = HolidaySyncRepository(holidayDao),
        excelScheduleImporter = ExcelScheduleImporter(shiftTemplateDao, shiftDayDao)
    )
}
