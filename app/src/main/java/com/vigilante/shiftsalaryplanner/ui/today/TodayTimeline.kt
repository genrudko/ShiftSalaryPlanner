package com.vigilante.shiftsalaryplanner.ui.today

/**
 * Pure Kotlin timeline model for the M15 Today screen.
 *
 * Defined by docs/superpowers/specs/2026-09-16-m15-today-weather-design.md section 3.
 * This file intentionally uses no Android APIs: the normalized model and ordering are
 * provider/store-independent so the UI can consume one ordered list of stable values.
 */

/**
 * Identifies the independent source that produced a timeline item.
 *
 * [priority] is used for deterministic tie-breaking: when two items share the same
 * [TimelineItem.startMillis], the one with the lower [priority] sorts earlier.
 */
enum class TimelineSource(val priority: Int) {
    SHIFT(0),
    ALARM(1),
    NOTE(2),
    FINANCE(3),
    EXTERNAL_CALENDAR(4),
    WEATHER(5),
}

/** Granular timeline event kind within a [TimelineSource]. */
enum class TimelineKind {
    SHIFT_START,
    SHIFT_END,
    ALARM,
    NOTE_REMINDER,
    PAYMENT,
    EXTERNAL_EVENT,
    WEATHER_EVENT,
}

/** Visual/operational importance of a timeline item. */
enum class TimelineSeverity {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
}

/**
 * Deliberately narrow set of actions the Today UI can invoke for a timeline item.
 *
 * Per the M15 design, creation of external events is a separate Android intent action
 * and never creates an app-owned event; these values only carry typed navigation
 * payloads supplied by the integration layer.
 */
sealed interface TimelineAction {
    /** Open the Calendar at the relevant day. */
    data object OpenCalendar : TimelineAction

    /** Open the Alarms section. */
    data object OpenAlarms : TimelineAction

    /** Open a specific note by its stable identity. */
    data class OpenNote(val noteId: String) : TimelineAction

    /** Open the Finance section. */
    data object OpenFinance : TimelineAction

    /** Open an external calendar event by its provider occurrence identity. */
    data class OpenExternalEvent(val occurrenceId: String) : TimelineAction
}

/**
 * One normalized row of the Today timeline.
 *
 * [stableId] is the stable identity consumed by later tasks and must remain unique
 * across distinct occurrences (for example recurring external-calendar instances).
 */
data class TimelineItem(
    val stableId: String,
    val source: TimelineSource,
    val kind: TimelineKind,
    val startMillis: Long,
    val endMillis: Long? = null,
    val title: String,
    val subtitle: String? = null,
    val severity: TimelineSeverity,
    val sourceLabel: String? = null,
    val colorArgb: Int? = null,
    val action: TimelineAction? = null,
)

/**
 * Normalizes [items] into the ordered Today timeline.
 *
 * Ordering rules (M15 design section 3):
 * 1. chronological by [TimelineItem.startMillis];
 * 2. deterministic tie-break by [TimelineSource.priority], then by [TimelineItem.stableId];
 * 3. all items are retained; expired-item emphasis/filtering is a later slice, so
 *    [nowMillis] is reserved for that contract and does not filter in this version.
 *
 * Uses a stable sort so items that are fully equal in start/source/stableId keep their
 * input order; equal stableIds are not expected for distinct occurrences.
 */
fun buildTodayTimeline(items: List<TimelineItem>, nowMillis: Long): List<TimelineItem> =
    items.sortedWith(
        compareBy<TimelineItem> { it.startMillis }
            .thenBy { it.source.priority }
            .thenBy { it.stableId }
    )
