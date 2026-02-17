package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.view_model.TripEditViewModel
import com.example.travel_planning.view_model.TripEditViewModelFactory
import kotlinx.coroutines.launch

class AddTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private lateinit var viewModel: TripEditViewModel
    private lateinit var addPlaceLauncher: ActivityResultLauncher<Intent>
    private lateinit var placeDetailLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        viewModel = ViewModelProvider(
            this,
            TripEditViewModelFactory(repository)
        )[TripEditViewModel::class.java]

        setupLaunchers()

        lifecycleScope.launch {
            viewModel.initCreateMode()
        }

        setContent {
            TravelPlanningTheme {
                Surface {
                    val trip by viewModel.tripState.collectAsStateWithLifecycle()
                    val defaultTitle by viewModel.defaultTitle.collectAsStateWithLifecycle()
                    val hasReturnedFromChild by viewModel.hasReturnedFromChild.collectAsStateWithLifecycle()
                    val showUnsavedDialog = remember { mutableStateOf(false) }

                    TripEditScreen(
                        trip = trip,
                        isCreateMode = true,
                        defaultTitle = defaultTitle,
                        showGeneratedTitle = hasReturnedFromChild,
                        onTitleChange = { viewModel.updateTripTitle(it) },
                        onDateChange = { viewModel.updateTripDate(it) },
                        onNotesChange = { viewModel.updateTripNotes(it) },
                        onBackClick = {
                            if (viewModel.hasChanges()) {
                                showUnsavedDialog.value = true
                            } else {
                                finish()
                                overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            }
                        },
                        onSaveTrip = {
                            lifecycleScope.launch {
                                viewModel.saveCreate()
                                intentToMainActivity()
                            }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this@AddTripActivity, AddPlaceActivity::class.java)
                            intent.putStringArrayListExtra(
                                "SELECTED_PLACE_IDS",
                                ArrayList(trip.places.map { it.id })
                            )
                            addPlaceLauncher.launch(intent)
                        },
                        onRemovePlaceClick = { placeId ->
                            viewModel.removePlace(placeId)
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(
                                this@AddTripActivity,
                                PlaceDetailActivity::class.java
                            ).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", true)
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {}
                    )

                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                lifecycleScope.launch {
                                    viewModel.saveCreate()
                                    intentToMainActivity()
                                }
                            },
                            onDelete = {
                                showUnsavedDialog.value = false
                                intentToMainActivity()
                            },
                            onDismiss = {
                                showUnsavedDialog.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupLaunchers() {
        addPlaceLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedIds = result.data?.getStringArrayListExtra("SELECTED_PLACE_IDS")
                    ?: return@registerForActivityResult

                val allPlaces = loadJsonListFromAssets<Place>(this, "all_places.json")
                val updatedPlaces = allPlaces.filter { it.id in updatedIds }

                viewModel.setPlaces(updatedPlaces)
                viewModel.onReturnedFromChild()
            }
        }

        placeDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val placeId = result.data?.getStringExtra("PLACE_ID")
                val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true

                if (placeId != null && !isSelected) {
                    viewModel.removePlace(placeId)
                }
            }
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }
}