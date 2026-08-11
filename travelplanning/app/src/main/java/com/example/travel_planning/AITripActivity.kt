package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.network.ApiClient
import com.example.travel_planning.ui.AITripScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.view_model.AITripNavigationEvent
import com.example.travel_planning.view_model.AITripViewModel
import com.example.travel_planning.view_model.AITripViewModelFactory
import kotlinx.coroutines.launch

class AITripActivity : ComponentActivity() {

    private val viewModel: AITripViewModel by viewModels {
        AITripViewModelFactory(ApiClient.api)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is AITripNavigationEvent.NavigateToEditTrip -> {
                        val intent = Intent(this@AITripActivity, EditTripActivity::class.java).apply {
                            putExtra("MODE", "AI")
                            putExtra("TRIP_ID", 0L)
                        }
                        startActivity(intent)
                    }
                }
            }
        }

        setContent {
            TravelPlanningTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

                AITripScreen(
                    uiState = uiState,
                    isGenerating = isGenerating,
                    onBack = { finish() },
                    onGenerate = { selectedTags, selectedMetro, topK ->
                        viewModel.generateRoute(selectedTags, selectedMetro, topK)
                    }
                )
            }
        }
    }
}