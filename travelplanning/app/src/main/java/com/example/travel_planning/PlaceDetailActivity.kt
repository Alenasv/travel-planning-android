package com.example.travel_planning

import Place
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.PlaceDetailScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme
import com.example.travel_planning.utils.loadJsonListFromAssets
import com.example.travel_planning.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceDetailActivity : ComponentActivity() {
    private lateinit var repository: TripRepository
    private var tripId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TripRepository(db)
        super.onCreate(savedInstanceState)

        tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) {
            finish()
            return
        }
        val placeId = intent.getStringExtra("PLACE_ID")
        val places: List<Place> = loadJsonListFromAssets(this, "all_places.json")
        val currentPlace = places.find { it.id == placeId }

        setContent {
            TravelPlanningTheme {
                Surface {
                    PlaceDetailScreen(
                        place = currentPlace,
                        onBackClick = {
                            intentToAddPlaceActivity()
                            finish() },
                        onAddToRoute = { place ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.addPlaceToTrip(tripId, place.toEntity())
                                }
                                withContext(Dispatchers.Main) {
                                    intentToAddPlaceActivity()
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun intentToAddPlaceActivity() {
        val intent = Intent(this@PlaceDetailActivity, AddPlaceActivity::class.java)
        intent.putExtra("TRIP_ID", tripId)
        startActivity(intent)
    }

}