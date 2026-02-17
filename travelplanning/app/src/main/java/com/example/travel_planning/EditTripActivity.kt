package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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

class EditTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var tripId: Long = -1
    private val tripState = mutableStateOf(
        Trip(
            id = 0,
            title = "",
            date = "",
            notes = "",
            places = emptyList()
        )
    )
    private var originalTrip: Trip? = null
    private val showDeleteDialog = mutableStateOf(false)
    private val showUnsavedDialog = mutableStateOf(false)

    private val addPlaceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIds = result.data?.getStringArrayListExtra("SELECTED_PLACE_IDS")
                ?: return@registerForActivityResult

            val allPlaces = loadJsonListFromAssets<Place>(this, "all_places.json")
            val updatedPlaces = allPlaces.filter { it.id in selectedIds }
            tripState.value = tripState.value.copy(places = updatedPlaces)
        }
    }

    private val placeDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val placeId = result.data?.getStringExtra("PLACE_ID")
            val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true

            if (placeId != null && !isSelected) {
                tripState.value = tripState.value.copy(
                    places = tripState.value.places.filter { it.id != placeId }
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tripId = intent.getLongExtra("TRIP_ID", -1)

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
                        val loadedTrip = repository.getTripForUI(tripId) ?: Trip(
                            id = tripId,
                            title = "",
                            date = "",
                            notes = "",
                            places = emptyList()
                        )
                        originalTrip = loadedTrip
                        tripState.value = loadedTrip
                    }

                    TripEditScreen(
                        trip = tripState.value,
                        isCreateMode = false,
                        showGeneratedTitle = false,
                        defaultTitle = "",
                        onBackClick = { currentTrip ->
                            handleBack(currentTrip)
                        },
                        onSaveTrip = { updatedTrip ->
                            saveTripAndGoToMain(updatedTrip)
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripActivity, AddPlaceActivity::class.java).apply {
                                putExtra("TRIP_ID", tripId)
                                putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(tripState.value.places.map { it.id })
                                )
                            }
                            addPlaceLauncher.launch(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onRemovePlaceClick = { placeId ->
                            tripState.value = tripState.value.copy(
                                places = tripState.value.places.filter { it.id != placeId }
                            )
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", true)
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {
                            showDeleteDialog.value = true
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
                                saveTripAndGoToMain(tripState.value)
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

    private fun saveTripAndGoToMain(trip: Trip) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTrip(
                trip.id,
                trip.title,
                trip.date,
                trip.notes
            )

            repository.replaceTripPlaces(trip.id, trip.places.map { it.toEntity() })

            withContext(Dispatchers.Main) {
                intentToMainActivity()
            }
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }

    private fun handleBack(currentTrip: Trip) {
        val original = originalTrip ?: return

        val hasChanges =
            currentTrip.title != original.title ||
                    currentTrip.date != original.date ||
                    currentTrip.notes != original.notes ||
                    currentTrip.places.map { it.id }.toSet() !=
                    original.places.map { it.id }.toSet()

        if (hasChanges) {
            showUnsavedDialog.value = true
        } else {
            intentToMainActivity()
        }
    }
}