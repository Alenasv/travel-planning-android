package com.example.travel_planning

import AddToRouteScreen
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.travel_planning.network.ApiClient
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.loadJsonListFromInternal
import kotlinx.coroutines.launch

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
                    var isOnline by remember { mutableStateOf(true) }

                    val coroutineScope = rememberCoroutineScope()
                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK) {
                            val placeId = result.data?.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true
                            selectedPlaceIds = if (isSelected) selectedPlaceIds + placeId else selectedPlaceIds - placeId
                        }
                    }
                    LaunchedEffect(Unit) {
                        isOnline = try {
                            ApiClient.api.ping()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    AddToRouteScreen(
                        selectedPlacesIds = selectedPlaceIds,
                        places = allPlaces,
                        isOnline = isOnline,
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
                        },
                        onAIClick = {

                            coroutineScope.launch {

                                try {

                                    ApiClient.api.ping()

                                    isOnline = true

                                    startActivity(
                                        Intent(
                                            this@AddFromTheCategory,
                                            AITripActivity::class.java
                                        )
                                    )

                                } catch (e: Exception) {

                                    isOnline = false

                                    Toast.makeText(
                                        this@AddFromTheCategory,
                                        "Сервер недоступен",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },

                                selectedCategory = selectedCategory,
                        onCategorySelected = { category -> selectedCategory = category }
                    )
                }
            }
        }
    }
}
