package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.loadJsonListFromInternal

class PlaceDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val placeId = intent.getStringExtra("PLACE_ID")
        val isSelected = intent.getBooleanExtra("IS_SELECTED", false)
        val mode = intent.getStringExtra("MODE") ?: "VIEW"

        val places:List<Place> =
            loadJsonListFromInternal(this, "all_places.json")
        val currentPlace = places.find { it.id == placeId }

        setContent {
            TravelPlanningTheme {
                Surface {
                    PlaceDetailScreen(
                        place = currentPlace,
                        isSelected = isSelected,
                        onBackClick = {
                            if (mode == "SELECTION") {
                                setResult(RESULT_CANCELED)
                            }
                            finish()
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onAddToRoute = {
                            val resultIntent = Intent().apply {
                                putExtra("PLACE_ID", placeId)
                                putExtra("IS_SELECTED", !isSelected)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        }
                    )
                }
            }
        }
    }
}