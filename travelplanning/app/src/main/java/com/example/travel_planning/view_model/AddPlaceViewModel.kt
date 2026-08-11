package com.example.travel_planning.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.network.ApiClient
import com.example.travel_planning.utils.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddPlaceViewModel : ViewModel() {

    private val _selectedPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPlaceIds: StateFlow<Set<String>> = _selectedPlaceIds.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow(TextFieldValue(""))
    val searchQuery: StateFlow<TextFieldValue> = _searchQuery.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _allPlaces = MutableStateFlow<List<Place>>(emptyList())

    val filteredPlaces: StateFlow<List<Place>> = combine(
        _allPlaces,
        _selectedCategory,
        _searchQuery
    ) { places, category, query ->
        var result = places
        if (query.text.isNotBlank()) {
            val lowercaseQuery = query.text.lowercase()
            result = result.filter { it.name.lowercase().contains(lowercaseQuery) }
        }
        if (category != "Все") {
            result = result.filter { it.category == category }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initPlaces(places: List<Place>) {
        _allPlaces.value = places
    }

    fun setInitialSelectedPlaces(ids: Set<String>) {
        _selectedPlaceIds.value = ids
    }

    fun setInitialCategory(category: String) {
        _selectedCategory.value = category
    }

    fun togglePlace(placeId: String, toggled: Boolean) {
        _selectedPlaceIds.value =
            if (toggled) _selectedPlaceIds.value + placeId
            else _selectedPlaceIds.value - placeId
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun changeSearchQuery(query: TextFieldValue) {
        _searchQuery.value = query
    }

    fun checkConnection() {
        viewModelScope.launch {
            _isOnline.value = try {
                ApiClient.api.ping()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}