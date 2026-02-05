package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.repository.getTripForUI
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
    private var createdTripId: Long? = null
    private var selectedPlaceIds = listOf<String>()

    private val showDeleteDialog = mutableStateOf(false)
    private val showUnsavedDialog = mutableStateOf(false)
    private var currentTripForDialog: Trip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ids = intent.getStringArrayListExtra("SELECTED_PLACE_IDS")
        selectedPlaceIds = ids ?: emptyList()

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            TravelPlanningTheme {
                Surface {
                    val allSelectedPlaces = remember { mutableStateListOf<Place>() }
                    val hiddenPlaceIds = remember { mutableStateMapOf<String, Boolean>() }

                    var currentTrip by remember { mutableStateOf(Trip(places = emptyList())) }

                    val updateVisiblePlaces: () -> Unit = {
                        val visiblePlaces = allSelectedPlaces.filter { place ->
                            !hiddenPlaceIds.containsKey(place.id) || hiddenPlaceIds[place.id] != true
                        }

                        currentTrip = currentTrip.copy(places = visiblePlaces)
                    }

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK && result.data != null) {
                                val placeId = result.data!!.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                                val isSelected = result.data!!.getBooleanExtra("IS_SELECTED", true)

                                lifecycleScope.launch(Dispatchers.Main) {
                                    if (!isSelected) {
                                        allSelectedPlaces.removeAll { it.id == placeId }

                                        hiddenPlaceIds.remove(placeId)

                                        updateVisiblePlaces()

                                        createdTripId?.let { tripId ->
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                repository.removePlaceFromTrip(tripId, placeId)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )

                    val addPlaceLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                lifecycleScope.launch {
                                    createdTripId?.let { tripId ->
                                        val updatedTrip = repository.getTripForUI(tripId)
                                        if (updatedTrip != null) {
                                            allSelectedPlaces.clear()
                                            allSelectedPlaces.addAll(updatedTrip.places)

                                            updateVisiblePlaces()
                                        }
                                    }
                                }
                            }
                        }
                    )

                    LaunchedEffect(selectedPlaceIds) {
                        val places = withContext(Dispatchers.IO) {
                            val allPlaces: List<Place> = loadJsonListFromAssets(
                                this@EditTripFromCategory,
                                "all_places.json"
                            )
                            allPlaces.filter { it.id in selectedPlaceIds }
                        }

                        allSelectedPlaces.clear()
                        allSelectedPlaces.addAll(places)

                        currentTrip = Trip(places = places)
                    }

                    TripEditScreen(
                        trip = currentTrip,
                        tripId = createdTripId,
                        repository = repository,
                        onBackClick = { handleBack(currentTrip, allSelectedPlaces, hiddenPlaceIds) },
                        onSaveTrip = { tripToSave ->
                            saveTripAndGoToMain(
                                tripToSave,
                                allSelectedPlaces.toList(),
                                hiddenPlaceIds
                            ) { intentToMainActivity() }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripFromCategory, AddPlaceActivity::class.java)
                            intent.putExtra("TRIP_ID", createdTripId)
                            addPlaceLauncher.launch(intent)
                        },
                        onRemovePlaceClick = { tripId, placeId ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                if (createdTripId != null) {
                                    repository.removePlaceFromTrip(tripId, placeId)
                                }

                                allSelectedPlaces.removeAll { it.id == placeId }

                                hiddenPlaceIds.remove(placeId)

                                withContext(Dispatchers.Main) {
                                    updateVisiblePlaces()
                                }
                            }
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this@EditTripFromCategory, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra("TRIP_ID", createdTripId ?: 0L)
                            intent.putExtra("IS_SELECTED", true)
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = { showDeleteDialog.value = true },
                        onHiddenPlacesChanged = { hiddenPlaces ->
                            hiddenPlaceIds.clear()
                            hiddenPlaces.forEach { placeId ->
                                hiddenPlaceIds[placeId] = true
                            }

                            updateVisiblePlaces()
                        }
                    )

                    if (showDeleteDialog.value) {
                        DeleteConfirmationDialog(
                            title = "Вы уверены, что хотите удалить?",
                            onConfirm = {
                                lifecycleScope.launch {
                                    createdTripId?.let { repository.deleteTripById(it) }
                                    withContext(Dispatchers.Main) {
                                        showDeleteDialog.value = false
                                        intentToMainActivity()
                                    }
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
                                    if (createdTripId == null) {
                                        createdTripId = repository.createTrip(
                                            tripToSave.title,
                                            tripToSave.date,
                                            tripToSave.notes
                                        )
                                    } else {
                                        repository.updateTrip(
                                            createdTripId!!,
                                            tripToSave.title,
                                            tripToSave.date,
                                            tripToSave.notes
                                        )
                                    }

                                    createdTripId?.let { tripId ->
                                        allSelectedPlaces.forEach { place ->
                                            repository.addPlaceToTrip(tripId, place.toEntity())
                                        }

                                        hiddenPlaceIds.keys.forEach { placeId ->
                                            repository.removePlaceFromTrip(tripId, placeId)
                                        }
                                    }

                                    withContext(Dispatchers.Main) { intentToMainActivity() }
                                }
                            },
                            onDelete = {
                                showUnsavedDialog.value = false
                                if (createdTripId != null) {
                                    lifecycleScope.launch(Dispatchers.IO) { repository.deleteTripById(createdTripId!!) }
                                }
                                intentToMainActivity()
                            },
                            onDismiss = { showUnsavedDialog.value = false }
                        )
                    }
                }
            }
        }
    }

    private fun handleBack(
        currentTrip: Trip,
        allSelectedPlaces: SnapshotStateList<Place>,
        hiddenPlaceIds: SnapshotStateMap<String, Boolean>
    ) {
        currentTripForDialog = currentTrip

        val currentVisiblePlaceIds = currentTrip.places.map { it.id }.toSet()
        val allPlaceIds = allSelectedPlaces.map { it.id }.toSet()

        val newHiddenIds = allPlaceIds.minus(currentVisiblePlaceIds)
        hiddenPlaceIds.clear()
        newHiddenIds.forEach { placeId ->
            hiddenPlaceIds[placeId] = true
        }

        val hasChanges = currentTrip.title.isNotBlank() ||
                currentTrip.date.isNotBlank() ||
                currentTrip.notes.isNotBlank() ||
                newHiddenIds.isNotEmpty()

        if (hasChanges) showUnsavedDialog.value = true else intentToMainActivity()
    }

    private fun saveTripAndGoToMain(
        trip: Trip,
        allSelectedPlaces: List<Place>,
        hiddenPlaceIds: Map<String, Boolean>,
        onComplete: () -> Unit = {}
    ) {
        lifecycleScope.launch {
            val tripId = createdTripId ?: withContext(Dispatchers.IO) {
                repository.createTrip(trip.title, trip.date, trip.notes)
            }
            createdTripId = tripId

            withContext(Dispatchers.IO) {
                allSelectedPlaces.forEach { place ->
                    repository.addPlaceToTrip(tripId, place.toEntity())
                }

                hiddenPlaceIds.keys.forEach { placeId ->
                    repository.removePlaceFromTrip(tripId, placeId)
                }
            }
            onComplete()
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this@EditTripFromCategory, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }
}