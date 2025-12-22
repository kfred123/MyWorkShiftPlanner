package com.pb.myworkshiftplanner.data

import androidx.room.Embedded
import androidx.room.Relation

data class ShiftAssignmentWithShift(
    @Embedded val assignment: ShiftAssignment,
    @Relation(
        parentColumn = "shiftId",
        entityColumn = "id"
    )
    val shift: Shift?
)

