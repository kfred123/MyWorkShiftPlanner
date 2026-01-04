package com.pb.myworkshiftplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkingHoursDao {

    @Query("SELECT * FROM working_hours ORDER BY yearMonth ASC")
    fun getAllWorkingHours(): Flow<List<WorkingHours>>

    @Query("SELECT * FROM working_hours WHERE yearMonth = :yearMonth")
    suspend fun getWorkingHoursByMonth(yearMonth: String): WorkingHours?

    @Query("SELECT * FROM working_hours WHERE yearMonth < :yearMonth ORDER BY yearMonth DESC LIMIT 1")
    suspend fun getPreviousWorkingHours(yearMonth: String): WorkingHours?

    @Query("SELECT * FROM working_hours ORDER BY yearMonth ASC LIMIT 1")
    suspend fun getEarliestWorkingHours(): WorkingHours?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workingHours: WorkingHours): Long

    @Update
    suspend fun update(workingHours: WorkingHours)

    @Query("DELETE FROM working_hours WHERE yearMonth = :yearMonth")
    suspend fun deleteByYearMonth(yearMonth: String)

    @Query("DELETE FROM working_hours WHERE yearMonth >= :fromYearMonth")
    suspend fun deleteFromMonthOnwards(fromYearMonth: String)
}

