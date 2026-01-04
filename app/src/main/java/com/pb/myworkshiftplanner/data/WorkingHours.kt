package com.pb.myworkshiftplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "working_hours")
data class WorkingHours(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val yearMonth: String, // Format: "YYYY-MM"
    val weeklyHours: Double
)

