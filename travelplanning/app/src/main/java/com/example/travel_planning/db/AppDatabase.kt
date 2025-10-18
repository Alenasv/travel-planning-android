package com.example.travel_planning.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.travel_planning.db.dao.PlaceDao
import com.example.travel_planning.db.dao.TripDao
import com.example.travel_planning.db.entities.PlaceEntity
import com.example.travel_planning.db.entities.TripEntity
import com.example.travel_planning.db.entities.TripPlaceCrossRef

@Database(
    entities = [PlaceEntity::class, TripEntity::class, TripPlaceCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "TravelPlanning.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}