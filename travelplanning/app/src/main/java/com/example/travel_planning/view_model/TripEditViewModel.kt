package com.example.travel_planning.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TripEditViewModel(
    private val repository: TripRepository
) : ViewModel() {

    private val _tripState = MutableStateFlow(
        com.example.travel_planning.ui.Trip(
            id = 0,
            title = "",
            date = "",
            notes = "",
            places = emptyList()
        )
    )
    val tripState: StateFlow<Trip> = _tripState.asStateFlow()

    private var originalTrip: Trip? = null

    private val _defaultTitle = MutableStateFlow("")
    val defaultTitle: StateFlow<String> = _defaultTitle.asStateFlow()

    private val _hasReturnedFromChild = MutableStateFlow(false)
    val hasReturnedFromChild: StateFlow<Boolean> = _hasReturnedFromChild.asStateFlow()

    private val _highlightTitleError = MutableStateFlow(false)
    val highlightTitleError: StateFlow<Boolean> = _highlightTitleError.asStateFlow()

    suspend fun initCreateMode() {
        _defaultTitle.value = repository.getNextDefaultTripName()
        _tripState.value = Trip(
            id = 0,
            title = "",
            date = "",
            notes = "",
            places = emptyList()
        )
        originalTrip = null
    }

    suspend fun initEditMode(tripId: Long) {
        val loaded: Trip? = repository.getTripForUI(tripId)

        _tripState.value = loaded ?: Trip(
            id = tripId,
            title = "",
            date = "",
            notes = "",
            places = emptyList()
        )

        originalTrip = _tripState.value
        _defaultTitle.value = _tripState.value.title
    }

    fun updateTripTitle(title: String) {
        _tripState.update { it.copy(title = title) }
        if (title.isNotBlank()) {
            _highlightTitleError.value = false
        }
    }

    fun updateTripDate(date: String) {
        _tripState.update { it.copy(date = date) }
    }

    fun updateTripNotes(notes: String) {
        _tripState.update { it.copy(notes = notes) }
    }

    fun removePlace(placeId: String) {
        _tripState.update {
            it.copy(places = it.places.filter { place -> place.id != placeId })
        }
    }

    fun setPlaces(places: List<Place>) {
        _tripState.update { it.copy(places = places) }
    }

    fun addPlace(place: Place) {
        _tripState.update {
            if (it.places.none { p -> p.id == place.id }) {
                it.copy(places = it.places + place)
            } else {
                it
            }
        }
    }

    fun onReturnedFromChild() {
        _hasReturnedFromChild.value = true
    }

    fun validateTitle(): Boolean {
        val isValid = _tripState.value.title.isNotBlank()
        _highlightTitleError.value = !isValid
        return isValid
    }

    fun hasChanges(): Boolean {
        val current = _tripState.value
        val original = originalTrip ?: return current.title.isNotBlank() ||
                current.date.isNotBlank() ||
                current.notes.isNotBlank() ||
                current.places.isNotEmpty()

        return current.title != original.title ||
                current.date != original.date ||
                current.notes != original.notes ||
                current.places.map { it.id }.toSet() !=
                original.places.map { it.id }.toSet()
    }

    fun isEmpty(): Boolean {
        val current = _tripState.value
        return current.title.isBlank() &&
                current.date.isBlank() &&
                current.notes.isBlank() &&
                current.places.isEmpty()
    }

    suspend fun saveCreate(): Long {
        val finalTitle = if (_tripState.value.title.isBlank())
            _defaultTitle.value
        else
            _tripState.value.title

        val newTripId = repository.createTrip(
            finalTitle,
            _tripState.value.date,
            _tripState.value.notes
        )

        if (_tripState.value.places.isNotEmpty()) {
            repository.replaceTripPlaces(
                newTripId,
                _tripState.value.places.map { it.toEntity() }
            )
        }

        return newTripId
    }

    suspend fun saveEdit() {
        repository.updateTrip(
            _tripState.value.id,
            _tripState.value.title,
            _tripState.value.date,
            _tripState.value.notes
        )

        repository.replaceTripPlaces(
            _tripState.value.id,
            _tripState.value.places.map { it.toEntity() }
        )
    }

    suspend fun saveWithSelectedPlaces(selectedPlaces: List<Place>): Long {
        val finalTitle = if (_tripState.value.title.isBlank())
            _defaultTitle.value
        else
            _tripState.value.title

        val newTripId = repository.createTrip(
            finalTitle,
            _tripState.value.date,
            _tripState.value.notes
        )

        if (selectedPlaces.isNotEmpty()) {
            repository.replaceTripPlaces(
                newTripId,
                selectedPlaces.map { it.toEntity() }
            )
        }

        return newTripId
    }
    suspend fun initFromSelectedPlaces(selectedPlaceIds: List<String>, selectedPlaces: List<Place>) {
        val defaultName = repository.getNextDefaultTripName()
        _defaultTitle.value = defaultName

        _tripState.value = Trip(
            id = 0,
            title = defaultName,
            date = "",
            notes = "",
            places = selectedPlaces
        )
        originalTrip = null
    }
}

class TripEditViewModelFactory(
    private val repository: TripRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TripEditViewModel(repository) as T
    }
}