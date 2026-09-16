package com.vigilante.shiftsalaryplanner.integrations.calendar

import com.vigilante.shiftsalaryplanner.ui.today.TimelineAction
import com.vigilante.shiftsalaryplanner.ui.today.TimelineItem
import com.vigilante.shiftsalaryplanner.ui.today.TimelineKind
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSeverity
import com.vigilante.shiftsalaryplanner.ui.today.TimelineSource

/**
 * Pure Kotlin read-only external calendar model and mapping for the M15 Today timeline.
 *
 * Defined by docs/superpowers/plans/2026-09-16-m15-1-today-foundation.md Task 4 and the
 * M15 design (external calendar layer is **read-only**; no write APIs and no app-owned
 * `PlannedEvent` persistence). This file intentionally uses no Android APIs so the model
 * and mapping stay provider/store-independent and JVM-testable.
 */

/**
 * One occurrence of an external calendar event as seen for a concrete time window.
 *
 * [calendarId], [eventId] and [instanceId] together retain the provider's stable
 * calendar/event/instance identity so recurring events never collapse into one row:
 * the Calendar Provider `Instances` table yields one row per expanded occurrence.
 */
data class ExternalCalendarOccurrence(
    val calendarId: Long,
    val eventId: Long,
    val instanceId: Long,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val title: String,
    val calendarLabel: String,
    val calendarColorArgb: Int,
)

/** Read-only source of external calendar occurrences for a time window. */
interface ExternalCalendarSource {
    /**
     * Returns occurrences intersecting [startMillis, endMillis) restricted to the
     * [enabledCalendarIds] calendars. An empty [enabledCalendarIds] yields no
     * occurrences. Implementations must never write to the provider.
     */
    fun occurrences(
        startMillis: Long,
        endMillis: Long,
        enabledCalendarIds: Collection<Long>,
    ): List<ExternalCalendarOccurrence>
}

/**
 * Deterministic stable identity of an occurrence for the Today timeline.
 *
 * Combines calendar/event/instance identity so distinct recurrence instances and events
 * from different calendars always produce distinct, repeatable IDs.
 */
fun externalCalendarStableId(occurrence: ExternalCalendarOccurrence): String =
    "external-calendar:${occurrence.calendarId}:${occurrence.eventId}:${occurrence.instanceId}"

/** Maps one external occurrence into the normalized Today timeline. */
fun mapExternalCalendarOccurrence(occurrence: ExternalCalendarOccurrence): TimelineItem {
    val stableId = externalCalendarStableId(occurrence)
    return TimelineItem(
        stableId = stableId,
        source = TimelineSource.EXTERNAL_CALENDAR,
        kind = TimelineKind.EXTERNAL_EVENT,
        startMillis = occurrence.startMillis,
        endMillis = occurrence.endMillis,
        title = occurrence.title,
        severity = TimelineSeverity.NORMAL,
        sourceLabel = occurrence.calendarLabel,
        colorArgb = occurrence.calendarColorArgb,
        action = TimelineAction.OpenExternalEvent(occurrenceId = stableId),
    )
}

/** Maps external occurrences into the normalized Today timeline, preserving input order. */
fun mapExternalCalendarOccurrences(
    occurrences: List<ExternalCalendarOccurrence>,
): List<TimelineItem> = occurrences.map(::mapExternalCalendarOccurrence)
