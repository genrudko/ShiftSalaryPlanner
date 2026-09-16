package com.vigilante.shiftsalaryplanner

import com.vigilante.shiftsalaryplanner.ui.today.TimelineAction
import com.vigilante.shiftsalaryplanner.ui.today.TimelineItem
import com.vigilante.shiftsalaryplanner.ui.today.TimelineKind
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSeverity
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSource
import com.vigilante.shiftsalaryplanner.ui.today.buildTodayTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused JVM tests for the M15.1 Today timeline pure model and ordering.
 *
 * Covers the Task 1 contract: chronological ordering, deterministic tie-breaking,
 * retention of distinct recurring external occurrences, and source/action payload
 * preservation. The timeline implementation must be pure Kotlin with no Android APIs.
 */
class M15TodayTimelineTest {

    private val nowMillis = 1_752_000_000_000L

    private fun item(
        stableId: String,
        startMillis: Long,
        source: TimelineSource = TimelineSource.NOTE,
        kind: TimelineKind = TimelineKind.NOTE_REMINDER,
        severity: TimelineSeverity = TimelineSeverity.NORMAL,
        title: String = "Title $stableId",
        action: TimelineAction? = null,
    ): TimelineItem = TimelineItem(
        stableId = stableId,
        startMillis = startMillis,
        source = source,
        kind = kind,
        severity = severity,
        title = title,
        action = action,
    )

    @Test
    fun ordersChronologicallyByStartMillis() {
        val later = item(
            stableId = "later-input-first",
            startMillis = 30_000,
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
        )
        val middle = item(
            stableId = "middle-input-second",
            startMillis = 20_000,
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
        )
        val earliest = item(
            stableId = "earliest-input-third",
            startMillis = 10_000,
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
        )

        val result = buildTodayTimeline(listOf(later, middle, earliest), nowMillis)

        assertEquals(
            listOf("earliest-input-third", "middle-input-second", "later-input-first"),
            result.map { it.stableId },
        )
    }

    @Test
    fun startTimeTiesAreBrokenBySourcePriorityThenStableIdRegardlessOfInputOrder() {
        val shift = item(
            stableId = "shift-9",
            startMillis = 60_000,
            source = TimelineSource.SHIFT,
            kind = TimelineKind.SHIFT_START,
        )
        val alarm = item(
            stableId = "alarm-z",
            startMillis = 60_000,
            source = TimelineSource.ALARM,
            kind = TimelineKind.ALARM,
        )
        val note = item(
            stableId = "note-m",
            startMillis = 60_000,
            source = TimelineSource.NOTE,
            kind = TimelineKind.NOTE_REMINDER,
        )

        val expected = listOf("shift-9", "alarm-z", "note-m")

        assertEquals(expected, buildTodayTimeline(listOf(note, alarm, shift), nowMillis).map { it.stableId })
        assertEquals(expected, buildTodayTimeline(listOf(shift, note, alarm), nowMillis).map { it.stableId })
        assertEquals(expected, buildTodayTimeline(listOf(alarm, shift, note), nowMillis).map { it.stableId })
    }

    @Test
    fun sameSourceStartTimeTiesAreBrokenByStableIdLexicographically() {
        val itemB = item(
            stableId = "b-item",
            startMillis = 90_000,
            source = TimelineSource.FINANCE,
            kind = TimelineKind.PAYMENT,
        )
        val itemA = item(
            stableId = "a-item",
            startMillis = 90_000,
            source = TimelineSource.FINANCE,
            kind = TimelineKind.PAYMENT,
        )

        val result = buildTodayTimeline(listOf(itemB, itemA), nowMillis)

        assertEquals(listOf("a-item", "b-item"), result.map { it.stableId })
    }

    @Test
    fun retainsDistinctRecurringExternalOccurrences() {
        val occurrenceA = item(
            stableId = "cal-1-ev-101-inst-a",
            startMillis = 101_000,
            source = TimelineSource.EXTERNAL_CALENDAR,
            kind = TimelineKind.EXTERNAL_EVENT,
            title = "Team sync",
        )
        val occurrenceB = item(
            stableId = "cal-1-ev-101-inst-b",
            startMillis = 90_000,
            source = TimelineSource.EXTERNAL_CALENDAR,
            kind = TimelineKind.EXTERNAL_EVENT,
            title = "Team sync",
        )
        val occurrenceC = item(
            stableId = "cal-1-ev-101-inst-c",
            startMillis = 101_000,
            source = TimelineSource.EXTERNAL_CALENDAR,
            kind = TimelineKind.EXTERNAL_EVENT,
            title = "Team sync",
        )

        val result = buildTodayTimeline(listOf(occurrenceA, occurrenceB, occurrenceC), nowMillis)

        assertEquals(
            listOf("cal-1-ev-101-inst-b", "cal-1-ev-101-inst-a", "cal-1-ev-101-inst-c"),
            result.map { it.stableId },
        )
        assertEquals(3, result.size)
        assertEquals(3, result.map { it.stableId }.distinct().size)
        assertTrue(result.all { it.title == "Team sync" && it.source == TimelineSource.EXTERNAL_CALENDAR })
    }

    @Test
    fun preservesSourceActionAndAllPayloadFields() {
        val noteAction = TimelineAction.OpenNote(noteId = "note-42")
        val input = TimelineItem(
            stableId = "note-42",
            source = TimelineSource.NOTE,
            kind = TimelineKind.NOTE_REMINDER,
            startMillis = 120_000,
            endMillis = 130_000,
            title = "Pay rent",
            subtitle = "bank transfer",
            severity = TimelineSeverity.HIGH,
            sourceLabel = "Notes",
            colorArgb = 0xFF3F51B5.toInt(),
            action = noteAction,
        )

        val result = buildTodayTimeline(listOf(input), nowMillis)

        assertEquals(1, result.size)
        assertEquals(input, result.single())
        assertEquals(TimelineSource.NOTE, result.single().source)
        assertEquals(noteAction, result.single().action)
    }

    @Test
    fun preservesNullActionOnItemsWithoutAction() {
        val input = item(
            stableId = "no-action",
            startMillis = 140_000,
            source = TimelineSource.WEATHER,
            kind = TimelineKind.WEATHER_EVENT,
        )

        val result = buildTodayTimeline(listOf(input), nowMillis)

        assertNull(result.single().action)
        assertEquals(TimelineSource.WEATHER, result.single().source)
    }

    @Test
    fun emptyInputReturnsEmptyTimeline() {
        assertEquals(emptyList<TimelineItem>(), buildTodayTimeline(emptyList(), nowMillis))
    }
}
