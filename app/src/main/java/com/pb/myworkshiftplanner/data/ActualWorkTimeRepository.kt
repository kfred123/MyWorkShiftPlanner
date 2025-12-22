package com.pb.myworkshiftplanner.data

import kotlinx.coroutines.flow.Flow

class ActualWorkTimeRepository(private val actualWorkTimeDao: ActualWorkTimeDao) {

    suspend fun getActualWorkTimeByDate(date: String): ActualWorkTime? {
        return actualWorkTimeDao.getActualWorkTimeByDate(date)
    }

    fun getActualWorkTimesInRange(startDate: String, endDate: String): Flow<List<ActualWorkTime>> {
        return actualWorkTimeDao.getActualWorkTimesInRange(startDate, endDate)
    }

    suspend fun insert(actualWorkTime: ActualWorkTime): Long {
        return actualWorkTimeDao.insertActualWorkTime(actualWorkTime)
    }

    suspend fun update(actualWorkTime: ActualWorkTime) {
        actualWorkTimeDao.updateActualWorkTime(actualWorkTime)
    }

    suspend fun delete(actualWorkTime: ActualWorkTime) {
        actualWorkTimeDao.deleteActualWorkTime(actualWorkTime)
    }

    suspend fun deleteByDate(date: String) {
        actualWorkTimeDao.deleteActualWorkTimeByDate(date)
    }
}

