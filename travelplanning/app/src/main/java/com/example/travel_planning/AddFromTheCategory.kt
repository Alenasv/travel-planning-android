package com.example.travel_planning

import AddToRouteScreen
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromInternal
import com.example.travel_planning.viewmodel.AddPlaceViewModel

class AddFromTheCategory : ComponentActivity() {

    private val viewModel: AddPlaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialCategory = intent.getStringExtra("CATEGORY") ?: "Все"

        val allPlaces: List<Place> = loadJsonListFromInternal(this, "all_places.json")

        if (savedInstanceState == null) {
            viewModel.initPlaces(allPlaces)
            viewModel.setInitialCategory(initialCategory)
            viewModel.checkConnection()
        }

        setContent {
            TravelPlanningTheme {
                Surface {
                    val selectedPlaceIds by viewModel.selectedPlaceIds.collectAsStateWithLifecycle()
                    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val filteredPlaces by viewModel.filteredPlaces.collectAsStateWithLifecycle()
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val placeId = result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true
                            viewModel.togglePlace(placeId, isSelected)
                        }
                    }

                    AddToRouteScreen(
                        selectedPlacesIds = selectedPlaceIds,
                        places = allPlaces,
                        filteredPlaces = filteredPlaces,
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { viewModel.changeSearchQuery(it) },
                        isOnline = isOnline,
                        onBackClick = { finish() },
                        onPlaceClick = { place, _ ->
                            val intent = Intent(this@AddFromTheCategory, PlaceDetailActivity::class.java).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", selectedPlaceIds.contains(place.id))
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        onTogglePlace = { placeId, toggled ->
                            viewModel.togglePlace(placeId, toggled)
                        },
                        onSaveTrip = { selectedPlaces ->
                            if (selectedPlaces.isEmpty()) {
                                finish()
                            } else {
                                val intent = Intent(this, EditTripFromCategory::class.java).apply {
                                    putStringArrayListExtra(
                                        "SELECTED_PLACE_IDS",
                                        ArrayList(selectedPlaces.map { it.id })
                                    )
                                }
                                startActivity(intent)
                                finish()
                            }
                        },
                        onAIClick = {
                            if (isOnline) {
                                startActivity(Intent(this@AddFromTheCategory, AITripActivity::class.java))
                            } else {
                                Toast.makeText(this@AddFromTheCategory, "Сервер недоступен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category -> viewModel.selectCategory(category) }
                    )
                }
            }
        }
    }
}