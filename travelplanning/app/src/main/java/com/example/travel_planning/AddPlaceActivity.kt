package com.example.travel_planning

import AddToRouteScreen
import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPlaceActivity : ComponentActivity() {

    private lateinit var repository: TripRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) finish()

        val allPlaces: List<Place> = loadJsonListFromAssets(this, "all_places.json")

        setContent {
            TravelPlanningTheme {
                Surface {
                    val selectedPlacesIds = remember { mutableStateOf(setOf<String>()) }

                    LaunchedEffect(tripId) {
                        val tripWithPlaces = withContext(Dispatchers.IO) { repository.getTripWithPlaces(tripId) }
                        selectedPlacesIds.value = tripWithPlaces?.places?.map { it.place_id }?.toSet() ?: emptySet()
                    }

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val addedPlaceId = result.data?.getStringExtra("ADDED_PLACE_ID")
                            if (addedPlaceId != null) {
                                selectedPlacesIds.value += addedPlaceId
                            }
                        }
                    }


                    AddToRouteScreen(
                        tripId = tripId,
                        selectedPlacesIds = selectedPlacesIds.value,
                        places = allPlaces,
                        onBackClick = { finish() },
                        onPlaceClick = { place ->
                            val intentToPlaceDetailActivity = Intent(this@AddPlaceActivity, PlaceDetailActivity::class.java)
                            intentToPlaceDetailActivity.putExtra("PLACE_ID", place.id)
                            intentToPlaceDetailActivity.putExtra("TRIP_ID", tripId)
                            placeDetailLauncher.launch(intentToPlaceDetailActivity)
                        },
                        onTogglePlace = { placeId, toggled ->
                            selectedPlacesIds.value = if (toggled)
                                selectedPlacesIds.value + placeId
                            else
                                selectedPlacesIds.value - placeId
                        },
                        onSaveTrip = { selectedPlaces ->
                            lifecycleScope.launch {
                                val currentPlaces = withContext(Dispatchers.IO) {
                                    repository.getTripWithPlaces(tripId)?.places?.map { it.place_id }?.toSet() ?: emptySet()
                                }

                                withContext(Dispatchers.IO) {
                                    selectedPlaces.filter { it.id !in currentPlaces }
                                        .forEach { repository.addPlaceToTrip(tripId, it.toEntity()) }

                                    currentPlaces.filter { it !in selectedPlaces.map { p -> p.id } }
                                        .forEach { repository.removePlaceFromTrip(tripId, it) }
                                }

                                val intentToEditTripActivity = Intent(this@AddPlaceActivity, EditTripActivity::class.java)
                                intentToEditTripActivity.putExtra("TRIP_ID", tripId)
                                startActivity(intentToEditTripActivity)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}
