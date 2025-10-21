package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.delay

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
                        trip = withContext(Dispatchers.IO) {
                            repository.getTripWithPlaces(tripId)?.let {
                                Trip(
                                    id = it.trip.id_.toInt(),
                                    title = it.trip.name,
                                    date = it.trip.date ?: "",
                                    notes = it.trip.notes ?: "",
                                    places = it.places.map { placeEntity ->
                                        Place(
                                            id = placeEntity.place_id,
                                            name = placeEntity.name,
                                            address = placeEntity.address,
                                            work_time = placeEntity.work_time ?: "",
                                            category = placeEntity.category,
                                            description = placeEntity.description ?: "",
                                            image_filename = placeEntity.imageFilename ?: ""
                                        )
                                    }
                                )
                            }
                        }
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
                                    val intent = Intent(this@EditTripActivity, MainActivity::class.java)
                                    startActivity(intent)
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
                            }
                        }
                    )
                }
            }
        }
    }
 }