package com.example.travel_planning.network

import Api
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://45.150.11.208:8000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: Api = retrofit.create(Api::class.java)
}