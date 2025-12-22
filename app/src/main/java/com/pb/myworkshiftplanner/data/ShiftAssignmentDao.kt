package com.pb.myworkshiftplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftAssignmentDao {
    @Query("SELECT * FROM shift_assignments WHERE date = :date")
    suspend fun getAssignmentByDate(date: String): ShiftAssignment?

    @Transaction
    @Query("SELECT * FROM shift_assignments WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getAssignmentsWithShiftsInRange(startDate: String, endDate: String): Flow<List<ShiftAssignmentWithShift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: ShiftAssignment): Long

    @Update
    suspend fun updateAssignment(assignment: ShiftAssignment)

    @Delete
    suspend fun deleteAssignment(assignment: ShiftAssignment)

    @Query("DELETE FROM shift_assignments WHERE date = :date")
    suspend fun deleteAssignmentByDate(date: String)
}

