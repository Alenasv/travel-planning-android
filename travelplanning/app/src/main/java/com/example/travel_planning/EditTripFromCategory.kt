package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTripFromCategory : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var selectedPlaceIds = listOf<String>()
    private val showUnsavedDialog = mutableStateOf(false)
    private var currentTrip by mutableStateOf(Trip())
    private val highlightTitleError = mutableStateOf(false)

    private val hiddenPlaceIds = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedPlaceIds = intent.getStringArrayListExtra("SELECTED_PLACE_IDS") ?: emptyList()

        repository = TripRepository(AppDatabase.getDatabase(applicationContext))
  setContent {
            val initialTrip = remember { mutableStateOf<Trip?>(null) }

            TravelPlanningTheme {
                Surface {
                    val visiblePlaces = remember { mutableStateListOf<Place>() }


                    LaunchedEffect(Unit) {
                        val defaultName = repository.getNextDefaultTripName()
                        val allPlaces = loadJsonListFromAssets<Place>(this@EditTripFromCategory, "all_places.json")
                        val selectedPlaces = allPlaces.filter { it.id in selectedPlaceIds }
                        visiblePlaces.clear()
                        visiblePlaces.addAll(selectedPlaces)

                        currentTrip = Trip(
                            title = defaultName,
                            date = "",
                            notes = "",
                            places = visiblePlaces.toList()
                        )
                    }


                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK && result.data != null) {
                            val placeId = result.data!!.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data!!.getBooleanExtra("IS_SELECTED", true)

                            if (!isSelected) visiblePlaces.removeAll { it.id == placeId }
                            else {
                                val allPlaces = loadJsonListFromAssets<Place>(this@EditTripFromCategory, "all_places.json")
                                val place = allPlaces.find { it.id == placeId }
                                if (place != null && visiblePlaces.none { it.id == placeId }) visiblePlaces.add(place)
                            }

                            currentTrip = currentTrip.copy(places = visiblePlaces.toList())
                        }
                    }

                    TripEditScreen(
                        trip = currentTrip,
                        highlightTitleError = highlightTitleError.value,
                        onBackClick = { editedTrip ->
                            handleBack(editedTrip, visiblePlaces)
                        },
                        onSaveTrip = { tripToSave ->
                            saveTripAndGoToMain(tripToSave)
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripFromCategory, AddPlaceActivity::class.java)
                            intent.putStringArrayListExtra("SELECTED_PLACE_IDS", ArrayList(visiblePlaces.map { it.id }))
                            startActivity(intent)
                        },
                        onRemovePlaceClick = { _, placeId ->

                            visiblePlaces.removeAll { it.id == placeId }
                            currentTrip = currentTrip.copy(places = visiblePlaces.toList())

                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this@EditTripFromCategory, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra("IS_SELECTED", visiblePlaces.any { it.id == place.id })
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {

                            intentToMainActivity()
                        }
                    )

                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                saveTripAndGoToMain(currentTrip)
                            },
                            onDelete = {
                                showUnsavedDialog.value = false
                                intentToMainActivity()
                            },
                            onDismiss = {
                                showUnsavedDialog.value = false
                            }
                        )
                    }

                }
            }
        }
    }

    private fun handleBack(editedTrip: Trip, visiblePlaces: List<Place>) {
        val isEmpty = editedTrip.title.isBlank() &&
                editedTrip.date.isBlank() &&
                editedTrip.notes.isBlank() &&
                visiblePlaces.isEmpty()

        val hasChanges = editedTrip.title.isNotBlank() ||
                editedTrip.date.isNotBlank() ||
                editedTrip.notes.isNotBlank() ||
                visiblePlaces.isNotEmpty()

        if (isEmpty || !hasChanges) intentToMainActivity()
        else showUnsavedDialog.value = true
    }

    private fun saveTripAndGoToMain(trip: Trip) {
        lifecycleScope.launch(Dispatchers.IO) {
            val titleToSave = if (trip.title.isBlank()) {
                repository.getNextDefaultTripName()
            } else {
                trip.title
            }

            val tripId = repository.createTrip(titleToSave, trip.date, trip.notes)
            trip.places.forEach { repository.addPlaceToTrip(tripId, it.toEntity()) }

            withContext(Dispatchers.Main) { intentToMainActivity() }
        }
    }


    private fun intentToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
