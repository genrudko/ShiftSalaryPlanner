package com.vigilante.shiftsalaryplanner

import android.content.Context
import com.vigilante.shiftsalaryplanner.app.ports.ActivityLogPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultActivityLogPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultNotesDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultSettingsDataPort
import com.vigilante.shiftsalaryplanner.app.ports.NotesDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultProfileDataPort
import com.vigilante.shiftsalaryplanner.app.ports.ProfileDataPort
import com.vigilante.shiftsalaryplanner.app.ports.SettingsDataPort
import com.vigilante.shiftsalaryplanner.app.ports.AlarmDataPort
import com.vigilante.shiftsalaryplanner.app.ports.AlarmPlatformPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultAlarmDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultAlarmPlatformPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultFinanceDataPort
import com.vigilante.shiftsalaryplanner.app.ports.FinanceDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultScheduleDataPort
import com.vigilante.shiftsalaryplanner.app.ports.ScheduleDataPort
import com.vigilante.shiftsalaryplanner.app.ports.DefaultServiceOperationsPort
import com.vigilante.shiftsalaryplanner.app.ports.ServiceOperationsPort
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
    val profileData: ProfileDataPort,
    val alarmPlatform: AlarmPlatformPort
)

data class ProfileDependencies(
    val financeData: FinanceDataPort,
    val scheduleData: ScheduleDataPort,
    val alarmData: AlarmDataPort,
    val notesData: NotesDataPort,
    val settingsData: SettingsDataPort,
    val activityLog: ActivityLogPort,
    val serviceOperations: ServiceOperationsPort
)

fun createAppDependencies(context: Context): AppDependencies {
    val appContext = context.applicationContext
    return AppDependencies(
        profileData = DefaultProfileDataPort(appContext),
        alarmPlatform = DefaultAlarmPlatformPort(appContext)
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
    val scheduleData = DefaultScheduleDataPort(
        shiftDayDao = shiftDayDao,
        shiftTemplateDao = shiftTemplateDao,
        holidayDao = holidayDao,
        workAssignmentsStore = workAssignmentsStore
    )

    return ProfileDependencies(
        financeData = DefaultFinanceDataPort(
            payrollSettingsStore = PayrollSettingsStore(appContext),
            workplacePayrollSettingsStore = WorkplacePayrollSettingsStore(appContext),
            additionalPaymentsStore = AdditionalPaymentsStore(appContext),
            deductionsStore = DeductionsStore(appContext),
            reportVisibilitySettingsStore = ReportVisibilitySettingsStore(appContext),
            reportHistoryStore = ReportHistoryStore(appContext)
        ),
        scheduleData = scheduleData,
        alarmData = DefaultAlarmDataPort(ShiftAlarmStore(appContext)),
        notesData = DefaultNotesDataPort(AppNotesStore(appContext)),
        settingsData = DefaultSettingsDataPort(
            workflowStore = AppWorkflowSettingsStore(appContext),
            assistantStore = AssistantAiSettingsStore(appContext),
            todayLayoutStore = TodayLayoutSettingsStore(appContext),
            patternStore = PatternTemplatesStore(appContext)
        ),
        activityLog = DefaultActivityLogPort(AppEventLogStore(appContext)),
        serviceOperations = DefaultServiceOperationsPort(
            context = appContext,
            holidaySyncRepository = HolidaySyncRepository(holidayDao),
            excelScheduleParser = ExcelScheduleParser(),
            excelScheduleImporter = ExcelScheduleImporter(shiftTemplateDao, shiftDayDao),
            googleDriveSyncStore = GoogleDriveSyncStore(appContext),
            scheduleData = scheduleData
        )
    )
}
