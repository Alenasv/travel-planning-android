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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddFromTheCategory : ComponentActivity() {

    private lateinit var repository: TripRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)
        val allPlaces: List<Place> = loadJsonListFromAssets(this, "all_places.json")

        setContent {
            TravelPlanningTheme {
                Surface {
                    var selectedPlaceIds by remember { mutableStateOf(setOf<String>()) }

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val placeId = result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true
                            selectedPlaceIds = if (isSelected) selectedPlaceIds + placeId else selectedPlaceIds - placeId
                        }
                    }

                    AddToRouteScreen(
                        tripId = 0L,
                        selectedPlacesIds = selectedPlaceIds,
                        places = allPlaces,
                        onBackClick = { finish() },
                        onPlaceClick = { place, _ ->
                            val intent = Intent(this@AddFromTheCategory, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra("IS_SELECTED", selectedPlaceIds.contains(place.id))
                            placeDetailLauncher.launch(intent)
                        },
                        onTogglePlace = { placeId, toggled ->
                            selectedPlaceIds = if (toggled) selectedPlaceIds + placeId else selectedPlaceIds - placeId
                        },
                        onSaveTrip = {
                            val intent = Intent(this, EditTripFromCategory::class.java)
                            intent.putStringArrayListExtra("SELECTED_PLACE_IDS", ArrayList(selectedPlaceIds))
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
