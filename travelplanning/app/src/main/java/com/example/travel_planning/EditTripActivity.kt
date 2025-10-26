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

class EditTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var tripId: Long = -1

    private val tripState = mutableStateOf<Trip?>(null)
    private val showDeleteDialog = mutableStateOf(false)

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
                        onBackClick = {
                            intentToMainActivity()
                            finish()
                        },
                        onSaveTrip = { updatedTrip ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.updateTrip(
                                        tripId = tripId,
                                        name = updatedTrip.title,
                                        date = updatedTrip.date,
                                        notes = updatedTrip.notes
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    intentToMainActivity()
                                    finish()
                                }
                            }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@EditTripActivity, AddPlaceActivity::class.java)
                            intent.putExtra("TRIP_ID", tripId)
                            startActivity(intent)
                        },
                        onRemovePlaceClick = { currentTripId, placeId ->
                            lifecycleScope.launch {
                                repository.removePlaceFromTrip(currentTripId, placeId)
                                val updatedTrip = repository.getTripForUI(tripId)
                                withContext(Dispatchers.Main) {
                                    tripState.value = updatedTrip
                                }
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

                }
            }
        }
    }

private fun intentToMainActivity() {
        val intent = Intent(this@EditTripActivity, MainActivity::class.java)
        startActivity(intent)
    }

}