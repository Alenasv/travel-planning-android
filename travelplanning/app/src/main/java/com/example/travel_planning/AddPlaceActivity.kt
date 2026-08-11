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

class AddPlaceActivity : ComponentActivity() {

    private val viewModel: AddPlaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initiallySelectedIds =
            intent.getStringArrayListExtra("SELECTED_PLACE_IDS") ?: arrayListOf()

        val allPlaces: List<Place> =
            loadJsonListFromInternal(this, "all_places.json")

        if (savedInstanceState == null) {
            viewModel.initPlaces(allPlaces)
            viewModel.setInitialSelectedPlaces(initiallySelectedIds.toSet())
            viewModel.checkConnection()
        }

        setContent {
            TravelPlanningTheme {
                Surface {
                    val selectedPlacesIds by viewModel.selectedPlaceIds.collectAsStateWithLifecycle()
                    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val filteredPlaces by viewModel.filteredPlaces.collectAsStateWithLifecycle()
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

                    val placeDetailLauncher =
                        rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                val placeId = result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                                val newState = result.data?.getBooleanExtra("IS_SELECTED", false) ?: false
                                viewModel.togglePlace(placeId, newState)
                            }
                        }

                    AddToRouteScreen(
                        selectedPlacesIds = selectedPlacesIds,
                        places = allPlaces,
                        filteredPlaces = filteredPlaces,
                        searchQuery = searchQuery,
                        onSearchQueryChanged = { viewModel.changeSearchQuery(it) },
                        isOnline = isOnline,
                        onBackClick = {
                            val resultIntent = Intent().apply {
                                putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(initiallySelectedIds)
                                )
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onPlaceClick = { place, isSelected ->
                            val intent = Intent(this@AddPlaceActivity, PlaceDetailActivity::class.java).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", isSelected)
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        onTogglePlace = { placeId, toggled ->
                            viewModel.togglePlace(placeId, toggled)
                        },
                        onAIClick = {
                            if (isOnline) {
                                startActivity(Intent(this@AddPlaceActivity, AITripActivity::class.java))
                            } else {
                                Toast.makeText(this@AddPlaceActivity, "Сервер недоступен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) },
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