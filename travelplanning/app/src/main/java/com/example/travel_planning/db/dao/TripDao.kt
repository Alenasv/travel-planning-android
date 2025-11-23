package com.example.travel_planning.db.dao

import androidx.room.*
import com.example.travel_planning.db.entities.*

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("SELECT * FROM trip")
    suspend fun getAllTrips(): List<TripEntity>

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Transaction
    @Query("SELECT * FROM trip WHERE id_ = :tripId")
    suspend fun getTripWithPlaces(tripId: Long): TripWithPlaces?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: TripPlaceCrossRef)

    @Query("DELETE FROM trip_place_cross_ref WHERE tripId = :tripId AND placeId = :placeId")
    suspend fun deleteCrossRef(tripId: Long, placeId: Long)
}