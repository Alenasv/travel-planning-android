package com.example.travel_planning.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id_: Long = 0,
    val place_id: String,
    val name: String,
    val category: String,
    val address: String,
    val work_time: String?,
    val description: String?,
    val imageFilename: String?
)
