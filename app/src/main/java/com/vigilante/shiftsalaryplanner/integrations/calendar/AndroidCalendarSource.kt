package com.vigilante.shiftsalaryplanner.integrations.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.CalendarContract

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
