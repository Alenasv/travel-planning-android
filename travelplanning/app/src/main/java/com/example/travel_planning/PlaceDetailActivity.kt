package com.example.travel_planning

import Place
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme

class PlaceDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val place = Place(
            id = 1,
            title = "Эрмитаж",
            category = "Музей",
           address= "адрес",
        description= "описание"
        )
        setContent {
            TravelPlanningTheme {
                Surface {
                    PlaceDetailScreen(
                        place = place,
                        onBackClick = { finish() },
                        onAddToRoute = { place ->
                        }
                    )
                }
            }
        }
    }

}