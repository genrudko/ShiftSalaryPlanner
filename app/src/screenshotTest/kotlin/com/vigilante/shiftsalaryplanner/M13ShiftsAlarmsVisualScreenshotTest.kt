package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
import java.time.Instant

private val m13Workplaces = listOf(
    Workplace(WORKPLACE_MAIN_ID, "Северный парк"),
    Workplace(WORKPLACE_SECOND_ID, "Подстанция")
)

private fun m13Template(
    code: String,
    title: String,
    iconKey: String,
    colorHex: String,
    order: Int,
    nightHours: Double = 0.0
) = ShiftTemplateEntity(
    code = code,
    title = title,
    iconKey = iconKey,
    totalHours = 12.0,
    breakHours = 1.0,
    nightHours = nightHours,
    colorHex = colorHex,
    isWeekendPaid = false,
    active = true,
    sortOrder = order,
    shiftPayAmount = 0.0
)

private val m13Day = m13Template("Д", "Дневная", "TEXT", "#A9D6FF", 0)
private val m13Night = m13Template("Н", "Ночная", "MOON", "#C8B6FF", 1, nightHours = 8.0)
private val m13SecondDay = m13Template(
    workplaceScopedShiftCode(WORKPLACE_SECOND_ID, "Д"),
    "Дневная · Подстанция",
    "TEXT",
    "#B8E0C2",
    2
)
private val m13Templates = listOf(m13Day, m13Night, m13SecondDay)

private val m13DayAlarm = ShiftAlarmConfig(
    id = "m13-day-0545",
    title = "На дневную",
    manualTitle = true,
    triggerHour = 5,
    triggerMinute = 45,
    enabled = true
)
private val m13NightAlarm = ShiftAlarmConfig(
    id = "m13-night-1840",
    title = "На ночную",
    manualTitle = true,
    triggerHour = 18,
    triggerMinute = 40,
    enabled = true
)
private val m13DayAlarmConfig = ShiftTemplateAlarmConfig(
    shiftCode = "Д",
    enabled = true,
    startHour = 8,
    startMinute = 0,
    endHour = 20,
    endMinute = 0,
    alarms = listOf(m13DayAlarm)
)
private val m13NightAlarmConfig = ShiftTemplateAlarmConfig(
    shiftCode = "Н",
    enabled = true,
    startHour = 20,
    startMinute = 0,
    endHour = 8,
    endMinute = 0,
    alarms = listOf(m13NightAlarm)
)

private val m13Upcoming = listOf(
    ShiftAlarmUpcomingInfo(
        triggerAtMillis = Instant.parse("2030-01-15T05:45:00Z").toEpochMilli(),
        title = "На дневную",
        text = "Дневная • 2030-01-15 • начало 08:00",
        shiftCode = "Д",
        alarmKey = "2030-01-15|Д|m13-day-0545"
    ),
    ShiftAlarmUpcomingInfo(
        triggerAtMillis = Instant.parse("2030-01-16T18:40:00Z").toEpochMilli(),
        title = "На ночную",
        text = "Ночная • 2030-01-16 • начало 20:00",
        shiftCode = "Н",
        alarmKey = "2030-01-16|Н|m13-night-1840"
    )
)

private val m13TemplateActions = TemplatesScreenActions(
    onModeChange = {}, onBack = {}, onSwitchWorkplace = {}, onOpenManageWorkplaces = {},
    onAddShift = {}, onAddSystemStatus = {}, onEditShift = {}, onDuplicateShift = {}, onDeleteShift = {},
    onReorderShifts = {}, onAddPattern = {}, onEditPattern = {}, onApplyPattern = {}, onDeletePattern = {}
)

private val m13AlarmActions = ShiftAlarmsTabActions(
    onSave = {}, onRequestNotificationPermission = {}, onOpenExactAlarmSettings = {},
    onOpenFullScreenIntentSettings = {}, onOpenSystemClock = {}, onRescheduleNow = {},
    onCancelUpcomingAlarm = {}, onCancelUpcomingAlarms = {}
)

@Composable
private fun M13ThemeSurface(dark: Boolean, content: @Composable () -> Unit) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        val roles = evolutionColorRoles()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = roles.appBackground,
            contentColor = roles.contentPrimary
        ) { content() }
    }
}

@Composable
private fun M13TemplatesReview(dark: Boolean, activeWorkplaceId: String = WORKPLACE_MAIN_ID) = M13ThemeSurface(dark) {
    TemplatesScreen(
        state = TemplatesScreenState(
            mode = TemplateMode.SHIFTS,
            templates = m13Templates,
            systemStatusCodes = emptySet(),
            specialRules = emptyMap(),
            patterns = listOf(
                PatternTemplate(
                    id = "m13-2x2",
                    name = "2/2",
                    steps = listOf("Д", "Д", "Н", "Н") + List(31) { "" }
                )
            ),
            workplaces = m13Workplaces,
            activeWorkplaceId = activeWorkplaceId
        ),
        actions = m13TemplateActions
    )
}

@Composable
private fun M13ShiftEditorReview() = M13ThemeSurface(false) {
    ShiftTemplateEditorScreen(
        currentTemplate = m13Day,
        workplaces = m13Workplaces,
        defaultWorkplaceId = WORKPLACE_MAIN_ID,
        currentAlarmTemplateConfig = m13DayAlarmConfig,
        onBack = {}, onSave = { _, _, _ -> }, onDelete = {}
    )
}

@Composable
private fun M13AlarmsReview(dark: Boolean, permissionsGranted: Boolean) = M13ThemeSurface(dark) {
    ShiftAlarmsTab(
        state = ShiftAlarmsTabState(
            settings = ShiftAlarmSettings(
                enabled = true,
                autoReschedule = true,
                scheduleHorizonDays = 90,
                wearMirrorEnabled = true,
                templateConfigs = listOf(m13DayAlarmConfig, m13NightAlarmConfig)
            ),
            shiftTemplates = listOf(m13Day, m13Night),
            lastRescheduleResult = null,
            upcomingAlarms = m13Upcoming,
            canScheduleExactAlarms = permissionsGranted,
            notificationPermissionGranted = permissionsGranted,
            fullScreenIntentPermissionGranted = permissionsGranted
        ),
        actions = m13AlarmActions,
        modifier = Modifier.fillMaxSize()
    )
}

@PreviewTest
@Preview(name = "M13 Templates light", widthDp = 412, heightDp = 1100, showBackground = true)
@Composable
fun m13TemplatesLight() = M13TemplatesReview(false)

@PreviewTest
@Preview(name = "M13 Templates dark", widthDp = 412, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m13TemplatesDark() = M13TemplatesReview(true)

@PreviewTest
@Preview(name = "M13 Templates large font", widthDp = 412, heightDp = 1300, fontScale = 1.3f, showBackground = true)
@Composable
fun m13TemplatesLargeFont() = M13TemplatesReview(false)

@PreviewTest
@Preview(name = "M13 Templates multi workplace", widthDp = 412, heightDp = 1100, showBackground = true)
@Composable
fun m13TemplatesMultiWorkplace() = M13TemplatesReview(false, WORKPLACE_SECOND_ID)

@PreviewTest
@Preview(name = "M13 Shift editor alarm context", widthDp = 412, heightDp = 1600, showBackground = true)
@Composable
fun m13ShiftEditorAlarmContext() = M13ShiftEditorReview()

@PreviewTest
@Preview(name = "M13 Alarms upcoming light", widthDp = 412, heightDp = 1300, showBackground = true)
@Composable
fun m13AlarmsUpcomingLight() = M13AlarmsReview(false, true)

@PreviewTest
@Preview(name = "M13 Alarms dark", widthDp = 412, heightDp = 1300, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m13AlarmsDark() = M13AlarmsReview(true, true)

@PreviewTest
@Preview(name = "M13 Alarms large font", widthDp = 412, heightDp = 1500, fontScale = 1.3f, showBackground = true)
@Composable
fun m13AlarmsLargeFont() = M13AlarmsReview(false, true)

@PreviewTest
@Preview(name = "M13 Alarms permission warning", widthDp = 412, heightDp = 1550, showBackground = true)
@Composable
fun m13AlarmsPermissionWarning() = M13AlarmsReview(false, false)
