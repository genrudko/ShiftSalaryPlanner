package com.vigilante.shiftsalaryplanner.integrations.calendar

import android.content.Intent
import android.provider.CalendarContract

/**
 * System `+ Event` delegation for the M15 Today screen.
 *
 * Defined by docs/superpowers/plans/2026-09-16-m15-1-today-foundation.md Task 5 and the
 * M15 design section 4: event creation is owned by the system calendar. This helper
 * builds an `Intent.ACTION_INSERT` targeting `CalendarContract.Events.CONTENT_URI`,
 * seeded with the current date/time so the system insert UI opens at the right moment.
 * The app never creates or persists its own calendar events and this path requires no
 * `WRITE_CALENDAR` permission.
 */
fun buildSystemEventInsertIntent(
    nowMillis: Long = System.currentTimeMillis(),
): Intent =
    Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nowMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, nowMillis + DEFAULT_EVENT_DURATION_MILLIS)

private const val DEFAULT_EVENT_DURATION_MILLIS = 60L * 60L * 1000L
