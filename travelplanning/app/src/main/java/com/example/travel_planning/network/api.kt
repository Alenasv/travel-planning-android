
package com.example.travel_planning.network

import com.example.travel_planning.network.model.RecommendRequest
import com.example.travel_planning.network.model.RecommendResponse
import com.example.travel_planning.utils.ClustersResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Api {

    @GET("places")
    suspend fun getPlaces(): Response<ResponseBody>

    @POST("recommend")
    suspend fun recommend(
        @Body request: RecommendRequest
    ): RecommendResponse

    @GET("clusters")
    suspend fun getClusters(): ClustersResponse

    @GET("ping")
    suspend fun ping(): Response<ResponseBody>

    @GET("metro-stations")
    suspend fun getMetroStations(): Response<ResponseBody>
}
