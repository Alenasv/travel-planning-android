package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.travel_planning.utils.DeleteConfirmationDialog
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.loadJsonListFromInternal
import com.example.travel_planning.view_model.TripEditViewModel
import com.example.travel_planning.view_model.TripEditViewModelFactory
import kotlinx.coroutines.launch

class EditTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private lateinit var viewModel: TripEditViewModel
    private var tripId: Long = -1

    private val showDeleteDialog = mutableStateOf(false)
    private val showUnsavedDialog = mutableStateOf(false)

    private val addPlaceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedIds = result.data?.getStringArrayListExtra("SELECTED_PLACE_IDS")
                ?: return@registerForActivityResult

            val allPlaces: List<Place> =
                loadJsonListFromInternal(this, "all_places.json")
            val updatedPlaces = allPlaces.filter { it.id in selectedIds }
            viewModel.setPlaces(updatedPlaces)
        }
    }

    private val placeDetailLauncher = registerForActivityResult(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra("MODE") ?: "EDIT"
        tripId = intent.getLongExtra("TRIP_ID", -1L)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        viewModel = ViewModelProvider(
            this,
            TripEditViewModelFactory(repository)
        )[TripEditViewModel::class.java]

        lifecycleScope.launch {

            if (mode == "AI") {

                val aiPlaces = SelectedPlacesHolder.places
                SelectedPlacesHolder.clear()

                val converted = aiPlaces.map {
                    Place(
                        id = it.id,
                        name = it.name,
                        address = it.address ?: "",
                        work_time = it.work_time ?: "",
                        category = it.category,
                        description = "",
                        image_filename = "",
                        tags = emptyList()
                    )
                }

                viewModel.initCreateMode()
                viewModel.setPlaces(converted)

            } else {

                viewModel.initEditMode(tripId)
            }
        }

        setContent {
            TravelPlanningTheme {
                Surface {
                    val trip by viewModel.tripState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                    }

                    TripEditScreen(
                        trip = trip,
                        isCreateMode = false,
                        showGeneratedTitle = false,
                        defaultTitle = "",
                        onTitleChange = { viewModel.updateTripTitle(it) },
                        onDateChange = { viewModel.updateTripDate(it) },
                        onNotesChange = { viewModel.updateTripNotes(it) },
                        onBackClick = {
                            if (viewModel.hasChanges()) {
                                showUnsavedDialog.value = true
                            } else {
                                intentToMainActivity()
                            }
                        },
                        onSaveTrip = {
                            lifecycleScope.launch {
                                if (mode == "AI") {
                                    viewModel.saveCreate()
                                } else {
                                    viewModel.saveEdit()
                                }
                                intentToMainActivity()
                            }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(
                                this@EditTripActivity,
                                AddPlaceActivity::class.java
                            ).apply {
                                putExtra("TRIP_ID", tripId)
                                putStringArrayListExtra(
                                    "SELECTED_PLACE_IDS",
                                    ArrayList(trip.places.map { it.id })
                                )
                            }
                            addPlaceLauncher.launch(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onRemovePlaceClick = { placeId ->
                            viewModel.removePlace(placeId)
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(
                                this@EditTripActivity,
                                PlaceDetailActivity::class.java
                            ).apply {
                                putExtra("PLACE_ID", place.id)
                                putExtra("IS_SELECTED", true)
                                putExtra("MODE", "SELECTION")
                            }
                            placeDetailLauncher.launch(intent)
                        },
                        deleteTrip = {
                            showDeleteDialog.value = true
                        }
                    )

                    if (showDeleteDialog.value) {
                        DeleteConfirmationDialog(
                            title = "Вы уверены, что хотите удалить?",
                            onConfirm = {
                                lifecycleScope.launch {
                                    repository.deleteTripById(tripId)
                                    showDeleteDialog.value = false
                                    intentToMainActivity()
                                }
                            },
                            onDismiss = {
                                showDeleteDialog.value = false
                            }
                        )
                    }

                    if (showUnsavedDialog.value) {
                        UnsavedTripDialog(
                            onSave = {
                                showUnsavedDialog.value = false
                                lifecycleScope.launch {
                                    viewModel.saveEdit()
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

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
        finish()
    }
}