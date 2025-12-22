package com.pb.myworkshiftplanner.ui.shifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pb.myworkshiftplanner.data.Shift
import com.pb.myworkshiftplanner.data.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShiftViewModel(private val repository: ShiftRepository) : ViewModel() {

    val allShifts = repository.allShifts

    private val _selectedShift = MutableStateFlow<Shift?>(null)
    val selectedShift: StateFlow<Shift?> = _selectedShift.asStateFlow()

    fun selectShift(shift: Shift?) {
        _selectedShift.value = shift
    }

    fun insertShift(shift: Shift) {
        viewModelScope.launch {
            repository.insert(shift)
        }
    }

    fun updateShift(shift: Shift) {
        viewModelScope.launch {
            repository.update(shift)
        }
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch {
            repository.delete(shift)
        }
    }
}

class ShiftViewModelFactory(private val repository: ShiftRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShiftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShiftViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

