package com.pb.myworkshiftplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actual_work_times")
data class ActualWorkTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val actualStartTime: String, // Format: "HH:mm"
    val actualEndTime: String,   // Format: "HH:mm"
    val actualBreakDuration: Int // in minutes
)

