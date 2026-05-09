package com.example.travel_planning

import AddToRouteScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.loadJsonListFromInternal

class AddFromTheCategory : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val allPlaces: List<Place> = loadJsonListFromInternal(this, "all_places.json")

        val initialCategory = intent.getStringExtra("CATEGORY") ?: "Все"

        setContent {
            TravelPlanningTheme {
                Surface {
                    var selectedPlaceIds by remember { mutableStateOf(setOf<String>()) }
                    var selectedCategory by remember { mutableStateOf(initialCategory) }

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
                        onSaveTrip = { selectedPlaces ->

                            if (selectedPlaces.isEmpty()) {
                                finish()
                            } else {
                                val intent = Intent(this, EditTripFromCategory::class.java)
                                intent.putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(selectedPlaces.map { it.id })
                                )
                                startActivity(intent)
                                finish()
                            }
                        },onAIClick = {
                            val intent = Intent(
                                this@AddFromTheCategory,
                                AITripActivity::class.java
                            )
                            startActivity(intent)
                        },

                                selectedCategory = selectedCategory,
                        onCategorySelected = { category -> selectedCategory = category }
                    )
                }
            }
        }
    }
}
