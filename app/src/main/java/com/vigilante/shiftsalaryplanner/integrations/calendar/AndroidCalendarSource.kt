package com.vigilante.shiftsalaryplanner.integrations.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.CalendarContract

/**
 * Reads the IDs of visible, non-deleted calendars on the device.
 *
 * Used only to seed the persisted set of enabled calendar IDs when the user first
 * enables the external-calendar layer and grants `READ_CALENDAR`. Read-only: this
 * function never writes to the provider.
 */
fun readVisibleCalendarIds(contentResolver: ContentResolver): List<Long> {
    val projection = arrayOf(CalendarContract.Calendars._ID)
    // Canonical Calendar Provider columns (`calendar_visible`/`deleted`) are not exposed
    // as public constants on this Android SDK's Calendars stub, so they are referenced by
    // their stable provider column names. Read-only: this query never writes.
    val selection = "calendar_visible != 0 AND deleted != 1"
    val ids = mutableListOf<Long>()
    contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        selection,
        null,
        null,
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        while (cursor.moveToNext()) {
            ids += cursor.getLong(idIndex)
        }
    }
    return ids
}

/**
 * Read-only Android Calendar Provider adapter for the M15 Today external calendar layer.
 *
 * Queries only the `CalendarContract.Instances` window table, which yields one row per
 * expanded occurrence (including each recurrence instance). This class never calls any
 * Calendar Provider write API and does not own or persist events; recurrence, exceptions,
 * account synchronization and conflict handling remain owned by the external provider.
 */
class AndroidCalendarSource(
    private val contentResolver: ContentResolver,
) : ExternalCalendarSource {

    override fun occurrences(
        startMillis: Long,
        endMillis: Long,
        enabledCalendarIds: Collection<Long>,
    ): List<ExternalCalendarOccurrence> {
        val calendarIds = enabledCalendarIds.toList().distinct()
        if (calendarIds.isEmpty()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances._ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_COLOR,
        )
        val selectionArgs = calendarIds.map { it.toString() }.toTypedArray()
        val calendarIdPlaceholders = calendarIds.joinToString(",") { "?" }
        val selection =
            "${CalendarContract.Instances.CALENDAR_ID} IN ($calendarIdPlaceholders)"

        val instanceUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .also { builder ->
                ContentUris.appendId(builder, startMillis)
                ContentUris.appendId(builder, endMillis)
            }
            .build()

        val result = mutableListOf<ExternalCalendarOccurrence>()
        contentResolver.query(
            instanceUri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val calendarIdIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val eventIdIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val instanceIdIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances._ID)
            val beginIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val titleIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val displayNameIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val colorIndex =
                cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex)
                result += ExternalCalendarOccurrence(
                    calendarId = cursor.getLong(calendarIdIndex),
                    eventId = cursor.getLong(eventIdIndex),
                    instanceId = cursor.getLong(instanceIdIndex),
                    startMillis = cursor.getLong(beginIndex),
                    endMillis = cursor.getLong(endIndex),
                    allDay = cursor.getInt(allDayIndex) != 0,
                    title = title.orEmpty(),
                    calendarLabel = cursor.getString(displayNameIndex).orEmpty(),
                    calendarColorArgb = cursor.getInt(colorIndex),
                )
            }
        }
        return result
    }
}
