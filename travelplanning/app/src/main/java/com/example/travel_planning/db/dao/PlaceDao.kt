package com.example.travel_planning.db.dao

import androidx.room.*
import com.example.travel_planning.db.entities.PlaceEntity

@Dao
interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: PlaceEntity): Long

    @Query("SELECT * FROM place WHERE place_id = :placeId LIMIT 1")
    suspend fun getByPlaceId(placeId: String): PlaceEntity?

    @Query("SELECT * FROM place")
    suspend fun getAll(): List<PlaceEntity>
}