package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTripActivity : ComponentActivity() {

    private lateinit var repository: TripRepository
    private var createdTripId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)

        setContent {
            TravelPlanningTheme {
                Surface {
                    TripEditScreen(
                        trip = null,
                        tripId = createdTripId,
                        repository = repository,
                        onBackClick = { finish() },
                        onSaveTrip = { newTrip ->
                            lifecycleScope.launch {
                                saveTrip(newTrip.title, newTrip.date, newTrip.notes)
                                val intentToMainActivity = Intent(this@AddTripActivity, MainActivity::class.java)
                                startActivity(intentToMainActivity)
                                finish()
                            }
                        },
                        onAddPlaceClick = { newTrip ->
                            lifecycleScope.launch {
                                val tripId = createdTripId ?: saveTrip(newTrip.title, newTrip.date, newTrip.notes)
                                navigateToAddPlace(tripId)
                            }
                        },
                        onRemovePlaceClick = { currentTripId, placeId ->
                            lifecycleScope.launch {
                                repository.removePlaceFromTrip(currentTripId, placeId)
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun saveTrip(name: String, date: String, notes: String): Long {
        return withContext(Dispatchers.IO) {
            val tripId = repository.createTrip(name, date, notes)
            createdTripId = tripId
            tripId
        }
    }

    private fun navigateToAddPlace(tripId: Long) {
        val intentToAddPlaceActivity = Intent(this, AddPlaceActivity::class.java)
        intentToAddPlaceActivity.putExtra("TRIP_ID", tripId)
        startActivity(intentToAddPlaceActivity)
    }
}
