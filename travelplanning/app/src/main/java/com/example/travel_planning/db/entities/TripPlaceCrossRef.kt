package com.example.travel_planning.db.entities

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(
    tableName = "trip_place_cross_ref",
    primaryKeys = ["tripId", "placeId"]
)
data class TripPlaceCrossRef(
    val tripId: Long,
    val placeId: Long,
    @ColumnInfo(defaultValue = "0") val position: Int = 0
)