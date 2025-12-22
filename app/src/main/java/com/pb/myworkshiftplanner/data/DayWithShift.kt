package com.pb.myworkshiftplanner.data

data class DayWithShift(
    val date: String, // Format: "yyyy-MM-dd"
    val shift: Shift?
)

