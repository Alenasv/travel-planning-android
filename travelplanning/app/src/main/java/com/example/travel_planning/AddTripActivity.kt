package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme

class AddTripActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelPlanningTheme {
                Surface {
                    TripEditScreen(
                        trip = null,
                        onBackClick = { finish() },
                        onSaveTrip = { newTrip ->
                            finish()
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this, AddPlaceActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}