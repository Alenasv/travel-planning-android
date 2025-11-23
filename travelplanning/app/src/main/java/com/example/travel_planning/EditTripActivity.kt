package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.travel_planning.repository.getTripForUI
import com.example.travel_planning.utils.DeleteConfirmationDialog
import com.example.travel_planning.utils.UnsavedTripDialog

class EditTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var tripId: Long = -1
    private var isNewTrip = false
    private val tripState = mutableStateOf<Trip?>(null)
    private val showDeleteDialog = mutableStateOf(false)
    private val showUnsavedDialog = mutableStateOf(false)
    private var currentTripForDialog: Trip? = null
    private var hiddenPlacesIdsForDialog = listOf<String>()

    private val placeDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val placeId = data.getStringExtra("PLACE_ID")
            val isSelected = data.getBooleanExtra("IS_SELECTED", true)

            if (placeId != null && !isSelected) {
                lifecycleScope.launch {
                    repository.removePlaceFromTrip(tripId, placeId)
                    val updatedTrip = repository.getTripForUI(tripId)
                    withContext(Dispatchers.Main) {
                        tripState.value = updatedTrip
                    }
                }
            }
        }
    }
    private val addPlaceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                val updatedTrip = repository.getTripForUI(tripId)
                withContext(Dispatchers.Main) {
                    tripState.value = updatedTrip
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tripId = intent.getLongExtra("TRIP_ID", -1)
        isNewTrip = intent.getBooleanExtra("IS_NEW_TRIP", false)

        if (tripId == -1L) {
            finish()
            return
        }

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            TravelPlanningTheme {
                Surface {
                    LaunchedEffect(Unit) {
                        val loadedTrip = repository.getTripForUI(tripId)
                        tripState.value = loadedTrip
                    }

                    TripEditScreen(
                        trip = tripState.value ?: Trip(
                            id = 0,
                            title = "",
                            date = "",
                            notes = "",
                            places = emptyList()
                        ),
                        tripId = tripId,
                        repository = repository,
                        onBackClick = { currentTrip ->
                            handleBack(currentTrip)
                        },
                        onSaveTrip = { updatedTrip ->
                            saveTripAndGoToMain(updatedTrip) {
                                intentToMainActivity()
                            }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripActivity, AddPlaceActivity::class.java)
                            intent.putExtra("TRIP_ID", tripId)
                            intent.putExtra("IS_NEW_TRIP", isNewTrip)
                            addPlaceLauncher.launch(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onRemovePlaceClick = { currentTripId, placeId ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                repository.removePlaceFromTrip(currentTripId, placeId)
                            }
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra("TRIP_ID", tripId)
                            intent.putExtra("IS_SELECTED", true)
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {
                            showDeleteDialog.value = true
                        },

                        onHiddenPlacesChanged = { hiddenPlaces ->
                            hiddenPlacesIdsForDialog = hiddenPlaces
                        }

                    )
                    if (showDeleteDialog.value) {
                        DeleteConfirmationDialog(
                            title = "Вы уверены, что хотите удалить?",
                            onConfirm = {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        repository.deleteTripById(tripId)
                                    }
                                    withContext(Dispatchers.Main) {
                                        showDeleteDialog.value = false
                                        intentToMainActivity()
                                        finish()
                                    }
                                }
                            },
                            onDismiss = {
                                showDeleteDialog.value = false
                            }
                        )
                    }
                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                val tripToSave = currentTripForDialog ?: return@UnsavedTripDialog

                                lifecycleScope.launch(Dispatchers.IO) {
                                    repository.updateTrip(
                                        tripToSave.id,
                                        tripToSave.title,
                                        tripToSave.date,
                                        tripToSave.notes
                                    )

                                    hiddenPlacesIdsForDialog.forEach { placeId ->
                                        repository.removePlaceFromTrip(tripId, placeId)
                                    }

                                    withContext(Dispatchers.Main) {
                                        intentToMainActivity()
                                    }
                                }
                            },
                            onDelete = {
                                showUnsavedDialog.value = false
                                if (isNewTrip) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        repository.deleteTripById(tripId)
                                    }
                                }
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

    private fun saveTripAndGoToMain(trip: Trip?, onComplete: () -> Unit = {}) {
        if (trip != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                repository.updateTrip(trip.id, trip.title, trip.date, trip.notes)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this@EditTripActivity, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }

    private fun handleBack(currentTrip: Trip) {
        currentTripForDialog = currentTrip

        val originalTrip = tripState.value ?: return

        val originalPlaces = originalTrip.places
        val currentPlaceIds = currentTrip.places.map { it.id }.toSet()
        val originalPlaceIds = originalPlaces.map { it.id }.toSet()
        val calculatedHiddenPlaces = originalPlaceIds.minus(currentPlaceIds).toList()

        hiddenPlacesIdsForDialog = calculatedHiddenPlaces

        val hasChanges = currentTrip.title != originalTrip.title ||
                currentTrip.date != originalTrip.date ||
                currentTrip.notes != originalTrip.notes ||
                calculatedHiddenPlaces.isNotEmpty()

        if (isNewTrip) {
            val hasData = currentTrip.title.isNotBlank() ||
                    currentTrip.date.isNotBlank() ||
                    currentTrip.notes.isNotBlank() ||
                    currentTrip.places.isNotEmpty()

            if (hasData) {
                showUnsavedDialog.value = true
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.deleteTripById(tripId)
                }
                intentToMainActivity()
            }
            return
        }

        if (hasChanges) {
            showUnsavedDialog.value = true
        } else {
            intentToMainActivity()
        }
    }
}