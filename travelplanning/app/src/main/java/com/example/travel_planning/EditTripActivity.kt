package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import com.example.travel_planning.repository.getTripForUI

class EditTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var tripId: Long = -1

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
                    var trip by remember { mutableStateOf<Trip?>(null) }

                    LaunchedEffect(Unit) {
                        trip = repository.getTripForUI(tripId)
                    }

                    TripEditScreen(
                        trip = trip ?: Trip(
                            id = 0,
                            title = "",
                            date = "",
                            notes = "",
                            places = emptyList()
                        ),
                        tripId = tripId,
                        repository = repository,
                        onBackClick = { finish() },
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
                                    val intentToMainActivity = Intent(this@EditTripActivity, MainActivity::class.java)
                                    startActivity(intentToMainActivity)
                                    finish()
                                }
                            }
                        },
                        onAddPlaceClick = {
                            val intentToAddPlaceActivity = Intent(this@EditTripActivity, AddPlaceActivity::class.java)
                            intentToAddPlaceActivity.putExtra("TRIP_ID", tripId)
                            startActivity(intentToAddPlaceActivity)
                        },
                        onRemovePlaceClick = { currentTripId, placeId ->
                            lifecycleScope.launch {
                                repository.removePlaceFromTrip(currentTripId, placeId)
                            }
                        }
                    )
                }
            }
        }
    }
 }