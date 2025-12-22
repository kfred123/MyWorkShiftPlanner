package com.pb.myworkshiftplanner.data

import kotlinx.coroutines.flow.Flow

class ShiftRepository(private val shiftDao: ShiftDao) {

    val allShifts: Flow<List<Shift>> = shiftDao.getAllShifts()

    suspend fun getShiftById(id: Long): Shift? {
        return shiftDao.getShiftById(id)
    }

    suspend fun insert(shift: Shift): Long {
        return shiftDao.insertShift(shift)
    }

    suspend fun update(shift: Shift) {
        shiftDao.updateShift(shift)
    }

    suspend fun delete(shift: Shift) {
        shiftDao.deleteShift(shift)
    }

    suspend fun deleteById(id: Long) {
        shiftDao.deleteShiftById(id)
    }
}

