package com.example.travel_planning.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.ui.AITripScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.view_model.AITripViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AITripActivity : ComponentActivity() {
    private val viewModel: AITripViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.pendingDraft.collect { draftId ->
                if (draftId != null) {
                    val intent = Intent(this@AITripActivity, EditTripActivity::class.java).apply {
                        putExtra("MODE", "AI")
                        putExtra("TRIP_ID", 0L)
                        putExtra("DRAFT_ID", draftId)
                    }
                    startActivity(intent)
                    viewModel.navigationHandled()
                }
            }
        }

        setContent {
            TravelPlanningTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
                val generationError by viewModel.generationError.collectAsStateWithLifecycle()

                AITripScreen(
                    uiState = uiState,
                    isGenerating = isGenerating,
                    generationError = generationError,
                    onRetry = viewModel::loadClusters,
                    onBack = { finish() },
                    onGenerate = { selectedTags, selectedMetro, topK ->
                        viewModel.generateRoute(selectedTags, selectedMetro, topK)
                    }
                )
            }
        }
    }
}