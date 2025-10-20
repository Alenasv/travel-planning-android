package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.TripEditScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import kotlinx.coroutines.launch

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

                                val result = repository.createTrip(
                                    name = newTrip.title,
                                    date = newTrip.date,
                                    notes = newTrip.notes
                                )

                                if (result > 0) {
                                    runOnUiThread {
                                        finish()
                                    }
                                } else {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@AddTripActivity,
                                            "Ошибка сохранения Result: $result",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        onAddPlaceClick = { newTrip ->
                            lifecycleScope.launch {
                                val tripId = createdTripId ?: repository.createTrip(
                                    name = newTrip.title,
                                    date = newTrip.date,
                                    notes = newTrip.notes
                                )

                                if (tripId > 0) {
                                    createdTripId = tripId
                                    runOnUiThread {
                                        val intent = Intent(this@AddTripActivity, AddPlaceActivity::class.java)
                                        intent.putExtra("TRIP_ID", tripId)
                                        startActivity(intent)
                                    }
                                } else {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@AddTripActivity,
                                            "Ошибка при создании поездки перед добавлением мест",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

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
}