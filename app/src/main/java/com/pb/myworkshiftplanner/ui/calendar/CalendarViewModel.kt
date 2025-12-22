package com.pb.myworkshiftplanner.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val assignments: Map<String, Shift> = emptyMap(),
    val actualWorkTimes: Map<String, ActualWorkTime> = emptyMap(),
    val allShifts: List<Shift> = emptyList(),
    val showDayDialog: Boolean = false
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ShiftDatabase.getDatabase(application)
    private val shiftRepository = ShiftRepository(database.shiftDao())
    private val assignmentRepository = ShiftAssignmentRepository(database.shiftAssignmentDao())
    private val actualWorkTimeRepository = ActualWorkTimeRepository(database.actualWorkTimeDao())

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth()
        loadAllShifts()
    }

    private fun loadAllShifts() {
        viewModelScope.launch {
            shiftRepository.allShifts.collectLatest { shifts ->
                _uiState.update { it.copy(allShifts = shifts) }
            }
        }
    }

    private fun loadMonth() {
        val currentMonth = _uiState.value.currentMonth
        val startDate = currentMonth.atDay(1).format(dateFormatter)
        val endDate = currentMonth.atEndOfMonth().format(dateFormatter)

        viewModelScope.launch {
            // Load shift assignments
            assignmentRepository.getAssignmentsInRange(startDate, endDate)
                .collectLatest { assignments ->
                    _uiState.update { it.copy(assignments = assignments) }
                }
        }

        viewModelScope.launch {
            // Load actual work times
            actualWorkTimeRepository.getActualWorkTimesInRange(startDate, endDate)
                .collectLatest { actualTimes ->
                    val actualTimesMap = actualTimes.associateBy { it.date }
                    _uiState.update { it.copy(actualWorkTimes = actualTimesMap) }
                }
        }
    }

    fun nextMonth() {
        _uiState.update {
            it.copy(currentMonth = it.currentMonth.plusMonths(1))
        }
        loadMonth()
    }

    fun previousMonth() {
        _uiState.update {
            it.copy(currentMonth = it.currentMonth.minusMonths(1))
        }
        loadMonth()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update {
            it.copy(selectedDate = date, showDayDialog = true)
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(showDayDialog = false, selectedDate = null)
        }
    }

    fun assignShift(shiftId: Long?) {
        val selectedDate = _uiState.value.selectedDate ?: return
        val dateString = selectedDate.format(dateFormatter)

        viewModelScope.launch {
            if (shiftId == null) {
                // Remove assignment
                assignmentRepository.deleteByDate(dateString)
            } else {
                // Create or update assignment
                val existing = assignmentRepository.getAssignmentByDate(dateString)
                if (existing != null) {
                    assignmentRepository.update(existing.copy(shiftId = shiftId))
                } else {
                    assignmentRepository.insert(ShiftAssignment(date = dateString, shiftId = shiftId))
                }
            }
        }
    }

    fun saveActualWorkTime(
        startTime: String,
        endTime: String,
        breakDuration: Int
    ) {
        val selectedDate = _uiState.value.selectedDate ?: return
        val dateString = selectedDate.format(dateFormatter)

        viewModelScope.launch {
            val existing = actualWorkTimeRepository.getActualWorkTimeByDate(dateString)
            val actualWorkTime = ActualWorkTime(
                id = existing?.id ?: 0,
                date = dateString,
                actualStartTime = startTime,
                actualEndTime = endTime,
                actualBreakDuration = breakDuration
            )

            if (existing != null) {
                actualWorkTimeRepository.update(actualWorkTime)
            } else {
                actualWorkTimeRepository.insert(actualWorkTime)
            }
        }
    }

    fun deleteActualWorkTime() {
        val selectedDate = _uiState.value.selectedDate ?: return
        val dateString = selectedDate.format(dateFormatter)

        viewModelScope.launch {
            actualWorkTimeRepository.deleteByDate(dateString)
        }
    }

    fun getShiftForDate(date: LocalDate): Shift? {
        val dateString = date.format(dateFormatter)
        return _uiState.value.assignments[dateString]
    }

    fun getActualWorkTimeForDate(date: LocalDate): ActualWorkTime? {
        val dateString = date.format(dateFormatter)
        return _uiState.value.actualWorkTimes[dateString]
    }
}

