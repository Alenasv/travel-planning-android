package com.example.travel_planning.network.model

data class RecommendRequest(
    val user_preferences: List<String>,
    val top_k: Int,
    val start_metro: String? = null
)