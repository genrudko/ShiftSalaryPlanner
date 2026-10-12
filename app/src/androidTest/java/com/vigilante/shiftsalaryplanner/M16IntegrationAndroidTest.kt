package com.vigilante.shiftsalaryplanner

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import com.vigilante.shiftsalaryplanner.excel.ExcelImportPreview
import com.vigilante.shiftsalaryplanner.excel.ExcelScheduleImporter
import com.vigilante.shiftsalaryplanner.excel.ParsedExcelShiftDay
import com.vigilante.shiftsalaryplanner.widget.NotesListWidgetProvider
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthClassicWidgetProviderV2
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthMiniWidgetProviderV2
import com.vigilante.shiftsalaryplanner.widget.ShiftMonthWidgetProviderV2
import com.vigilante.shiftsalaryplanner.widget.TodayNotesWidgetProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room/Android platform contracts. Uses a dedicated test-only database. */
@RunWith(AndroidJUnit4::class)
class M16IntegrationAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun preview(vararg days: String, month: Int = 10): ExcelImportPreview =
        ExcelImportPreview(
            fullName = "M16 test",
            year = 2026,
            selectedMonths = listOf(month),
            parsedMonths = emptyList(),
            templatesToCreate = emptyList(),
            importedDays = days.map { date ->
                ParsedExcelShiftDay(
                    date = LocalDate.parse(date),
                    sourceCode = "Д", sourceHours = 12.0,
                    targetShiftCode = "Д", targetTemplateTitle = "Day",
                    isAutoCreatedTemplate = false,
                )
            },
        )

    @Test fun importReplacesSelectedMonthInsideRealRoomTransaction() = runBlocking {
        val database = AppDatabase.getDatabase(context, "m16_instrumentation_qualified")
        database.shiftDayDao().clearAll()
        database.shiftDayDao().upsert(ShiftDayEntity("2026-10-02", "N"))
        database.shiftDayDao().upsert(ShiftDayEntity("2026-09-22", "N"))

        ExcelScheduleImporter(database).import(preview("2026-10-03"))
        val days = database.shiftDayDao().observeAll().first().associateBy { it.date }
        assertEquals("Д", days["2026-10-03"]?.shiftCode)
        assertTrue("Old month day must be replaced", "2026-10-02" !in days)
        assertEquals("N", days["2026-09-22"]?.shiftCode)
    }

    @Test fun invalidPreviewLeavesPreviousPeriodUntouched() = runBlocking {
        val database = AppDatabase.getDatabase(context, "m16_instrumentation_qualified")
        database.shiftDayDao().clearAll()
        database.shiftDayDao().upsert(ShiftDayEntity("2026-10-02", "N"))

        val failed = runCatching { ExcelScheduleImporter(database).import(preview()) }
        assertTrue(failed.isFailure)
        assertEquals("N", database.shiftDayDao().observeAll().first().single().shiftCode)
    }

    @Test fun allFiveWidgetUpdateReceiversHandlePackageLocalBroadcast() {
        for (provider in listOf(
            ShiftMonthWidgetProviderV2(),
            ShiftMonthClassicWidgetProviderV2(),
            ShiftMonthMiniWidgetProviderV2(),
            NotesListWidgetProvider(),
            TodayNotesWidgetProvider(),
        )) {
            provider.onReceive(
                context,
                Intent(context, provider.javaClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                },
            )
        }
    }
}
