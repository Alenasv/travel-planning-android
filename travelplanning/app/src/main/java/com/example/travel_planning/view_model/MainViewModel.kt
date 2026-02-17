package com.example.travel_planning.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TripRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pendingDeleteIds = MutableStateFlow<List<Long>>(emptyList())
    val pendingDeleteIds: StateFlow<List<Long>> = _pendingDeleteIds.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    init {
        loadTrips()
    }

    fun loadTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trips.value = repository.getAllTrips()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestDeleteTrips(ids: List<Long>) {
        _pendingDeleteIds.value = ids
        _showDeleteDialog.value = true
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _pendingDeleteIds.value.forEach { id ->
                    repository.deleteTripById(id)
                }
                loadTrips()
                cancelDelete()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelDelete() {
        _pendingDeleteIds.value = emptyList()
        _showDeleteDialog.value = false
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }
}

class MainViewModelFactory(
    private val repository: TripRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repository) as T
    }
}