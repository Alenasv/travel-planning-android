package com.example.travel_planning

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.MainScreen
import com.example.travel_planning.ui.theme.TravelPlanningTheme


class MainActivity : ComponentActivity() {

    private lateinit var repository: TripRepository

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
                    MainScreen(
                        onAddTripClick = {
                            startActivity(Intent(this, AddTripActivity::class.java))
                        },
                        onEditTripClick = { tripId ->
                            val intent = Intent(this, EditTripActivity::class.java)
                            intent.putExtra("TRIP_ID", tripId)
                            startActivity(intent)
                        },
                        repository = repository
                    )
                }
            }
        }
    }
}