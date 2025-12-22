package com.pb.myworkshiftplanner.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.pb.myworkshiftplanner.data.CalendarInfo

object CalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAvailableCalendars(context: Context): List<CalendarInfo> {
        if (!hasCalendarPermission(context)) {
            return emptyList()
        }

        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val colorIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val accountName = it.getString(accountIndex)
                val color = it.getInt(colorIndex)

                calendars.add(CalendarInfo(id, name, accountName, color))
            }
        }

        return calendars
    }
}

