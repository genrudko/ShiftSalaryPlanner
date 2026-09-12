package com.vigilante.shiftsalaryplanner

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.vigilante.shiftsalaryplanner.app.ports.AlarmDataPort
import com.vigilante.shiftsalaryplanner.app.ports.AlarmPlatformPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultAlarmDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultAlarmPlatformPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultFinanceDataPort
import com.vigilante.shiftsalaryplanner.app.ports.FinanceDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultScheduleDataPort
import com.vigilante.shiftsalaryplanner.app.ports.ScheduleDataPort
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.HolidaySyncRepository
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
    val alarmPlatform: AlarmPlatformPort,
    val excelScheduleParser: ExcelScheduleParser
)

data class ProfileDependencies(
    val financeData: FinanceDataPort,
    val scheduleData: ScheduleDataPort,
    val alarmData: AlarmDataPort,
    val patternTemplatesStore: PatternTemplatesStore,
    val appEventLogStore: AppEventLogStore,
    val appWorkflowSettingsStore: AppWorkflowSettingsStore,
    val assistantAiSettingsStore: AssistantAiSettingsStore,
    val appNotesStore: AppNotesStore,
    val todayLayoutSettingsStore: TodayLayoutSettingsStore,
    val googleDriveSyncStore: GoogleDriveSyncStore,
    val database: AppDatabase,
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
        alarmPlatform = DefaultAlarmPlatformPort(appContext),
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
    val workAssignmentsStore = WorkAssignmentsStore(appContext)

    return ProfileDependencies(
        financeData = DefaultFinanceDataPort(
            payrollSettingsStore = PayrollSettingsStore(appContext),
            workplacePayrollSettingsStore = WorkplacePayrollSettingsStore(appContext),
            additionalPaymentsStore = AdditionalPaymentsStore(appContext),
            deductionsStore = DeductionsStore(appContext),
            reportVisibilitySettingsStore = ReportVisibilitySettingsStore(appContext),
            reportHistoryStore = ReportHistoryStore(appContext)
        ),
        scheduleData = DefaultScheduleDataPort(
            shiftDayDao = shiftDayDao,
            shiftTemplateDao = shiftTemplateDao,
            holidayDao = holidayDao,
            workAssignmentsStore = workAssignmentsStore
        ),
        alarmData = DefaultAlarmDataPort(ShiftAlarmStore(appContext)),
        patternTemplatesStore = PatternTemplatesStore(appContext),
        appEventLogStore = AppEventLogStore(appContext),
        appWorkflowSettingsStore = AppWorkflowSettingsStore(appContext),
        assistantAiSettingsStore = AssistantAiSettingsStore(appContext),
        appNotesStore = AppNotesStore(appContext),
        todayLayoutSettingsStore = TodayLayoutSettingsStore(appContext),
        googleDriveSyncStore = GoogleDriveSyncStore(appContext),
        database = database,
        holidaySyncRepository = HolidaySyncRepository(holidayDao),
        excelScheduleImporter = ExcelScheduleImporter(shiftTemplateDao, shiftDayDao)
    )
}
