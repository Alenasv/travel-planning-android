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
    private var isNewTrip = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        isNewTrip = intent.getBooleanExtra("IS_NEW_TRIP", false)

        if (tripId == -1L) {
            finish()
            return
        }

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

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
                            val placeId = result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val newState = result.data?.getBooleanExtra("IS_SELECTED", false) ?: false

                            selectedPlacesIds.value = if (newState) {
                                selectedPlacesIds.value + placeId
                            } else {
                                selectedPlacesIds.value - placeId
                            }
                        }
                    }

                    AddToRouteScreen(
                        tripId = tripId,
                        selectedPlacesIds = selectedPlacesIds.value,
                        places = allPlaces,
                        onBackClick = {
                            if (isNewTrip) {
                                val intent = Intent(this@AddPlaceActivity, EditTripActivity::class.java)
                                intent.putExtra("TRIP_ID", tripId)
                                intent.putExtra("IS_NEW_TRIP", isNewTrip)
                                setResult(RESULT_OK)
                                startActivity(intent)
                            } else {
                                setResult(RESULT_OK)
                            }
                            finish()
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onPlaceClick = { place, isSelected ->
                            val intentToPlaceDetailActivity = Intent(this@AddPlaceActivity, PlaceDetailActivity::class.java)
                            intentToPlaceDetailActivity.putExtra("PLACE_ID", place.id)
                            intentToPlaceDetailActivity.putExtra("TRIP_ID", tripId)
                            intentToPlaceDetailActivity.putExtra("IS_SELECTED", isSelected)
                            placeDetailLauncher.launch(intentToPlaceDetailActivity)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
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

                                setResult(RESULT_OK)

                                if (isNewTrip) {
                                    val intent = Intent(this@AddPlaceActivity, EditTripActivity::class.java)
                                    intent.putExtra("TRIP_ID", tripId)
                                    intent.putExtra("IS_NEW_TRIP", isNewTrip)
                                    startActivity(intent)
                                }

                                finish()
                                overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            }
                        }
                    )
                }
            }
        }
    }
}