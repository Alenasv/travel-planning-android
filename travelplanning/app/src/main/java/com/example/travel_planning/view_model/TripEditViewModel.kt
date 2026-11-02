package com.example.travel_planning.view_model

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.repository.DraftStore
import com.example.travel_planning.repository.TripDraft
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromInternal
import com.example.travel_planning.utils.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class TripEditViewModel @Inject constructor(
    private val repository: TripRepository,
    private val drafts: DraftStore,
    private val savedState: SavedStateHandle
) : ViewModel() {
    val draftId = savedState.get<String>("editorDraft")
        ?: savedState.get<String>("DRAFT_ID")
        ?: drafts.newId()
    private val restored = drafts.read(draftId)
    private val _tripState = MutableStateFlow(restored?.trip ?: Trip(id = 0, title = "", date = "", notes = "", places = emptyList()))
    val tripState = _tripState.asStateFlow()
    private var originalTrip = restored?.original
    private val _defaultTitle = MutableStateFlow(restored?.defaultTitle.orEmpty())
    val defaultTitle = _defaultTitle.asStateFlow()
    private val _hasReturnedFromChild = MutableStateFlow(false)
    val hasReturnedFromChild = _hasReturnedFromChild.asStateFlow()
    private val _highlightTitleError = MutableStateFlow(false)
    val highlightTitleError = _highlightTitleError.asStateFlow()
    private var initialized = restored != null
    private val saveMutex = Mutex()

    init { savedState["editorDraft"] = draftId }

    @Synchronized
    private fun setTrip(trip: Trip) {
        drafts.write(draftId, TripDraft(trip, originalTrip, _defaultTitle.value))
        _tripState.value = trip
    }

    @Synchronized
    private fun change(transform: (Trip) -> Trip) = setTrip(transform(_tripState.value))

    suspend fun deleteTrip(tripId: Long) = repository.deleteTripById(tripId)

    suspend fun initCreateMode() {
        if (initialized) return
        initialized = true
        _defaultTitle.value = repository.getNextDefaultTripName()
        originalTrip = null
        setTrip(Trip(id = 0, title = "", date = "", notes = "", places = emptyList()))
    }

    suspend fun initEditMode(tripId: Long) {
        if (initialized) return
        initialized = true
        val trip = repository.getTripForUI(tripId) ?: error("Trip not found: $tripId")
        originalTrip = trip
        _defaultTitle.value = trip.title
        setTrip(trip)
    }

    fun loadData(mode: String, tripId: Long) {
        viewModelScope.launch {
            if (mode == "AI") initCreateMode() else initEditMode(tripId)
        }
    }

    suspend fun saveTrip(mode: String) {
        if (mode == "AI") saveCreate() else saveEdit()
    }

    fun updateTripTitle(title: String) {
        change { it.copy(title = title) }
        if (title.isNotBlank()) _highlightTitleError.value = false
    }
    fun updateTripDate(date: String) = change { it.copy(date = date) }
    fun updateTripNotes(notes: String) = change { it.copy(notes = notes) }

    fun initSelectedPlaces(context: Context, selectedIds: List<String>) {
        if (initialized) return
        initialized = true
        handleSelectedPlaces(context, selectedIds)
    }

    fun handleSelectedPlaces(context: Context, selectedIds: List<String>) {
        val available = (loadJsonListFromInternal(context, "places.json") + _tripState.value.places)
            .associateBy { it.id }
        change { it.copy(places = selectedIds.distinct().mapNotNull(available::get)) }
    }

    fun handlePlaceDetailResult(context: Context, placeId: String, isSelected: Boolean) {
        if (!isSelected) removePlace(placeId)
        else loadJsonListFromInternal(context, "places.json").find { it.id == placeId }?.let(::addPlace)
    }

    fun removePlace(placeId: String) = change { it.copy(places = it.places.filter { p -> p.id != placeId }) }
    fun setPlaces(places: List<Place>) = change { it.copy(places = places) }
    fun addPlace(place: Place) = change {
        if (it.places.any { p -> p.id == place.id }) it else it.copy(places = it.places + place)
    }
    fun onReturnedFromChild() { _hasReturnedFromChild.value = true }
    fun validateTitle(): Boolean {
        val valid = _tripState.value.title.isNotBlank()
        _highlightTitleError.value = !valid
        return valid
    }
    fun hasChanges(): Boolean {
        val current = _tripState.value
        return originalTrip?.let { current != it }
            ?: (current.title.isNotBlank() || current.date.isNotBlank() ||
                current.notes.isNotBlank() || current.places.isNotEmpty())
    }
    fun isEmpty(): Boolean = !hasChanges()

    private suspend fun persist(places: List<Place>? = null): Long = saveMutex.withLock {
        val trip = _tripState.value.let {
            it.copy(title = it.title.ifBlank { _defaultTitle.value.ifBlank { "Новая поездка" } },
                places = places ?: it.places)
        }
        val id = repository.saveTrip(trip.id, trip.title, trip.date, trip.notes, trip.places.map { it.toEntity() })
        val saved = trip.copy(id = id)
        originalTrip = saved
        setTrip(saved)
        id
    }
    suspend fun saveCreate(): Long = persist()
    suspend fun saveEdit() { persist() }
    suspend fun saveWithSelectedPlaces(selectedPlaces: List<Place>): Long = persist(selectedPlaces)

    suspend fun initFromSelectedPlaces(selectedPlaceIds: List<String>, selectedPlaces: List<Place>) {
        if (initialized) return
        initCreateMode()
        setTrip(_tripState.value.copy(title = _defaultTitle.value, places = selectedPlaces))
    }
}
