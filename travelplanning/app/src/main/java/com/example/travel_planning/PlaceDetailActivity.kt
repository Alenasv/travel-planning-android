package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets

class PlaceDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) finish()

        val placeId = intent.getStringExtra("PLACE_ID")
        val places: List<Place> = loadJsonListFromAssets(this, "all_places.json")
        val currentPlace = places.find { it.id == placeId }

        val isSelected = intent.getBooleanExtra("IS_SELECTED", false)

        setContent {
            TravelPlanningTheme {
                Surface {
                    PlaceDetailScreen(
                        place = currentPlace,
                        isSelected = isSelected,
                        onBackClick = { finish() },
                        onAddToRoute = {
                            val resultIntent = Intent().apply {
                                putExtra("PLACE_ID", placeId)
                                putExtra("IS_SELECTED", !isSelected)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }


                    )
                }
            }
        }
    }
}
