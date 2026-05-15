package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.network.ApiClient
import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.network.model.RecommendedPlace
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
                    onGenerate = { selectedTags, selectedMetro, topK ->

                        lifecycleScope.launch {
                            val response = ApiClient.api.recommend(
                                RecommendRequest(
                                    user_preferences = selectedTags,
                                    top_k = topK,
                                    start_metro = selectedMetro?.takeIf { it.isNotBlank() }
                                )
                            )

                            SelectedPlacesHolder.places = response.places

                            val intent = Intent(this@AITripActivity, EditTripActivity::class.java)
                            intent.putExtra("MODE", "AI")
                            intent.putExtra("TRIP_ID", 0L)

                            startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}
object SelectedPlacesHolder {
    var places: List<RecommendedPlace> = emptyList()

    fun clear() {
        places = emptyList()
    }
}