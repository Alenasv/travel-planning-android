package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromInternal
import com.example.travel_planning.view_model.TripEditViewModel
import com.example.travel_planning.view_model.TripEditViewModelFactory
import kotlinx.coroutines.launch

class EditTripFromCategory : ComponentActivity() {

    private lateinit var repository: TripRepository
    private lateinit var viewModel: TripEditViewModel
    private var selectedPlaceIds = listOf<String>()
    private val showUnsavedDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedPlaceIds = intent.getStringArrayListExtra("SELECTED_PLACE_IDS") ?: emptyList()
        repository = TripRepository(AppDatabase.getDatabase(applicationContext))

        viewModel = ViewModelProvider(
            this,
            TripEditViewModelFactory(repository)
        )[TripEditViewModel::class.java]

        viewModel.handleSelectedPlaces(this, selectedPlaceIds)

        setContent {
            TravelPlanningTheme {
                Surface {
                    val trip by viewModel.tripState.collectAsStateWithLifecycle()
                    val defaultTitle by viewModel.defaultTitle.collectAsStateWithLifecycle()
                    val highlightTitleError by viewModel.highlightTitleError.collectAsStateWithLifecycle()

                    val placeDetailLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == RESULT_OK && result.data != null) {
                            val placeId = result.data!!.getStringExtra("PLACE_ID") ?: return@rememberLauncherForActivityResult
                            val isSelected = result.data!!.getBooleanExtra("IS_SELECTED", true)

                            viewModel.handlePlaceDetailResult(this@EditTripFromCategory, placeId, isSelected)
                        }
                    }

                    TripEditScreen(
                        trip = trip,
                        isCreateMode = true,
                        defaultTitle = defaultTitle,
                        showGeneratedTitle = false,
                        highlightTitleError = highlightTitleError,
                        onTitleChange = { viewModel.updateTripTitle(it) },
                        onDateChange = { viewModel.updateTripDate(it) },
                        onNotesChange = { viewModel.updateTripNotes(it) },
                        onBackClick = {
                            if (!viewModel.isEmpty() && viewModel.hasChanges()) {
                                showUnsavedDialog.value = true
                            } else {
                                intentToMainActivity()
                            }
                        },
                        onSaveTrip = {
                            if (!viewModel.validateTitle()) {
                                return@TripEditScreen
                            }
                            saveTripAndGoToMain()
                        },
                        onAddPlaceClick = {
                            val intent = Intent(
                                this@EditTripFromCategory,
                                AddPlaceActivity::class.java
                            )
                            intent.putStringArrayListExtra(
                                "SELECTED_PLACE_IDS",
                                ArrayList(trip.places.map { it.id })
                            )
                            startActivity(intent)
                        },
                        onRemovePlaceClick = { placeId ->
                            viewModel.removePlace(placeId)
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(
                                this@EditTripFromCategory,
                                PlaceDetailActivity::class.java
                            )
                            intent.putExtra("PLACE_ID", place.id)
                            intent.putExtra(
                                "IS_SELECTED",
                                trip.places.any { it.id == place.id }
                            )
                            intent.putExtra("MODE", "SELECTION")
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {
                            intentToMainActivity()
                        }
                    )

                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                saveTripAndGoToMain()
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

    private fun saveTripAndGoToMain() {
        lifecycleScope.launch {
            viewModel.saveCreate()
            intentToMainActivity()
        }
    }

    private fun intentToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}