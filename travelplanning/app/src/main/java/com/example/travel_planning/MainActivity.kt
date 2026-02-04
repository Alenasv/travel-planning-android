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
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.MainScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.DeleteConfirmationDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var onSelectionReset: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            TravelPlanningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var trips by remember { mutableStateOf(listOf<com.example.travel_planning.db.entities.TripEntity>()) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var pendingDeleteTrips by remember { mutableStateOf(listOf<Long>()) }

                    LaunchedEffect(Unit) {
                        trips = repository.getAllTrips()
                    }

                    val handleDelete: (List<Long>) -> Unit = { ids ->
                        pendingDeleteTrips = ids
                        showDeleteDialog = true
                    }

                    val onResetSelection: () -> Unit = {
                        onSelectionReset?.invoke()
                    }

                    if (showDeleteDialog) {
                        DeleteConfirmationDialog(
                            title = "Удалить выбранные записи?",
                            onConfirm = {
                                showDeleteDialog = false
                                lifecycleScope.launch {
                                    pendingDeleteTrips.forEach { id ->
                                        repository.deleteTripById(id)
                                    }
                                    trips = repository.getAllTrips()
                                    pendingDeleteTrips = emptyList()
                                    onResetSelection()
                                }
                            },
                            onDismiss = {
                                showDeleteDialog = false
                                pendingDeleteTrips = emptyList()
                                onResetSelection()
                            }
                        )
                    }

                    MainScreen(
                        onAddTripClick = {
                            startActivity(Intent(this, AddTripActivity::class.java))
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            finish()
                        },
                        onGalleryItemClick = { category ->
                            val intent = Intent(this, AddFromTheCategory::class.java)
                            intent.putExtra("CATEGORY", category)
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            finish()
                        },
                        onEditTripClick = { tripId ->
                            val intent = Intent(this, EditTripActivity::class.java)
                            intent.putExtra("TRIP_ID", tripId)
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast)
                            finish()
                        },
                        repository = repository,
                        tripsOverride = trips,
                        onDeleteTrips = handleDelete,
                        onSelectionResetCallback = { callback ->
                            onSelectionReset = callback
                        }
                    )
                }
            }
        }
    }
}