package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.patterns.PatternTemplate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarPatternPersistenceBoundaryTest {
    @Test
    fun monthPatternUsesInjectedPersistenceOperations() = runBlocking {
        val calls = mutableListOf<String>()
        val pattern = PatternTemplate(id = "p", name = "P", steps = listOf("D", "", "N"))

        applyPatternToMonth(
            pattern = pattern,
            cycleStartDate = LocalDate.of(2026, 9, 1),
            month = YearMonth.of(2026, 9),
            validShiftCodes = setOf("D"),
            upsertShiftDay = { item -> calls += "upsert:${item.date}:${item.shiftCode}" },
            deleteShiftDay = { date -> calls += "delete:$date" }
        )

        assertEquals("upsert:2026-09-01:D", calls[0])
        assertEquals("delete:2026-09-02", calls[1])
    }

    @Test
    fun rangePatternUsesInjectedPersistenceOperations() = runBlocking {
        val calls = mutableListOf<String>()
        val pattern = PatternTemplate(id = "p", name = "P", steps = listOf("D"))

        applyPatternToRange(
            pattern = pattern,
            rangeStart = LocalDate.of(2026, 9, 12),
            rangeEnd = LocalDate.of(2026, 9, 12),
            validShiftCodes = setOf("D"),
            upsertShiftDay = { item -> calls += "upsert:${item.date}:${item.shiftCode}" },
            deleteShiftDay = { date -> calls += "delete:$date" }
        )

        assertEquals(listOf("upsert:2026-09-12:D"), calls)
    }
}
