package com.pb.myworkshiftplanner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shift_assignments",
    foreignKeys = [
        ForeignKey(
            entity = Shift::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shiftId"]), Index(value = ["date"], unique = true)]
)
data class ShiftAssignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val shiftId: Long
)

