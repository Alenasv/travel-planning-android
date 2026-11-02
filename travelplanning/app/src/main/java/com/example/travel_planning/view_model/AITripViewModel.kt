package com.example.travel_planning.view_model
import com.example.travel_planning.network.Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.travel_planning.repository.DraftStore
import com.example.travel_planning.repository.TripDraft
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromInternal
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.utils.ClusterDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AITripUiState {
    object Loading : AITripUiState
    data class Success(val clusters: List<ClusterDto>) : AITripUiState
    data class Error(val message: String) : AITripUiState
}

@HiltViewModel
class AITripViewModel @Inject constructor(
    private val api: Api,
    private val drafts: DraftStore,
    private val savedState: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AITripUiState>(AITripUiState.Loading)
    val uiState: StateFlow<AITripUiState> = _uiState.asStateFlow()

    val pendingDraft = savedState.getStateFlow<String?>("pendingDraft", null)
    fun navigationHandled() { savedState["pendingDraft"] = null }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError = _generationError.asStateFlow()

    init {
        loadClusters()
    }

    fun loadClusters() {
        viewModelScope.launch {
            _uiState.value = AITripUiState.Loading
            try {
                val response = api.getClusters()
                _uiState.value = AITripUiState.Success(response.clusters)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AITripUiState.Error("Не удалось загрузить интересы. Проверьте подключение и повторите попытку.")
            }
        }
    }

    fun generateRoute(selectedTags: List<String>, selectedMetro: String?, topK: Int) {
        if (_isGenerating.value) return

        _isGenerating.value = true
        _generationError.value = null
        viewModelScope.launch {
            try {
                val response = api.recommend(
                    RecommendRequest(
                        user_preferences = selectedTags,
                        top_k = topK,
                        start_metro = selectedMetro?.takeIf { it.isNotBlank() }
                    )
                )
                if (response.places.isEmpty()) {
                    _generationError.value = "Места не найдены. Измените интересы или метро."
                    return@launch
                }
                val catalog = loadJsonListFromInternal(context, "places.json").associateBy { it.id }
                val places = response.places.map {
                    catalog[it.id] ?: Place(it.id, it.name, it.address.orEmpty(),
                        it.work_time, it.category, null, null, emptyList())
                }
                val id = drafts.newId()
                drafts.write(id, TripDraft(Trip(id = 0, title = "", date = "", notes = "", places = places),
                    defaultTitle = "Новая поездка"))
                savedState["pendingDraft"] = id
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _generationError.value = when (e) {
                    is retrofit2.HttpException -> when (e.code()) {
                        422 -> "Проверьте параметры подбора."
                        503 -> "Сервис пока недоступен. Попробуйте позже."
                        else -> "Ошибка сервера. Повторите попытку."
                    }
                    is java.io.IOException -> "Проверьте подключение и повторите попытку."
                    else -> "Не удалось подобрать места. Повторите попытку."
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }
}
