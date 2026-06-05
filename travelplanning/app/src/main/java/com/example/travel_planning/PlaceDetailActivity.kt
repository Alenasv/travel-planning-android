package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.viewmodel.PlaceDetailViewModel

class PlaceDetailActivity : ComponentActivity() {

    private val viewModel: PlaceDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val placeId = intent.getStringExtra("PLACE_ID")
        val isSelectedInitial = intent.getBooleanExtra("IS_SELECTED", false)
        val mode = intent.getStringExtra("MODE") ?: "VIEW"

        viewModel.initData(this, placeId, isSelectedInitial)

        setContent {
            TravelPlanningTheme {
                Surface {
                    val currentPlace by viewModel.currentPlace.collectAsStateWithLifecycle()
                    val isSelected by viewModel.isSelected.collectAsStateWithLifecycle()
                    val imageFile by viewModel.imageFile.collectAsStateWithLifecycle()

                    PlaceDetailScreen(
                        place = currentPlace,
                        isSelected = isSelected,
                        imageFile = imageFile,
                        onBackClick = {
                            handleBackAction(placeId, isSelected, mode)
                        },
                        onAddToRouteClick = {
                            viewModel.toggleRouteSelection()
                        }
                    )
                }
            }
        }
    }

    private fun handleBackAction(placeId: String?, isSelected: Boolean, mode: String) {
        if (mode == "SELECTION" && viewModel.isSelectionChanged()) {
            val resultIntent = Intent().apply {
                putExtra("PLACE_ID", placeId)
                putExtra("IS_SELECTED", isSelected)
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
    }
}