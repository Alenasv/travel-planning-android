package com.example.travel_planning.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import Api
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.utils.ClusterDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AITripUiState {
    object Loading : AITripUiState
    data class Success(val clusters: List<ClusterDto>) : AITripUiState
    data class Error(val message: String) : AITripUiState
}

sealed interface AITripNavigationEvent {
    object NavigateToEditTrip : AITripNavigationEvent
}

class AITripViewModel(private val api: Api) : ViewModel() {

    private val _uiState = MutableStateFlow<AITripUiState>(AITripUiState.Loading)
    val uiState: StateFlow<AITripUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AITripNavigationEvent>()
    val navigationEvent: SharedFlow<AITripNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        loadClusters()
    }

    fun loadClusters() {
        viewModelScope.launch {
            _uiState.value = AITripUiState.Loading
            try {
                val response = api.getClusters()
                _uiState.value = AITripUiState.Success(response.clusters)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AITripUiState.Error(e.localizedMessage ?: "Ошибка загрузки интересов")
            }
        }
    }

    fun generateRoute(selectedTags: List<String>, selectedMetro: String?, topK: Int) {
        if (_isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val response = api.recommend(
                    RecommendRequest(
                        user_preferences = selectedTags,
                        top_k = topK,
                        start_metro = selectedMetro?.takeIf { it.isNotBlank() }
                    )
                )
                SelectedPlacesHolder.places = response.places
                _navigationEvent.emit(AITripNavigationEvent.NavigateToEditTrip)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

class AITripViewModelFactory(private val api: Api) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AITripViewModel(api) as T
    }
}
object SelectedPlacesHolder {
    var places: List<com.example.travel_planning.network.model.RecommendedPlace> = emptyList()

    fun clear() {
        places = emptyList()
    }
}