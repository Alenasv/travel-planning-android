package com.example.travel_planning.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id_: Long = 0,
    val name: String,
    val date: String?,
    val notes: String?
)