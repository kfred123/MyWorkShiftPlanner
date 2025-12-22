package com.pb.myworkshiftplanner.utils

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.pb.myworkshiftplanner.data.Shift
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

/**
 * Helper class for synchronizing shift assignments with Google Calendar
 */
object CalendarSyncHelper {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Check if the app has write calendar permission
     */
    fun hasWriteCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Create a calendar event for a shift assignment
     * @return The event ID if successful, null otherwise
     */
    fun createCalendarEvent(
        context: Context,
        calendarId: String,
        assignmentId: Long,
        date: String,
        shift: Shift
    ): String? {
        if (!hasWriteCalendarPermission(context)) {
            return null
        }

        try {
            val localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val startTime = LocalTime.parse(shift.beginTime, timeFormatter)
            val endTime = LocalTime.parse(shift.endTime, timeFormatter)

            val startDateTime = localDate.atTime(startTime)
            val endDateTime = if (endTime.isBefore(startTime)) {
                // Shift goes past midnight
                localDate.plusDays(1).atTime(endTime)
            } else {
                localDate.atTime(endTime)
            }

            val zoneId = ZoneId.systemDefault()
            val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, shift.name)
                put(CalendarContract.Events.DESCRIPTION, buildEventDescription(shift, assignmentId))
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Update an existing calendar event
     * @return true if successful, false otherwise
     */
    fun updateCalendarEvent(
        context: Context,
        calendarId: String,
        eventId: String,
        assignmentId: Long,
        date: String,
        shift: Shift
    ): Boolean {
        if (!hasWriteCalendarPermission(context)) {
            return false
        }

        try {
            val localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val startTime = LocalTime.parse(shift.beginTime, timeFormatter)
            val endTime = LocalTime.parse(shift.endTime, timeFormatter)

            val startDateTime = localDate.atTime(startTime)
            val endDateTime = if (endTime.isBefore(startTime)) {
                // Shift goes past midnight
                localDate.plusDays(1).atTime(endTime)
            } else {
                localDate.atTime(endTime)
            }

            val zoneId = ZoneId.systemDefault()
            val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
            val endMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, shift.name)
                put(CalendarContract.Events.DESCRIPTION, buildEventDescription(shift, assignmentId))
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
            }

            val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())
            val rowsUpdated = context.contentResolver.update(eventUri, values, null, null)
            return rowsUpdated > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Delete a calendar event
     * @return true if successful, false otherwise
     */
    fun deleteCalendarEvent(
        context: Context,
        eventId: String
    ): Boolean {
        if (!hasWriteCalendarPermission(context)) {
            return false
        }

        try {
            val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId.toLong())
            val rowsDeleted = context.contentResolver.delete(eventUri, null, null)
            return rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Build event description with metadata for identification
     */
    private fun buildEventDescription(shift: Shift, assignmentId: Long): String {
        return buildString {
            append("Schicht: ${shift.name}\n")
            append("Zeit: ${shift.beginTime} - ${shift.endTime}\n")
            append("Pause: ${shift.breakDuration} Minuten\n")
            append("\n")
            append("// MyWorkShiftPlanner Assignment ID: $assignmentId")
        }
    }
}

