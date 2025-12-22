package com.pb.myworkshiftplanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val beginTime: String, // Format: "HH:mm"
    val endTime: String,   // Format: "HH:mm"
    val breakDuration: Int // in minutes
)

