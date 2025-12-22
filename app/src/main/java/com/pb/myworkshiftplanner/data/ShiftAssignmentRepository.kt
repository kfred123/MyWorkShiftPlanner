package com.pb.myworkshiftplanner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftAssignmentRepository(private val shiftAssignmentDao: ShiftAssignmentDao) {

    fun getAssignmentsInRange(startDate: String, endDate: String): Flow<Map<String, Shift>> {
        return shiftAssignmentDao.getAssignmentsWithShiftsInRange(startDate, endDate)
            .map { assignmentsWithShifts ->
                assignmentsWithShifts.mapNotNull { item ->
                    item.shift?.let { item.assignment.date to it }
                }.toMap()
            }
    }

    suspend fun getAssignmentByDate(date: String): ShiftAssignment? {
        return shiftAssignmentDao.getAssignmentByDate(date)
    }

    suspend fun insert(assignment: ShiftAssignment): Long {
        return shiftAssignmentDao.insertAssignment(assignment)
    }

    suspend fun update(assignment: ShiftAssignment) {
        shiftAssignmentDao.updateAssignment(assignment)
    }

    suspend fun delete(assignment: ShiftAssignment) {
        shiftAssignmentDao.deleteAssignment(assignment)
    }

    suspend fun deleteByDate(date: String) {
        shiftAssignmentDao.deleteAssignmentByDate(date)
    }
}

