package com.example.travel_planning.view_model
import com.example.travel_planning.network.Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.utils.ClusterDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AITripUiState {
    object Loading : AITripUiState
    data class Success(val clusters: List<ClusterDto>) : AITripUiState
    data class Error(val message: String) : AITripUiState
}

sealed interface AITripNavigationEvent {
    object NavigateToEditTrip : AITripNavigationEvent
}



@HiltViewModel
class AITripViewModel @Inject constructor(
    private val api: Api
) : ViewModel() {

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

object SelectedPlacesHolder {
    var places: List<com.example.travel_planning.network.model.RecommendedPlace> = emptyList()

    fun clear() {
        places = emptyList()
    }
}