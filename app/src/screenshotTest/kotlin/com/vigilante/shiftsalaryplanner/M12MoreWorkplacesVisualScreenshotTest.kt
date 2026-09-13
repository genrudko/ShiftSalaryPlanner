package com.vigilante.shiftsalaryplanner

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vigilante.shiftsalaryplanner.settings.Workplace
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_THIRD_ID
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles

private val m12Workplaces = listOf(
    Workplace(WORKPLACE_MAIN_ID, "Северный парк"),
    Workplace(WORKPLACE_SECOND_ID, "Подстанция"),
    Workplace(WORKPLACE_THIRD_ID, "Резерв")
)

@Composable
private fun M12ThemeSurface(dark: Boolean, content: @Composable () -> Unit) {
    ShiftSalaryPlannerTheme(AppearanceSettings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, visualStyleMode = AppVisualStyleMode.EXPRESSIVE)) {
        val roles = evolutionColorRoles()
        Surface(modifier = Modifier.fillMaxSize(), color = roles.appBackground, contentColor = roles.contentPrimary) { content() }
    }
}

@Composable
private fun M12MoreReview(dark: Boolean) = M12ThemeSurface(dark) {
    MoreTab(
        currentProfileLabel = "Основной", appearanceSummary = "Светлая · Evolution",
        onOpenWorkplaces = {}, onOpenShiftTemplates = {}, onOpenManualHolidays = {}, onSyncProductionCalendar = {},
        isHolidaySyncing = false, holidaySyncMessage = "Календарь 2026 актуален",
        onOpenAlarms = {}, onOpenNotes = {}, onOpenAssistant = {}, onOpenAppearance = {}, onOpenWidgets = {}, onOpenQuickActions = {},
        onOpenWear = {}, onOpenProfiles = {}, onOpenQuickStart = {}, onOpenBackupRestore = {}, onOpenGoogleDrive = {}, onOpenImport = {},
        onOpenReportCenter = {}, onOpenHealthCheck = {}, onOpenEventLog = {}, onOpenCurrentParameters = {}, modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun M12WorkplacesReview() = M12ThemeSurface(false) {
    WorkplacesScreen(
        workplaces = m12Workplaces, activeWorkplaceId = WORKPLACE_SECOND_ID, onBack = {}, onSelectWorkplace = {},
        onRenameWorkplaces = {}, onOpenPayrollSettings = {}, modifier = Modifier.fillMaxSize()
    )
}

@PreviewTest
@Preview(name = "More light", widthDp = 412, heightDp = 1000, showBackground = true)
@Composable
fun m12MoreLight() = M12MoreReview(false)

@PreviewTest
@Preview(name = "More dark", widthDp = 412, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun m12MoreDark() = M12MoreReview(true)

@PreviewTest
@Preview(name = "More large font", widthDp = 412, heightDp = 1200, fontScale = 1.3f, showBackground = true)
@Composable
fun m12MoreLargeFont() = M12MoreReview(false)

@PreviewTest
@Preview(name = "Workplaces", widthDp = 412, heightDp = 900, showBackground = true)
@Composable
fun m12Workplaces() = M12WorkplacesReview()
