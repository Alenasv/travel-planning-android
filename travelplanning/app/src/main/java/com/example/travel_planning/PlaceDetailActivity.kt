package com.example.travel_planning

import Place
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets

class PlaceDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val placeId = intent.getStringExtra("PLACE_ID")
        val places: List<Place> = loadJsonListFromAssets(this, "all_places.json")
        val currentPlace = places.find { it.id == placeId }

        setContent {
            TravelPlanningTheme {
                Surface {
                    PlaceDetailScreen(
                        place = currentPlace,
                        onBackClick = { finish() },
                        onAddToRoute = { place ->
                        }
                    )
                }
            }
        }
    }

}