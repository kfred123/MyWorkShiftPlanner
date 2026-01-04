package com.pb.myworkshiftplanner.ui.workinghours

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.ShiftDatabase
import com.pb.myworkshiftplanner.data.WorkingHours
import com.pb.myworkshiftplanner.data.WorkingHoursRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter

data class MonthWorkingHours(
    val yearMonth: YearMonth,
    val hours: Double?,
    val isManual: Boolean
)

data class WorkingHoursUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val monthsList: List<MonthWorkingHours> = emptyList(),
    val earliestMonth: YearMonth? = null,
    val isLoading: Boolean = false
)

class WorkingHoursViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ShiftDatabase.getDatabase(application)
    private val workingHoursRepository = WorkingHoursRepository(database.workingHoursDao())

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val _uiState = MutableStateFlow(WorkingHoursUiState())
    val uiState: StateFlow<WorkingHoursUiState> = _uiState.asStateFlow()

    init {
        loadWorkingHours()
    }

    private fun loadWorkingHours() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            workingHoursRepository.allWorkingHours.collectLatest { workingHoursList ->
                val earliest = workingHoursRepository.getEarliestWorkingHours()
                val earliestMonth = earliest?.let { YearMonth.parse(it.yearMonth, dateFormatter) }

                val monthsList = buildMonthsList(workingHoursList, earliestMonth)

                _uiState.update {
                    it.copy(
                        monthsList = monthsList,
                        earliestMonth = earliestMonth,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun buildMonthsList(
        workingHoursList: List<WorkingHours>,
        earliestMonth: YearMonth?
    ): List<MonthWorkingHours> {
        val currentMonth = YearMonth.now()
        val workingHoursMap = workingHoursList.associateBy { it.yearMonth }

        // Determine the start month: earliest manual entry or current month
        val startMonth = earliestMonth ?: currentMonth

        // End month: 12 months into the future
        val endMonth = currentMonth.plusMonths(12)

        val months = mutableListOf<MonthWorkingHours>()
        var month = startMonth

        while (month <= endMonth) {
            val monthString = month.format(dateFormatter)
            val manualEntry = workingHoursMap[monthString]

            if (manualEntry != null) {
                // Has manual entry
                months.add(MonthWorkingHours(month, manualEntry.weeklyHours, true))
            } else {
                // Get value from previous month
                val previousValue = workingHoursRepository.getPreviousWorkingHours(monthString)
                months.add(MonthWorkingHours(month, previousValue?.weeklyHours, false))
            }

            month = month.plusMonths(1)
        }

        return months
    }

    fun saveWorkingHours(yearMonth: YearMonth, hours: Double) {
        viewModelScope.launch {
            val monthString = yearMonth.format(dateFormatter)
            val existing = workingHoursRepository.getWorkingHoursByMonth(monthString)

            if (existing != null) {
                workingHoursRepository.update(existing.copy(weeklyHours = hours))
            } else {
                workingHoursRepository.insert(WorkingHours(yearMonth = monthString, weeklyHours = hours))
            }

            // If the month is current or future, apply to all future months
            val currentMonth = YearMonth.now()
            if (yearMonth >= currentMonth) {
                applyToFutureMonths(yearMonth, hours)
            }
        }
    }

    private suspend fun applyToFutureMonths(fromMonth: YearMonth, hours: Double) {
        // Delete all manual entries from the next month onwards
        val nextMonth = fromMonth.plusMonths(1)
        val nextMonthString = nextMonth.format(dateFormatter)

        workingHoursRepository.deleteFromMonthOnwards(nextMonthString)
    }

    fun deleteWorkingHours(yearMonth: YearMonth) {
        viewModelScope.launch {
            val monthString = yearMonth.format(dateFormatter)
            workingHoursRepository.deleteByYearMonth(monthString)
        }
    }

    fun scrollToMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
    }
}

