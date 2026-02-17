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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.UnsavedTripDialog
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private lateinit var addPlaceLauncher: ActivityResultLauncher<Intent>
    private lateinit var placeDetailLauncher: ActivityResultLauncher<Intent>

    private val tripState = mutableStateOf(
        Trip(
            id = 0,
            title = "",
            date = "",
            notes = "",
            places = emptyList()
        )
    )

    private var isFirstLaunch = true
    private var hasReturnedFromChild = false

    override fun onResume() {
        super.onResume()

        if (!isFirstLaunch) {
            hasReturnedFromChild = true
        }

        isFirstLaunch = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setupLaunchers()

        setContent {
            TravelPlanningTheme {
                Surface {
                    var defaultTitle by rememberSaveable { mutableStateOf("") }
                    val showUnsavedDialog = remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        defaultTitle = repository.getNextDefaultTripName()
                    }

                    TripEditScreen(
                        trip = tripState.value,
                        isCreateMode = true,
                        defaultTitle = defaultTitle,
                        showGeneratedTitle = hasReturnedFromChild,
                        onBackClick = {
                            if (tripState.value.title.isNotBlank() ||
                                tripState.value.places.isNotEmpty() ||
                                tripState.value.date.isNotBlank() ||
                                tripState.value.notes.isNotBlank()) {
                                showUnsavedDialog.value = true
                            } else {
                                finish()
                                overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            }
                        },
                        onSaveTrip = { trip ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val finalTitle = if (trip.title.isBlank()) defaultTitle else trip.title

                                val newTripId = repository.createTrip(
                                    finalTitle,
                                    trip.date,
                                    trip.notes
                                )

                                repository.replaceTripPlaces(
                                    newTripId,
                                    trip.places.map { it.toEntity() }
                                )

                                withContext(Dispatchers.Main) {
                                    intentToMainActivity()
                                }
                            }
                        },
                        onAddPlaceClick = {
                            val intent = Intent(this, AddPlaceActivity::class.java)
                            intent.putStringArrayListExtra(
                                "SELECTED_PLACE_IDS",
                                ArrayList(tripState.value.places.map { it.id })
                            )
                            addPlaceLauncher.launch(intent)
                        },
                        onRemovePlaceClick = { placeId ->
                            tripState.value = tripState.value.copy(
                                places = tripState.value.places.filter { it.id != placeId }
                            )
                        },
                        onPlaceClick = { place ->
                            val intent = Intent(this, PlaceDetailActivity::class.java).apply {
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
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val finalTitle = if (tripState.value.title.isBlank())
                                        defaultTitle
                                    else
                                        tripState.value.title

                                    val newTripId = repository.createTrip(
                                        finalTitle,
                                        tripState.value.date,
                                        tripState.value.notes
                                    )

                                    repository.replaceTripPlaces(
                                        newTripId,
                                        tripState.value.places.map { it.toEntity() }
                                    )

                                    withContext(Dispatchers.Main) {
                                        intentToMainActivity()
                                    }
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

                tripState.value = tripState.value.copy(places = updatedPlaces)
            }
        }

        placeDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val placeId = result.data?.getStringExtra("PLACE_ID")
                val isSelected = result.data?.getBooleanExtra("IS_SELECTED", true) ?: true

                if (placeId != null && !isSelected) {
                    tripState.value = tripState.value.copy(
                        places = tripState.value.places.filter { it.id != placeId }
                    )
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