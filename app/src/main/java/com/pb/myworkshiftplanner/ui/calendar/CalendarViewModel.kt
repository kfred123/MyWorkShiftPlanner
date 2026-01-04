package com.pb.myworkshiftplanner.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.*
import com.pb.myworkshiftplanner.utils.CalendarSyncHelper
import com.pb.myworkshiftplanner.utils.TimeCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

data class MonthlySummary(
    val previousMonthBalance: Int = 0, // in minutes, positive = overtime, negative = deficit
    val targetHours: Int = 0, // target work minutes for current month
    val plannedHours: Int = 0, // planned work minutes for current month
    val actualHours: Int = 0, // actual worked minutes for current month
    val difference: Int = 0 // planned - target (positive = more than target, negative = less than target)
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val assignments: Map<String, Shift> = emptyMap(),
    val actualWorkTimes: Map<String, ActualWorkTime> = emptyMap(),
    val allShifts: List<Shift> = emptyList(),
    val showDayDialog: Boolean = false,
    val monthlySummary: MonthlySummary = MonthlySummary()
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ShiftDatabase.getDatabase(application)
    private val shiftRepository = ShiftRepository(database.shiftDao())
    private val assignmentRepository = ShiftAssignmentRepository(database.shiftAssignmentDao())
    private val actualWorkTimeRepository = ActualWorkTimeRepository(database.actualWorkTimeDao())
    private val workingHoursRepository = WorkingHoursRepository(database.workingHoursDao())
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
                    loadMonthlySummary()
                }
        }

        viewModelScope.launch {
            // Load actual work times
            actualWorkTimeRepository.getActualWorkTimesInRange(startDate, endDate)
                .collectLatest { actualTimes ->
                    val actualTimesMap = actualTimes.associateBy { it.date }
                    _uiState.update { it.copy(actualWorkTimes = actualTimesMap) }
                    loadMonthlySummary()
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

    private fun loadMonthlySummary() {
        viewModelScope.launch {
            val currentMonth = _uiState.value.currentMonth
            val summary = calculateMonthlySummary(currentMonth)
            _uiState.update { it.copy(monthlySummary = summary) }
        }
    }

    private suspend fun calculateMonthlySummary(month: YearMonth): MonthlySummary {
        // 1. Calculate previous month balance (overtime/deficit)
        val previousMonth = month.minusMonths(1)
        val previousMonthBalance = calculateMonthBalance(previousMonth)

        // 2. Calculate target hours for current month
        val targetHours = calculateTargetHours(month)

        // 3. Calculate planned hours for current month
        val plannedHours = calculatePlannedHours(month)

        // 4. Calculate actual worked hours for current month
        val actualHours = calculateActualWorkedHours(month)

        // 5. Calculate difference (planned - target)
        val difference = plannedHours - targetHours

        return MonthlySummary(
            previousMonthBalance = previousMonthBalance,
            targetHours = targetHours,
            plannedHours = plannedHours,
            actualHours = actualHours,
            difference = difference
        )
    }

    private suspend fun calculateMonthBalance(month: YearMonth): Int {
        // Check if working hours were ever configured for this month or earlier
        val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val earliestWorkingHours = workingHoursRepository.getEarliestWorkingHours()

        // If no working hours were ever configured, return 0
        if (earliestWorkingHours == null) {
            return 0
        }

        // If the month is before the earliest configured working hours, return 0
        if (yearMonthString < earliestWorkingHours.yearMonth) {
            return 0
        }

        // Get all assignments and actual work times for the month
        val startDate = month.atDay(1).format(dateFormatter)
        val endDate = month.atEndOfMonth().format(dateFormatter)

        val assignmentsMap = mutableMapOf<String, Shift>()
        assignmentRepository.getAssignmentsInRange(startDate, endDate).first().forEach { (date, shift) ->
            assignmentsMap[date] = shift
        }

        val actualTimesMap = mutableMapOf<String, ActualWorkTime>()
        actualWorkTimeRepository.getActualWorkTimesInRange(startDate, endDate).first().forEach { actualTime ->
            actualTimesMap[actualTime.date] = actualTime
        }

        // Calculate target hours for the month
        val targetMinutes = calculateTargetHours(month)

        // Calculate actual worked hours
        var actualMinutes = 0
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        var date = firstDay

        while (!date.isAfter(lastDay)) {
            val dateString = date.format(dateFormatter)
            val actualTime = actualTimesMap[dateString]

            if (actualTime != null) {
                // Use actual work time
                actualMinutes += TimeCalculator.calculateWorkMinutes(
                    actualTime.actualStartTime,
                    actualTime.actualEndTime,
                    actualTime.actualBreakDuration
                )
            } else {
                // Use planned shift if date is in the past
                val shift = assignmentsMap[dateString]
                if (shift != null && !date.isAfter(LocalDate.now())) {
                    actualMinutes += TimeCalculator.calculateWorkMinutes(
                        shift.beginTime,
                        shift.endTime,
                        shift.breakDuration
                    )
                }
            }
            date = date.plusDays(1)
        }

        // Return balance (actual - target)
        return actualMinutes - targetMinutes
    }

    private suspend fun calculateTargetHours(month: YearMonth): Int {
        // Get weekly working hours for the month
        val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        var workingHours = workingHoursRepository.getWorkingHoursByMonth(yearMonthString)

        // If not found, get from previous month
        if (workingHours == null) {
            workingHours = workingHoursRepository.getPreviousWorkingHours(yearMonthString)
        }

        val weeklyHours = workingHours?.weeklyHours ?: 40.0

        // Count working days (Monday-Friday) in the month
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        var workingDays = 0
        var date = firstDay

        while (!date.isAfter(lastDay)) {
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++
            }
            date = date.plusDays(1)
        }

        // Calculate target: (weeklyHours / 5) * workingDays
        val dailyHours = weeklyHours / 5.0
        val targetHours = dailyHours * workingDays
        return (targetHours * 60).toInt() // Convert to minutes
    }

    private suspend fun calculatePlannedHours(month: YearMonth): Int {
        // Get all assignments for the month
        val startDate = month.atDay(1).format(dateFormatter)
        val endDate = month.atEndOfMonth().format(dateFormatter)

        val assignmentsMap = assignmentRepository.getAssignmentsInRange(startDate, endDate).first()

        var totalMinutes = 0
        assignmentsMap.values.forEach { shift ->
            totalMinutes += TimeCalculator.calculateWorkMinutes(
                shift.beginTime,
                shift.endTime,
                shift.breakDuration
            )
        }

        return totalMinutes
    }

    private suspend fun calculateActualWorkedHours(month: YearMonth): Int {
        // Get all assignments and actual work times for the month
        val startDate = month.atDay(1).format(dateFormatter)
        val endDate = month.atEndOfMonth().format(dateFormatter)

        val assignmentsMap = mutableMapOf<String, Shift>()
        assignmentRepository.getAssignmentsInRange(startDate, endDate).first().forEach { (date, shift) ->
            assignmentsMap[date] = shift
        }

        val actualTimesMap = mutableMapOf<String, ActualWorkTime>()
        actualWorkTimeRepository.getActualWorkTimesInRange(startDate, endDate).first().forEach { actualTime ->
            actualTimesMap[actualTime.date] = actualTime
        }

        // Calculate actual worked hours
        var actualMinutes = 0
        val firstDay = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        var date = firstDay

        while (!date.isAfter(lastDay)) {
            val dateString = date.format(dateFormatter)
            val actualTime = actualTimesMap[dateString]

            if (actualTime != null) {
                // Use actual work time
                actualMinutes += TimeCalculator.calculateWorkMinutes(
                    actualTime.actualStartTime,
                    actualTime.actualEndTime,
                    actualTime.actualBreakDuration
                )
            } else {
                // Use planned shift if date is in the past
                val shift = assignmentsMap[dateString]
                if (shift != null && !date.isAfter(LocalDate.now())) {
                    actualMinutes += TimeCalculator.calculateWorkMinutes(
                        shift.beginTime,
                        shift.endTime,
                        shift.breakDuration
                    )
                }
            }
            date = date.plusDays(1)
        }

        return actualMinutes
    }
}

