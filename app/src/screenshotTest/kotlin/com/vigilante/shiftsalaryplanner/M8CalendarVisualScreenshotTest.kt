package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_THIRD_ID
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
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
private fun M8CalendarReviewSurface(dark: Boolean, rangePreview: Boolean = false) {
    ShiftSalaryPlannerTheme(
        AppearanceSettings(
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
            visualStyleMode = AppVisualStyleMode.EXPRESSIVE
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Text("Сентябрь 2026", style = MaterialTheme.typography.headlineSmall)
            Text("Северный парк · все рабочие места", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CalendarGrid(
                currentMonth = reviewMonth,
                selectedDate = reviewToday,
                today = reviewToday,
                shiftCodesByDate = reviewShiftCodes,
                dayAssignmentsByDate = reviewAssignments,
                noteDates = setOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 22)),
                shiftOverrideDates = setOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 22)),
                holidayMap = reviewHolidays,
                templateMap = reviewTemplates,
                shiftColors = reviewColors,
                activeBrushCode = null,
                previewRangeStartDate = if (rangePreview) LocalDate.of(2026, 9, 21) else null,
                previewRangeEndDate = if (rangePreview) LocalDate.of(2026, 9, 25) else null,
                onEraseDate = {}, onDayClick = {}, onDayLongPress = {}, compactMode = false
            )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Calendar light", widthDp = 412, heightDp = 620, showBackground = true)
@Composable
fun m8CalendarLight() = M8CalendarReviewSurface(false)

@PreviewTest
@Preview(name = "Calendar dark", widthDp = 412, heightDp = 620, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m8CalendarDark() = M8CalendarReviewSurface(true)

@PreviewTest
@Preview(name = "Calendar large font", widthDp = 412, heightDp = 690, fontScale = 1.3f, showBackground = true)
@Composable
fun m8CalendarLargeFont() = M8CalendarReviewSurface(false)

@PreviewTest
@Preview(name = "Calendar range preview", widthDp = 412, heightDp = 620, showBackground = true)
@Composable
fun m8CalendarRangePreview() = M8CalendarReviewSurface(false, true)
