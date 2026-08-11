package com.example.travel_planning.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.network.downloadImage
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromInternal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class PlaceDetailViewModel : ViewModel() {

    private val _currentPlace = MutableStateFlow<Place?>(null)
    val currentPlace: StateFlow<Place?> = _currentPlace.asStateFlow()

    private val _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> = _isSelected.asStateFlow()

    private val _imageFile = MutableStateFlow<File?>(null)
    val imageFile: StateFlow<File?> = _imageFile.asStateFlow()

    private var isStateChanged = false

    fun initData(context: Context, placeId: String?, initialSelection: Boolean) {
        if (_currentPlace.value != null) return

        _isSelected.value = initialSelection

        val places: List<Place> = loadJsonListFromInternal(context, "all_places.json")
        val place = places.find { it.id == placeId }
        _currentPlace.value = place

        place?.let {
            val localFile = File(context.filesDir, it.image_filename)
            if (localFile.exists()) {
                _imageFile.value = localFile
            } else {
                downloadPlaceImage(context, it.image_filename)
            }
        }
    }

    private fun downloadPlaceImage(context: Context, filename: String) {
        if (filename.isEmpty()) return
        viewModelScope.launch {
            val downloaded = downloadImage(context, filename)
            if (downloaded != null) {
                _imageFile.value = downloaded
            }
        }
    }

    fun toggleRouteSelection() {
        _isSelected.value = !_isSelected.value
        isStateChanged = true
    }

    fun isSelectionChanged(): Boolean = isStateChanged
}