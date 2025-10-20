package com.example.travel_planning.db.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TripWithPlaces(
    @Embedded val trip: TripEntity,
    @Relation(
        parentColumn = "id_",
        entityColumn = "id_",
        associateBy = Junction(
            value = TripPlaceCrossRef::class,
            parentColumn = "tripId",
            entityColumn = "placeId"
        )
    )
    val places: List<PlaceEntity>
)
