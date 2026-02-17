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
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets

class AddPlaceActivity : ComponentActivity() {

    private var selectedCategory: String = "Все"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initiallySelectedIds =
            intent.getStringArrayListExtra("SELECTED_PLACE_IDS") ?: arrayListOf()

        val allPlaces: List<Place> =
            loadJsonListFromAssets(this, "all_places.json")

        setContent {
            TravelPlanningTheme {
                Surface {
                    val selectedPlacesIds = remember { mutableStateOf(initiallySelectedIds.toSet()) }

                    val placeDetailLauncher =
                        rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                val placeId =
                                    result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                                val newState =
                                    result.data?.getBooleanExtra("IS_SELECTED", false) ?: false

                                selectedPlacesIds.value =
                                    if (newState)
                                        selectedPlacesIds.value + placeId
                                    else
                                        selectedPlacesIds.value - placeId
                            }
                        }

                    AddToRouteScreen(
                        selectedPlacesIds = selectedPlacesIds.value,
                        places = allPlaces,
                        onBackClick = {
                            val resultIntent = Intent().apply {
                                putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(selectedPlacesIds.value)
                                )
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onPlaceClick = { place, isSelected ->
                            val intent = Intent(
                                this@AddPlaceActivity,
                                PlaceDetailActivity::class.java
                            ).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", isSelected)
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        onTogglePlace = { placeId, toggled ->
                            selectedPlacesIds.value =
                                if (toggled)
                                    selectedPlacesIds.value + placeId
                                else
                                    selectedPlacesIds.value - placeId
                        },
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        onSaveTrip = { selectedPlaces ->
                            val resultIntent = Intent().apply {
                                putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(selectedPlaces.map { it.id })
                                )
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }
}