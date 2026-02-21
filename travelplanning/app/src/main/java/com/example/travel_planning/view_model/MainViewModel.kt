package com.example.travel_planning.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.net.URLEncoder
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TripRepository
) : ViewModel() {

    val trips: StateFlow<List<TripEntity>> =
        repository.getAllTrips()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _pendingDeleteIds = MutableStateFlow<List<Long>>(emptyList())
    val pendingDeleteIds: StateFlow<List<Long>> = _pendingDeleteIds

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    fun requestDeleteTrips(ids: List<Long>) {
        _pendingDeleteIds.value = ids
        _showDeleteDialog.value = true
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _pendingDeleteIds.value.forEach { id ->
                repository.deleteTripById(id)
            }
            cancelDelete()
        }
    }

    fun cancelDelete() {
        _pendingDeleteIds.value = emptyList()
        _showDeleteDialog.value = false
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }

    suspend fun buildShareText(tripId: Long): String? {
        val trip = trips.value.find { it.id_ == tripId } ?: return null
        val tripUI = repository.getTripForUI(tripId) ?: return null

        return buildString {
            appendLine("Поездка: ${trip.name}")
            if (!trip.date.isNullOrBlank()) appendLine("Дата: ${trip.date}")
            appendLine()
            appendLine("Места:")
            appendLine()

            tripUI.places.forEachIndexed { index, place ->
                appendLine("${index + 1}. ${place.name}")
                appendLine("   Адрес: ${place.address}")
                appendLine()
                val encoded = URLEncoder.encode(place.address, "UTF-8")
                appendLine("   https://yandex.ru/maps/?text=$encoded")
                appendLine()
            }

            appendLine("Создано в приложении Travel Planning")
        }
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