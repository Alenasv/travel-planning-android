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
import com.example.travel_planning.utils.DeleteConfirmationDialog
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTripFromCategory : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var selectedPlaceIds = listOf<String>()

    private val showDeleteDialog = mutableStateOf(false)
    private val showUnsavedDialog = mutableStateOf(false)
    private var currentTripForDialog: Trip? = null

    private val hiddenPlaceIds = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedPlaceIds = intent.getStringArrayListExtra("SELECTED_PLACE_IDS") ?: emptyList()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            val initialTrip = remember { mutableStateOf<Trip?>(null) }

            TravelPlanningTheme {
                Surface {
                    val visiblePlaces = remember { mutableStateListOf<Place>() }
                    var currentTrip by remember { mutableStateOf(Trip(places = emptyList())) }

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK && result.data != null) {
                            val placeId = result.data!!.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data!!.getBooleanExtra("IS_SELECTED", true)

                            if (!isSelected) {
                                visiblePlaces.removeAll { it.id == placeId }
                                hiddenPlaceIds.add(placeId)
                            } else {
                                val allPlaces: List<Place> = loadJsonListFromAssets(this, "all_places.json")
                                val place = allPlaces.find { it.id == placeId }
                                if (place != null && visiblePlaces.none { it.id == placeId } && !hiddenPlaceIds.contains(placeId)) {
                                    visiblePlaces.add(place)
                                }
                                hiddenPlaceIds.remove(placeId)
                            }

                            currentTrip = currentTrip.copy(places = visiblePlaces.toList())
                        }
                    }

                    LaunchedEffect(Unit) {
                        val places = withContext(Dispatchers.IO) {
                            val allPlaces: List<Place> = loadJsonListFromAssets(
                                this@EditTripFromCategory,
                                "all_places.json"
                            )
                            allPlaces.filter { it.id in selectedPlaceIds }
                        }

                        visiblePlaces.clear()
                        visiblePlaces.addAll(places.filter { it.id !in hiddenPlaceIds })

                        currentTrip = Trip(places = visiblePlaces.toList())
                        initialTrip.value = currentTrip.copy()
                    }

                    val softRemovePlace: (String) -> Unit = { placeId ->
                        hiddenPlaceIds.add(placeId)
                        visiblePlaces.removeAll { it.id == placeId }
                        currentTrip = currentTrip.copy(places = visiblePlaces.toList())
                    }

                    TripEditScreen(
                        trip = currentTrip.copy(places = visiblePlaces.toList()),
                        onHiddenPlacesChanged = { ids ->
                            hiddenPlaceIds.clear()
                            hiddenPlaceIds.addAll(ids)
                        },
                        tripId = null,
                        repository = repository,
                        onBackClick = {
                            handleBack(currentTrip, visiblePlaces, initialTrip.value)
                        },
                        onSaveTrip = { tripToSave ->
                            saveTripAndGoToMain(
                                tripToSave.copy(places = visiblePlaces.toList()),
                                visiblePlaces.toList()
                            ) { intentToMainActivity() }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripFromCategory, AddPlaceActivity::class.java)
                            intent.putStringArrayListExtra("SELECTED_PLACE_IDS", ArrayList(visiblePlaces.map { it.id }))
                            startActivity(intent)
                        },
                        onRemovePlaceClick = { _, placeId ->
                            softRemovePlace(placeId)
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this@EditTripFromCategory, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra("IS_SELECTED", visiblePlaces.any { it.id == place.id })
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {
                            val hasChanges = currentTrip.title.isNotBlank() ||
                                    currentTrip.date.isNotBlank() ||
                                    currentTrip.notes.isNotBlank() ||
                                    visiblePlaces.isNotEmpty()

                            if (hasChanges) showDeleteDialog.value = true
                            else intentToMainActivity()
                        }
                    )

                    if (showDeleteDialog.value) {
                        DeleteConfirmationDialog(
                            title = "Вы уверены, что хотите удалить?",
                            onConfirm = {
                                lifecycleScope.launch {
                                    showDeleteDialog.value = false
                                    intentToMainActivity()
                                }
                            },
                            onDismiss = { showDeleteDialog.value = false }
                        )
                    }

                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                val tripToSave = currentTripForDialog ?: return@UnsavedTripDialog
                                lifecycleScope.launch(Dispatchers.IO) {
                                    saveTripToDatabase(tripToSave, visiblePlaces.toList())
                                    withContext(Dispatchers.Main) { intentToMainActivity() }
                                }
                            },
                            onDelete = {
                                showUnsavedDialog.value = false
                                intentToMainActivity()
                            },
                            onDismiss = { showUnsavedDialog.value = false }
                        )
                    }
                }
            }
        }
    }

    private fun handleBack(currentTrip: Trip, visiblePlaces: List<Place>, initialTrip: Trip?) {
        val displayedPlaces = visiblePlaces
        val isTripEmpty = currentTrip.title.isBlank() &&
                currentTrip.date.isBlank() &&
                currentTrip.notes.isBlank() &&
                displayedPlaces.isEmpty()

        val hasChanges = initialTrip != null && initialTrip.let { init ->
            init.title != currentTrip.title ||
                    init.date != currentTrip.date ||
                    init.notes != currentTrip.notes ||
                    init.places.map { it.id } != displayedPlaces.map { it.id }
        }



        if (hasChanges && !isTripEmpty) {
            currentTripForDialog = currentTrip.copy(places = displayedPlaces)
            showUnsavedDialog.value = true
        } else {
            intentToMainActivity()
        }

    }

    private suspend fun saveTripToDatabase(trip: Trip, places: List<Place>) {
        val tripId = repository.createTrip(trip.title, trip.date, trip.notes)
        places.forEach { place -> repository.addPlaceToTrip(tripId, place.toEntity()) }
    }

    private fun saveTripAndGoToMain(trip: Trip, places: List<Place>, onComplete: () -> Unit = {}) {
        lifecycleScope.launch(Dispatchers.IO) {
            saveTripToDatabase(trip, places)
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this@EditTripFromCategory, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }
}
