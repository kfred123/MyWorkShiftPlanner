package com.pb.myworkshiftplanner.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.*
import com.pb.myworkshiftplanner.utils.CalendarSyncHelper
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
    private val settingsRepository = SettingsRepository(application)

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
            val existing = assignmentRepository.getAssignmentByDate(dateString)

            if (shiftId == null) {
                // Remove assignment
                if (existing != null) {
                    // Delete from Google Calendar if event exists
                    existing.googleCalendarEventId?.let { eventId ->
                        CalendarSyncHelper.deleteCalendarEvent(
                            getApplication(),
                            eventId
                        )
                    }
                    assignmentRepository.deleteByDate(dateString)
                }
            } else {
                // Get the shift details
                val shift = _uiState.value.allShifts.find { it.id == shiftId }
                if (shift == null) return@launch

                // Get selected calendar ID
                val calendarId = settingsRepository.selectedCalendarId.first()

                if (existing != null) {
                    // Update existing assignment
                    val updatedAssignment = if (calendarId != null && CalendarSyncHelper.hasWriteCalendarPermission(getApplication())) {
                        // Sync with Google Calendar
                        if (existing.googleCalendarEventId != null) {
                            // Update existing calendar event
                            val success = CalendarSyncHelper.updateCalendarEvent(
                                getApplication(),
                                calendarId,
                                existing.googleCalendarEventId,
                                existing.id,
                                dateString,
                                shift
                            )
                            if (success) {
                                existing.copy(shiftId = shiftId)
                            } else {
                                existing.copy(shiftId = shiftId, googleCalendarEventId = null)
                            }
                        } else {
                            // Create new calendar event
                            val eventId = CalendarSyncHelper.createCalendarEvent(
                                getApplication(),
                                calendarId,
                                existing.id,
                                dateString,
                                shift
                            )
                            existing.copy(shiftId = shiftId, googleCalendarEventId = eventId)
                        }
                    } else {
                        // No calendar sync
                        existing.copy(shiftId = shiftId)
                    }
                    assignmentRepository.update(updatedAssignment)
                } else {
                    // Create new assignment
                    val newAssignment = ShiftAssignment(
                        date = dateString,
                        shiftId = shiftId,
                        googleCalendarEventId = null
                    )
                    val assignmentId = assignmentRepository.insert(newAssignment)

                    // Sync with Google Calendar
                    if (calendarId != null && CalendarSyncHelper.hasWriteCalendarPermission(getApplication())) {
                        val eventId = CalendarSyncHelper.createCalendarEvent(
                            getApplication(),
                            calendarId,
                            assignmentId,
                            dateString,
                            shift
                        )
                        // Update assignment with event ID
                        if (eventId != null) {
                            assignmentRepository.update(
                                newAssignment.copy(id = assignmentId, googleCalendarEventId = eventId)
                            )
                        }
                    }
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

