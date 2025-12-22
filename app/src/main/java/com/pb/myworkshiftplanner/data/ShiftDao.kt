package com.pb.myworkshiftplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY name ASC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getShiftById(id: Long): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    @Delete
    suspend fun deleteShift(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Long)
}

