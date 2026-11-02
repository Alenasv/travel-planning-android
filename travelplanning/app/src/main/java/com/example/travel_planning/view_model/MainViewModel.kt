package com.example.travel_planning.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.net.URLEncoder
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.travel_planning.network.Api
import android.content.Context
import android.util.Log
import com.example.travel_planning.network.downloadPlacesJson
import com.example.travel_planning.network.downloadMetroStationsJson
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TripRepository,
    private val api: Api,
    @ApplicationContext private val context: Context
) : ViewModel() {


    val trips: StateFlow<List<TripEntity>> =
        repository.getAllTrips()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    private val _allPlaces = MutableStateFlow<List<com.example.travel_planning.utils.Place>>(emptyList())
    val allPlaces: StateFlow<List<com.example.travel_planning.utils.Place>> = _allPlaces

    private val _isLoadingState = MutableStateFlow(_allPlaces.value.isEmpty())
    val isLoadingState: StateFlow<Boolean> = _isLoadingState


    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingState.value = true

            try {
                _allPlaces.value =
                    com.example.travel_planning.utils
                        .loadJsonListFromInternal(context, "places.json")

                val success = downloadPlacesJson(api, context)

                if (success) {
                    _allPlaces.value =
                        com.example.travel_planning.utils
                            .loadJsonListFromInternal(context, "places.json")

                    Log.d("PLACES", "Места обновлены из PostgreSQL")
                } else {
                    Log.e("PLACES", "Не удалось обновить места с сервера")
                }
                val metroSuccess = downloadMetroStationsJson(api, context)

                if (metroSuccess) {
                    Log.d("METRO", "Станции метро обновлены из PostgreSQL")
                } else {
                    Log.e("METRO", "Не удалось обновить станции метро с сервера")
                }
            } catch (e: Exception) {
                Log.e("PLACES", "Ошибка загрузки: ${e.message}", e)
            } finally {
                _isLoadingState.value = false
            }
        }
    }

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
