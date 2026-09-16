package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.integrations.calendar.ExternalCalendarOccurrence
import com.vigilante.shiftsalaryplanner.integrations.calendar.externalCalendarStableId
import com.vigilante.shiftsalaryplanner.integrations.calendar.mapExternalCalendarOccurrence
import com.vigilante.shiftsalaryplanner.integrations.calendar.mapExternalCalendarOccurrences
import com.vigilante.shiftsalaryplanner.ui.today.TimelineAction
import com.vigilante.shiftsalaryplanner.ui.today.TimelineKind
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSeverity
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused JVM tests for the M15.1 Task 4 read-only external calendar source contract.
 *
 * Covers: distinct recurrence instances retained as distinct deterministic stable IDs,
 * all-day occurrences, calendar label/color propagation into TimelineItem, and the
 * pure mapping into the Today timeline (source EXTERNAL_CALENDAR / kind EXTERNAL_EVENT).
 * The mapping must be pure Kotlin with no Android APIs.
 */
class M15ExternalCalendarMappingTest {

    private fun occurrence(
        calendarId: Long = 1L,
        eventId: Long = 2L,
        instanceId: Long = 3L,
        startMillis: Long = 100_000L,
        endMillis: Long = 110_000L,
        allDay: Boolean = false,
        title: String = "Team sync",
        calendarLabel: String = "Work",
        calendarColorArgb: Int = 0xFF1976D2.toInt(),
    ): ExternalCalendarOccurrence = ExternalCalendarOccurrence(
        calendarId = calendarId,
        eventId = eventId,
        instanceId = instanceId,
        startMillis = startMillis,
        endMillis = endMillis,
        allDay = allDay,
        title = title,
        calendarLabel = calendarLabel,
        calendarColorArgb = calendarColorArgb,
    )

    @Test
    fun retainsDistinctRecurrenceInstancesWithDeterministicDistinctStableIds() {
        val recurrenceA = occurrence(eventId = 101L, instanceId = 1001L, startMillis = 90_000L)
        val recurrenceB = occurrence(eventId = 101L, instanceId = 1002L, startMillis = 180_000L)
        val recurrenceC = occurrence(eventId = 101L, instanceId = 1003L, startMillis = 270_000L)

        val result = mapExternalCalendarOccurrences(listOf(recurrenceA, recurrenceB, recurrenceC))

        assertEquals(3, result.size)
        assertEquals(3, result.map { it.stableId }.distinct().size)
        assertEquals(
            listOf(90_000L, 180_000L, 270_000L),
            result.map { it.startMillis },
        )
        assertEquals(
            listOf(
                externalCalendarStableId(recurrenceA),
                externalCalendarStableId(recurrenceB),
                externalCalendarStableId(recurrenceC),
            ),
            result.map { it.stableId },
        )
    }

    @Test
    fun allDayOccurrenceKeepsAllDayFlagAndMapsStartEndTitle() {
        val allDay = occurrence(
            eventId = 202L,
            instanceId = 2L,
            startMillis = 1_752_000_000_000L,
            endMillis = 1_752_086_400_000L,
            allDay = true,
            title = "Public holiday",
            calendarLabel = "Holidays",
        )
        val timed = occurrence(
            eventId = 203L,
            instanceId = 3L,
            allDay = false,
            title = "Dentist",
        )

        assertTrue(allDay.allDay)
        assertFalse(timed.allDay)

        val items = mapExternalCalendarOccurrences(listOf(allDay, timed))

        val allDayItem = items.first { it.stableId == externalCalendarStableId(allDay) }
        assertEquals(1_752_000_000_000L, allDayItem.startMillis)
        assertEquals(1_752_086_400_000L, allDayItem.endMillis)
        assertEquals("Public holiday", allDayItem.title)
        assertEquals("Holidays", allDayItem.sourceLabel)

        val timedItem = items.first { it.stableId == externalCalendarStableId(timed) }
        assertEquals(100_000L, timedItem.startMillis)
        assertEquals(110_000L, timedItem.endMillis)
        assertEquals("Dentist", timedItem.title)
    }

    @Test
    fun propagatesCalendarLabelAndColorIntoTimelineItem() {
        val occurrence = occurrence(
            calendarId = 7L,
            eventId = 8L,
            instanceId = 9L,
            calendarLabel = "Personal",
            calendarColorArgb = 0xFFE91E63.toInt(),
        )

        val item = mapExternalCalendarOccurrence(occurrence)

        assertEquals("Personal", item.sourceLabel)
        assertEquals(0xFFE91E63.toInt(), item.colorArgb)
    }

    @Test
    fun stableIdIsDeterministicAndIncludesCalendarEventInstanceIdentity() {
        val first = occurrence(calendarId = 10L, eventId = 20L, instanceId = 30L)
        val second = occurrence(calendarId = 10L, eventId = 20L, instanceId = 30L)
        val differentCalendar = occurrence(calendarId = 99L, eventId = 20L, instanceId = 30L)
        val differentEvent = occurrence(calendarId = 10L, eventId = 99L, instanceId = 30L)
        val differentInstance = occurrence(calendarId = 10L, eventId = 20L, instanceId = 99L)

        assertEquals(externalCalendarStableId(first), externalCalendarStableId(second))
        assertEquals(externalCalendarStableId(first), mapExternalCalendarOccurrence(first).stableId)
        assertEquals(externalCalendarStableId(first), mapExternalCalendarOccurrence(first).stableId)

        val ids = setOf(
            externalCalendarStableId(differentCalendar),
            externalCalendarStableId(differentEvent),
            externalCalendarStableId(differentInstance),
        )
        assertFalse(externalCalendarStableId(first) in ids)
        assertEquals(3, ids.size)
    }

    @Test
    fun mappedItemUsesExternalCalendarSourceKindAndOpenEventAction() {
        val occurrence = occurrence(
            calendarId = 5L,
            eventId = 6L,
            instanceId = 7L,
            title = "Interview",
        )

        val item = mapExternalCalendarOccurrence(occurrence)

        assertEquals(TimelineSource.EXTERNAL_CALENDAR, item.source)
        assertEquals(TimelineKind.EXTERNAL_EVENT, item.kind)
        assertEquals(TimelineSeverity.NORMAL, item.severity)
        assertEquals("Interview", item.title)
        assertEquals(
            TimelineAction.OpenExternalEvent(occurrenceId = externalCalendarStableId(occurrence)),
            item.action,
        )
    }

    @Test
    fun emptyOccurrenceListMapsToEmptyTimelineItems() {
        assertTrue(mapExternalCalendarOccurrences(emptyList()).isEmpty())
    }
}
