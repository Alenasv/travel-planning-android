package com.example.travel_planning.network.model

data class RecommendedPlace(
    val id: String,
    val name: String,
    val category: String,
    val metro: String?,
    val address: String?,
    val work_time: String?
)