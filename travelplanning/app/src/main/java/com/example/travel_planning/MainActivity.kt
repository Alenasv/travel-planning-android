package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.network.downloadJson
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.MainScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.DeleteConfirmationDialog
import com.example.travel_planning.view_model.MainViewModel
import com.example.travel_planning.view_model.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private lateinit var viewModel: MainViewModel
    private var onSelectionReset: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(repository)
        )[MainViewModel::class.java]

        setContent {
            TravelPlanningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
                    val trips by viewModel.trips.collectAsStateWithLifecycle()

                    val allPlaces by viewModel.allPlaces.collectAsStateWithLifecycle()
                    val isLoadingState by viewModel.isLoadingState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.loadInitialData(applicationContext)
                    }
                    val handleDelete: (List<Long>) -> Unit = { ids ->
                        viewModel.requestDeleteTrips(ids)
                    }

                    val onResetSelection: () -> Unit = {
                        onSelectionReset?.invoke()
                    }

                    if (showDeleteDialog) {
                        DeleteConfirmationDialog(
                            title = "Удалить выбранные записи?",
                            onConfirm = {
                                viewModel.confirmDelete()
                                onResetSelection()
                            },
                            onDismiss = {
                                viewModel.dismissDeleteDialog()
                                onResetSelection()
                            }
                        )
                    }

                    val handleShare: (Long) -> Unit = { tripId ->
                        lifecycleScope.launch {
                            val text = viewModel.buildShareText(tripId) ?: return@launch

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }

                            startActivity(Intent.createChooser(intent, "Поделиться поездкой"))
                        }
                    }

                    MainScreen(
                        onAddTripClick = {
                            startActivity(Intent(this, AddTripActivity::class.java))
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onGalleryItemClick = { category ->
                            val intent = Intent(this, AddFromTheCategory::class.java)
                            intent.putExtra("CATEGORY", category)
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        onEditTripClick = { tripId ->
                            val intent = Intent(this, EditTripActivity::class.java)
                            intent.putExtra("TRIP_ID", tripId)
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                        },
                        trips = trips,
                        allPlaces = allPlaces,
                        isLoadingState = isLoadingState,
                        onDeleteTrips = handleDelete,
                        onSelectionResetCallback = { callback ->
                            onSelectionReset = callback
                        }, onShareTripClick = handleShare
                    )
                }
            }
        }
    }
}