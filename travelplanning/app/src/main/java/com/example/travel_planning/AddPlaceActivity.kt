package com.example.travel_planning

import AddToRouteScreen
import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets

class AddPlaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val places: List<Place> = loadJsonListFromAssets(this, "all_places.json")

        setContent {
            TravelPlanningTheme {
                Surface {
                    AddToRouteScreen(
                        places = places,
                        onBackClick = { finish() },
                        onSaveTrip = { newTrip -> finish() },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java)
                            intent.putExtra("PLACE_ID", place.id)
                            startActivity(intent)
                        }
                    )
                }
            }
        }

    }
}