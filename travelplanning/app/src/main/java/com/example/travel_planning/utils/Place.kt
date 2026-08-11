package com.example.travel_planning.utils

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val work_time: String,
    val category: String,
    val description: String,
    val image_filename: String,
    val tags: List<String>
)
