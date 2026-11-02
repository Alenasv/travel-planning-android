package com.example.travel_planning.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel_planning.network.downloadImage
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromInternal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val drafts: com.example.travel_planning.repository.DraftStore,
    private val repository: com.example.travel_planning.repository.TripRepository
) : ViewModel() {

    private val _currentPlace = MutableStateFlow<Place?>(null)
    val currentPlace: StateFlow<Place?> = _currentPlace.asStateFlow()

    private val _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> = _isSelected.asStateFlow()

    private val _imageFile = MutableStateFlow<File?>(null)
    val imageFile: StateFlow<File?> = _imageFile.asStateFlow()

    private var isStateChanged = false

    fun initData(context: Context, placeId: String?, initialSelection: Boolean, draftId: String? = null) {
        if (_currentPlace.value != null) return
        _isSelected.value = initialSelection
        viewModelScope.launch {
            val place = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val draftPlace = draftId?.let { drafts.read(it)?.trip?.places?.find { p -> p.id == placeId } }
                draftPlace ?: loadJsonListFromInternal(context, "places.json").find { it.id == placeId }
                    ?: placeId?.let { repository.getPlaceForUI(it) }
            }
            _currentPlace.value = place
            place?.image_filename?.takeIf { it.isNotBlank() }?.let { downloadPlaceImage(context, it) }
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