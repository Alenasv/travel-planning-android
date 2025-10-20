package com.example.travel_planning

import AddToRouteScreen
import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.travel_planning.utils.toEntity
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets
import kotlinx.coroutines.launch

class AddPlaceActivity : ComponentActivity() {
    private lateinit var repository: TripRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) {
            finish()
            return
        }
        val allPlaces: List<Place> = loadJsonListFromAssets(this, "all_places.json")

        setContent {
            TravelPlanningTheme {
                Surface {
                    val selectedPlacesIds = remember { mutableStateOf(setOf<String>()) }

                    LaunchedEffect(tripId) {
                        val tripWithPlaces = repository.getTripWithPlaces(tripId)
                        selectedPlacesIds.value = tripWithPlaces?.places?.map { it.place_id }?.toSet() ?: emptySet()
                    }

                    AddToRouteScreen(
                        places = allPlaces,
                        onBackClick = { finish() },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            startActivity(intent)
                        },
                        onSaveTrip = { selectedPlaces ->
                            lifecycleScope.launch {
                                val currentPlaces = repository.getTripWithPlaces(tripId)?.places?.map { it.place_id }?.toSet() ?: emptySet()

                                selectedPlaces.filter { it.id !in currentPlaces }
                                    .forEach { repository.addPlaceToTrip(tripId, it.toEntity()) }
                                currentPlaces.filter { it !in selectedPlaces.map { p -> p.id } }
                                    .forEach { repository.removePlaceFromTrip(tripId, it) }

                                val intentEditTripActivity = Intent(this@AddPlaceActivity, EditTripActivity::class.java)
                                intentEditTripActivity.putExtra("TRIP_ID", tripId)
                                startActivity(intentEditTripActivity)
                            }
                        }
                    )
                }
            }
        }
    }

}

