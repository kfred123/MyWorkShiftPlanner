package com.pb.myworkshiftplanner.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.CalendarInfo
import com.pb.myworkshiftplanner.data.SettingsRepository
import com.pb.myworkshiftplanner.utils.CalendarHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val availableCalendars: List<CalendarInfo> = emptyList(),
    val selectedCalendarId: String? = null,
    val selectedCalendarName: String? = null,
    val hasCalendarPermission: Boolean = false,
    val isLoading: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoadCalendars()
        loadSelectedCalendar()
    }

    fun checkPermissionAndLoadCalendars() {
        viewModelScope.launch {
            val hasPermission = CalendarHelper.hasCalendarPermission(getApplication())
            _uiState.update { it.copy(hasCalendarPermission = hasPermission) }

            if (hasPermission) {
                loadCalendars()
            }
        }
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val calendars = CalendarHelper.getAvailableCalendars(getApplication())
            _uiState.update {
                it.copy(
                    availableCalendars = calendars,
                    isLoading = false
                )
            }
        }
    }

    private fun loadSelectedCalendar() {
        viewModelScope.launch {
            combine(
                settingsRepository.selectedCalendarId,
                settingsRepository.selectedCalendarName
            ) { id, name ->
                Pair(id, name)
            }.collectLatest { (id, name) ->
                _uiState.update {
                    it.copy(
                        selectedCalendarId = id,
                        selectedCalendarName = name
                    )
                }
            }
        }
    }

    fun selectCalendar(calendarId: String, calendarName: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedCalendar(calendarId, calendarName)
        }
    }

    fun clearCalendarSelection() {
        viewModelScope.launch {
            settingsRepository.clearSelectedCalendar()
        }
    }
}

