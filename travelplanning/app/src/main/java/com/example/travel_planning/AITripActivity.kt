package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.network.ApiClient
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.ui.AITripScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.launch

class AITripActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TravelPlanningTheme {
                AITripScreen(
                    api = ApiClient.api,
                    onBack = { finish() },
                    onGenerate = { selectedTags, selectedMetro ->

                        lifecycleScope.launch {
                            try {
                                val response = ApiClient.api.recommend(
                                    RecommendRequest(
                                        user_preferences = selectedTags,

                                        top_k = 10,
                                        start_metro = selectedMetro
                                    )
                                )


                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            }
        }
    }
}