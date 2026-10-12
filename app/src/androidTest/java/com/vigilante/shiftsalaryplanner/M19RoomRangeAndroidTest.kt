package com.vigilante.shiftsalaryplanner

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vigilante.shiftsalaryplanner.data.AppDatabase
import com.vigilante.shiftsalaryplanner.data.ShiftDayEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M19RoomRangeAndroidTest {
    @Test fun widgetRangeIncludesBothEdgesAndExcludesAllOtherHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            listOf(
                "2025-12-31", "2026-01-31", "2026-02-01",
                "2026-02-14", "2026-02-28", "2026-03-01", "2027-01-01"
            ).reversed().forEach { date ->
                database.shiftDayDao().upsert(ShiftDayEntity(date, "Д"))
            }
            assertEquals(
                listOf("2026-02-01", "2026-02-14", "2026-02-28"),
                database.shiftDayDao().listBetween("2026-02-01", "2026-02-28").map { it.date }
            )
            assertEquals(emptyList<ShiftDayEntity>(), database.shiftDayDao().listBetween("2026-04-01", "2026-04-30"))
        } finally {
            database.close()
        }
    }
}
