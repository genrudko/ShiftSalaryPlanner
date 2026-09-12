package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.data.HolidayEntity
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import com.vigilante.shiftsalaryplanner.settings.AppNote
import com.vigilante.shiftsalaryplanner.settings.AppProfile
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_THIRD_ID
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
import java.time.LocalDate
import java.time.YearMonth

private val reviewMonth = YearMonth.of(2026, 9)
private val reviewToday = LocalDate.of(2026, 9, 12)

private fun reviewTemplate(code: String, title: String, iconKey: String, colorHex: String, order: Int) =
    ShiftTemplateEntity(code, title, iconKey, 12.0, 1.0, if (code == "Н") 8.0 else 0.0, colorHex, true, true, order)

private val reviewTemplates = listOf(
    reviewTemplate("Д", "Дневная", "TEXT", "#A9D6FF", 0),
    reviewTemplate("Н", "Ночная", "MOON", "#C8B6FF", 1),
    reviewTemplate("8", "8 часов", "EIGHT", "#B8E0C2", 2),
    reviewTemplate("В", "Выходной", "HOME", "#E2E3E5", 3),
    reviewTemplate("Э", "Энергетика", "EMOJI:⚡", "#FFD6A5", 4),
    reviewTemplate("Ц", "Цех", "MSR:Factory", "#FFB4A2", 5)
).associateBy { it.code }

private val reviewColors = mapOf(
    "Д" to 0xFFA9D6FF.toInt(), "Н" to 0xFFC8B6FF.toInt(), "8" to 0xFFB8E0C2.toInt(),
    "В" to 0xFFE2E3E5.toInt(), "Э" to 0xFFFFD6A5.toInt(), "Ц" to 0xFFFFB4A2.toInt(),
    KEY_EMPTY_DAY to 0xFFF5F7FA.toInt()
)

private val reviewProfiles = listOf(AppProfile("review-profile", "Основной"))
private val reviewWorkplaces = listOf(
    Workplace(WORKPLACE_MAIN_ID, "Северный парк"),
    Workplace(WORKPLACE_SECOND_ID, "Подстанция"),
    Workplace(WORKPLACE_THIRD_ID, "Резерв")
)

private val reviewShiftCodes = buildMap {
    val pattern = listOf("Д", "Д", "Н", "Н", "В", "В")
    for (day in 1..30) put(LocalDate.of(2026, 9, day), pattern[(day - 1) % pattern.size])
    put(LocalDate.of(2026, 9, 9), "8")
    put(LocalDate.of(2026, 9, 15), "Э")
    put(LocalDate.of(2026, 9, 22), "Н")
}

private val reviewAssignments = mapOf(
    LocalDate.of(2026, 9, 15) to listOf(
        CalendarDayAssignment(WORKPLACE_MAIN_ID, "Э"),
        CalendarDayAssignment(WORKPLACE_SECOND_ID, "8")
    ),
    LocalDate.of(2026, 9, 22) to listOf(
        CalendarDayAssignment(WORKPLACE_MAIN_ID, "Н"),
        CalendarDayAssignment(WORKPLACE_SECOND_ID, "Ц"),
        CalendarDayAssignment(WORKPLACE_THIRD_ID, "8")
    )
)

private val reviewHolidays = mapOf(
    LocalDate.of(2026, 9, 24) to HolidayEntity("review-2026-09-24", "2026-09-24", "Особый день")
)

@Composable
private fun M8CalendarReviewSurface(
    dark: Boolean,
    rangePreview: Boolean = false,
    selectedDate: LocalDate = reviewToday,
    activeBrushCode: String? = null,
    patternMode: Boolean = false
) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        val roles = evolutionColorRoles()
        val selectedAssignments = reviewAssignments[selectedDate].orEmpty().ifEmpty {
            listOf(CalendarDayAssignment(WORKPLACE_MAIN_ID, reviewShiftCodes[selectedDate] ?: "Н"))
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = roles.appBackground,
            contentColor = roles.contentPrimary
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonthHeader(
                    currentMonth = reviewMonth,
                    onPrevMonth = {},
                    onNextMonth = {},
                    onPickMonth = {},
                    useEvolution = true
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarWorkplaceSwitcher(
                        workplaces = reviewWorkplaces,
                        activeWorkplaceId = CALENDAR_WORKPLACE_ALL_ID,
                        onSwitchWorkplace = {},
                        showAllWorkplacesOption = true,
                        allWorkplacesOptionId = CALENDAR_WORKPLACE_ALL_ID,
                        allWorkplacesOptionLabel = "Все работы",
                        modifier = Modifier.weight(1f),
                        useEvolution = true
                    )
                    CalendarProfileSwitcher(
                        profiles = reviewProfiles,
                        activeProfileId = reviewProfiles.first().id,
                        onSwitchProfile = {},
                        onOpenProfiles = {},
                        modifier = Modifier.weight(1f)
                    )
                }
                if (patternMode) {
                    PatternApplyModeCard(
                        pattern = PatternTemplate(
                            id = "review-pattern",
                            name = "День / Ночь / выходные",
                            steps = listOf("Д", "Д", "Н", "Н", "В", "В")
                        ),
                        rangeStartDate = LocalDate.of(2026, 9, 21),
                        previewRangeStartDate = LocalDate.of(2026, 9, 21),
                        previewRangeEndDate = LocalDate.of(2026, 9, 25),
                        onOpenPreview = {},
                        onCancel = {}
                    )
                } else if (activeBrushCode != null) {
                    ActiveBrushCard(
                        activeBrushCode = activeBrushCode,
                        templateMap = reviewTemplates,
                        onDisableBrush = {}
                    )
                }
                CalendarGrid(
                    currentMonth = reviewMonth,
                    selectedDate = selectedDate,
                    today = reviewToday,
                    shiftCodesByDate = reviewShiftCodes,
                    dayAssignmentsByDate = reviewAssignments,
                    noteDates = setOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 22)),
                    shiftOverrideDates = setOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 22)),
                    holidayMap = reviewHolidays,
                    templateMap = reviewTemplates,
                    shiftColors = reviewColors,
                    activeBrushCode = activeBrushCode,
                    previewRangeStartDate = if (rangePreview) LocalDate.of(2026, 9, 21) else null,
                    previewRangeEndDate = if (rangePreview) LocalDate.of(2026, 9, 25) else null,
                    onEraseDate = {}, onDayClick = {}, onDayLongPress = {}, compactMode = false
                )
                SelectedDaySummaryCard(
                    date = selectedDate,
                    today = reviewToday,
                    assignments = selectedAssignments,
                    workplaces = reviewWorkplaces,
                    templateMap = reviewTemplates,
                    shiftColors = reviewColors,
                    hasNote = selectedDate in setOf(reviewToday, LocalDate.of(2026, 9, 22)),
                    hasShiftOverride = selectedDate in setOf(reviewToday, LocalDate.of(2026, 9, 22)),
                    isSpecialDay = selectedDate in reviewHolidays,
                    onEdit = {},
                    onDetails = {}
                )
                QuickShiftBar(
                    shiftTemplates = reviewTemplates.values.sortedBy { it.sortOrder },
                    workplaces = reviewWorkplaces,
                    activeWorkplaceId = WORKPLACE_MAIN_ID,
                    systemStatusCodes = emptySet(),
                    activeBrushCode = activeBrushCode,
                    isRangeClearModeActive = false,
                    onSelectBrush = {},
                    onClearBrush = {},
                    onDisableBrush = {},
                    onAddNewShift = {},
                    onOpenPatternEditor = {},
                    onClearCurrentMonth = {},
                    onStartRangeClearMode = {},
                    onClearAllCalendar = {},
                    onClose = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Calendar light", widthDp = 412, heightDp = 1040, showBackground = true)
@Composable
fun m8CalendarLight() = M8CalendarReviewSurface(false)

@PreviewTest
@Preview(name = "Calendar dark", widthDp = 412, heightDp = 1040, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m8CalendarDark() = M8CalendarReviewSurface(true)

@PreviewTest
@Preview(name = "Calendar large font", widthDp = 412, heightDp = 1180, fontScale = 1.3f, showBackground = true)
@Composable
fun m8CalendarLargeFont() = M8CalendarReviewSurface(false)

@PreviewTest
@Preview(name = "Calendar range preview", widthDp = 412, heightDp = 1040, showBackground = true)
@Composable
fun m8CalendarRangePreview() = M8CalendarReviewSurface(false, true)

@PreviewTest
@Preview(name = "Calendar brush active", widthDp = 412, heightDp = 1120, showBackground = true)
@Composable
fun m10CalendarBrushActive() = M8CalendarReviewSurface(false, activeBrushCode = "Н")

@PreviewTest
@Preview(name = "Calendar multi workplace selected", widthDp = 412, heightDp = 1120, showBackground = true)
@Composable
fun m10CalendarMultiWorkplaceSelected() = M8CalendarReviewSurface(
    dark = false,
    selectedDate = LocalDate.of(2026, 9, 22)
)

@PreviewTest
@Preview(name = "Calendar pattern mode", widthDp = 412, heightDp = 1180, showBackground = true)
@Composable
fun m10CalendarPatternMode() = M8CalendarReviewSurface(
    dark = false,
    rangePreview = true,
    patternMode = true
)

@PreviewTest
@Preview(name = "Calendar day detail", widthDp = 412, heightDp = 840, showBackground = true)
@Composable
fun m10CalendarDayDetail() {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        EvolutionSurface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            role = EvolutionSurfaceRole.FLOATING,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(appCornerRadius(28.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Смены на 22.09.2026",
                    style = MaterialTheme.typography.titleMedium
                )
                DayAssignmentsContent(
                    date = LocalDate.of(2026, 9, 22),
                    assignments = reviewAssignments.getValue(LocalDate.of(2026, 9, 22)),
                    workplaces = reviewWorkplaces,
                    templateMap = reviewTemplates,
                    templateAlarmConfigs = mapOf(
                        "Н" to ShiftTemplateAlarmConfig("Н", startHour = 20, endHour = 8),
                        "Ц" to ShiftTemplateAlarmConfig("Ц", startHour = 8, endHour = 20),
                        "8" to ShiftTemplateAlarmConfig("8", startHour = 8, endHour = 16)
                    ),
                    shiftColors = reviewColors,
                    shiftDayRecordsByCode = mapOf(
                        "Н" to ShiftDayEntity(
                            date = "2026-09-22",
                            shiftCode = "Н",
                            overrideStartTime = "21:00",
                            overrideEndTime = "08:00",
                            overridePaidHours = 10.5,
                            overrideNote = "Подмена на ПС-17"
                        )
                    ),
                    notes = listOf(
                        AppNote(
                            id = "review-note-2026-09-22",
                            date = "2026-09-22",
                            workplaceId = WORKPLACE_MAIN_ID,
                            shiftCode = "Н",
                            title = "Подмена на ПС-17",
                            body = "Проверить допуск перед сменой",
                            createdAtMillis = 1L,
                            updatedAtMillis = 1L
                        )
                    )
                )
            }
        }
    }
}
