package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.launch

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

        lifecycleScope.launch {
            val tripWithPlaces = repository.getTripWithPlaces(tripId)
            if (tripWithPlaces != null) {
                val currentTrip = Trip(
                    id = tripWithPlaces.trip.id_.toInt(),
                    title = tripWithPlaces.trip.name,
                    date = tripWithPlaces.trip.date ?: "",
                    notes = tripWithPlaces.trip.notes ?: "",
                    places = tripWithPlaces.places.map { placeEntity ->
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

                setContent {
                    TravelPlanningTheme {
                        Surface {
                            TripEditScreen(
                                trip = currentTrip,
                                tripId = tripId,
                                repository = repository,
                                onBackClick = {
                                    finish()
                                },
                                onSaveTrip = { updatedTrip ->
                                    lifecycleScope.launch {
                                       repository.updateTrip(
                                            tripId = tripId,
                                            name = updatedTrip.title,
                                            date = updatedTrip.date,
                                            notes = updatedTrip.notes
                                        )
                                    }
                                },
                                onAddPlaceClick = { trip ->
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
            } else {
                finish()
            }
        }
    }
}