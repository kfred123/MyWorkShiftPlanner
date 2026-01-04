package com.pb.myworkshiftplanner.data

import kotlinx.coroutines.flow.Flow

class WorkingHoursRepository(private val workingHoursDao: WorkingHoursDao) {

    val allWorkingHours: Flow<List<WorkingHours>> = workingHoursDao.getAllWorkingHours()

    suspend fun getWorkingHoursByMonth(yearMonth: String): WorkingHours? {
        return workingHoursDao.getWorkingHoursByMonth(yearMonth)
    }

    suspend fun getPreviousWorkingHours(yearMonth: String): WorkingHours? {
        return workingHoursDao.getPreviousWorkingHours(yearMonth)
    }

    suspend fun getEarliestWorkingHours(): WorkingHours? {
        return workingHoursDao.getEarliestWorkingHours()
    }

    suspend fun insert(workingHours: WorkingHours): Long {
        return workingHoursDao.insert(workingHours)
    }

    suspend fun update(workingHours: WorkingHours) {
        workingHoursDao.update(workingHours)
    }

    suspend fun deleteByYearMonth(yearMonth: String) {
        workingHoursDao.deleteByYearMonth(yearMonth)
    }

    suspend fun deleteFromMonthOnwards(fromYearMonth: String) {
        workingHoursDao.deleteFromMonthOnwards(fromYearMonth)
    }
}

