package com.pb.myworkshiftplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActualWorkTimeDao {
    @Query("SELECT * FROM actual_work_times WHERE date = :date")
    suspend fun getActualWorkTimeByDate(date: String): ActualWorkTime?

    @Query("SELECT * FROM actual_work_times WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getActualWorkTimesInRange(startDate: String, endDate: String): Flow<List<ActualWorkTime>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActualWorkTime(actualWorkTime: ActualWorkTime): Long

    @Update
    suspend fun updateActualWorkTime(actualWorkTime: ActualWorkTime)

    @Delete
    suspend fun deleteActualWorkTime(actualWorkTime: ActualWorkTime)

    @Query("DELETE FROM actual_work_times WHERE date = :date")
    suspend fun deleteActualWorkTimeByDate(date: String)
}

