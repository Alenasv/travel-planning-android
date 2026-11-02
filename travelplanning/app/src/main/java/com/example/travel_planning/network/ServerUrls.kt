package com.example.travel_planning.network

import com.example.travel_planning.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl

object ServerUrls {
    fun image(path: String): String {
        val relativePath = path.trim().trimStart('/').removePrefix("data/").removePrefix("static/")
        require(relativePath.isNotBlank() && relativePath.split('/').none { it == ".." })
        return BuildConfig.API_BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("static")
            .addPathSegments(relativePath)
            .build()
            .toString()
    }
}
