package com.example.travel_planning

import AddToRouteScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.travel_planning.ui.theme.TravelPlanningTheme

class AddPlaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelPlanningTheme {
                Surface {
                    AddToRouteScreen(
                        onBackClick = { finish() },
                        onSaveTrip = { newTrip ->
                            finish()
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}