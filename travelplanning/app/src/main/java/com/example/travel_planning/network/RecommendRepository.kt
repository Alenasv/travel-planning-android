package com.example.travel_planning.network

import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.network.model.RecommendedPlace

class RecommendRepository {

    suspend fun getRecommendations(
        preferences: List<String>
    ): List<RecommendedPlace> {

        return try {

            val response = ApiClient.api.recommend(
                RecommendRequest(
                    user_preferences = preferences,
                    top_k = 10
                )
            )

            response.places

        } catch (e: Exception) {
            emptyList()
        }
    }
}