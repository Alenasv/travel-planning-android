package com.example.travel_planning.utils

data class PlaceDto(
    val id: String,
    val name: String,
    val category: String?,
    val address: String?,
    val work_time: String?,
    val metro: String?
)