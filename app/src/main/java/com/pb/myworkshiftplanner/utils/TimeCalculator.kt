package com.pb.myworkshiftplanner.utils

import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoUnit

object TimeCalculator {

    /**
     * Calculate work duration in minutes from start and end time, minus break duration
     */
    fun calculateWorkMinutes(startTime: String, endTime: String, breakMinutes: Int): Int {
        val start = LocalTime.parse(startTime)
        val end = LocalTime.parse(endTime)

        var minutes = ChronoUnit.MINUTES.between(start, end).toInt()

        // Handle overnight shifts
        if (minutes < 0) {
            minutes += 24 * 60
        }

        return (minutes - breakMinutes).coerceAtLeast(0)
    }

    /**
     * Calculate overtime: actual work time minus planned work time
     * Returns positive for overtime, negative for undertime
     */
    fun calculateOvertime(
        plannedStart: String,
        plannedEnd: String,
        plannedBreak: Int,
        actualStart: String,
        actualEnd: String,
        actualBreak: Int
    ): Int {
        val plannedMinutes = calculateWorkMinutes(plannedStart, plannedEnd, plannedBreak)
        val actualMinutes = calculateWorkMinutes(actualStart, actualEnd, actualBreak)
        return actualMinutes - plannedMinutes
    }

    /**
     * Format minutes to hours and minutes string (e.g., "2:30 h" or "-0:15 h")
     */
    fun formatMinutesToHoursString(minutes: Int): String {
        val absMinutes = kotlin.math.abs(minutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val sign = if (minutes < 0) "-" else "+"
        return "$sign$hours:${mins.toString().padStart(2, '0')} h"
    }

    /**
     * Format minutes to decimal hours (e.g., "2.5" or "-0.25")
     */
    fun formatMinutesToDecimal(minutes: Int): String {
        val hours = minutes / 60.0
        return String.format("%.2f h", hours)
    }
}

