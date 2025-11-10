package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.UnsavedTripDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var createdTripId: Long? = null
    private val originalTripState = mutableStateOf<Trip?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            TravelPlanningTheme {
                Surface {
                    val showExitDialog = remember { mutableStateOf(false) }
                    val currentTrip = remember { mutableStateOf(Trip(0, "", "", emptyList(), "")) }

                    TripEditScreen(
                        trip = null,
                        tripId = createdTripId,
                        repository = repository,
                        onBackClick = { currentTripState ->
                            currentTrip.value = currentTripState

                            if (originalTripState.value == null) {
                                originalTripState.value = Trip(0, "", "", emptyList(), "")
                            }

                            val hasChanges = hasTripChanged(currentTripState)

                            if (hasChanges || createdTripId != null) {
                                showExitDialog.value = true
                            } else {
                                intentToMainActivity()
                            }
                        },
                        onSaveTrip = { newTrip ->
                            lifecycleScope.launch {
                                saveTrip(newTrip.title, newTrip.date, newTrip.notes)
                                intentToMainActivity()
                            }
                        },
                        onAddPlaceClick = { newTrip ->
                            lifecycleScope.launch {
                                val tripId = createdTripId ?: saveTrip(newTrip.title, newTrip.date, newTrip.notes)
                                navigateToAddPlace(tripId, true)
                            }
                        },
                        onRemovePlaceClick = { currentTripId, placeId ->
                            lifecycleScope.launch {
                                repository.removePlaceFromTrip(currentTripId, placeId)
                            }
                        },
                        onPlaceClick = { place ->
                        },
                        deleteTrip = {}
                    )

                    if (showExitDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showExitDialog.value = false
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (createdTripId != null) {
                                            repository.updateTrip(
                                                tripId = createdTripId!!,
                                                name = currentTrip.value.title,
                                                date = currentTrip.value.date,
                                                notes = currentTrip.value.notes
                                            )
                                        } else {
                                            createdTripId = repository.createTrip(
                                                currentTrip.value.title,
                                                currentTrip.value.date,
                                                currentTrip.value.notes
                                            )
                                        }
                                    }
                                    intentToMainActivity()
                                }
                            },
                            onDelete = {
                                showExitDialog.value = false
                                revertToOriginalState()
                            },
                            onDismiss = {
                                showExitDialog.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hasTripChanged(currentTrip: Trip): Boolean {
        val original = originalTripState.value ?: return false
        return currentTrip.title != original.title ||
                currentTrip.date != original.date ||
                currentTrip.notes != original.notes
    }

    private fun revertToOriginalState() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                createdTripId?.let { repository.deleteTripById(it) }
            }
            intentToMainActivity()
        }
    }

    private fun intentToMainActivity() {
        val intentToMainActivity = Intent(this@AddTripActivity, MainActivity::class.java)
        startActivity(intentToMainActivity)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }

    private suspend fun saveTrip(name: String, date: String, notes: String): Long {
        return withContext(Dispatchers.IO) {
            val tripId = repository.createTrip(name, date, notes)
            createdTripId = tripId
            tripId
        }
    }

    private fun navigateToAddPlace(tripId: Long, isNewTrip: Boolean) {
        val intentToAddPlaceActivity = Intent(this, AddPlaceActivity::class.java)
        intentToAddPlaceActivity.putExtra("TRIP_ID", tripId)
        intentToAddPlaceActivity.putExtra("IS_NEW_TRIP", isNewTrip)
        startActivity(intentToAddPlaceActivity)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }
}