package com.example.travel_planning.network

import android.content.Context
import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import retrofit2.awaitResponse

suspend fun downloadJson(context: Context, filename: String): Boolean {
    return try {
        val response: Response<ResponseBody> = ApiClient.api.getJson(filename).awaitResponse()
        if (response.isSuccessful) {
            val body = response.body()?.string() ?: return false
            val file = File(context.filesDir, filename)
            file.writeText(body)
            Log.d("JSON", "обновлен")
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Log.e("JSON", "Ошибка ${e.message}")
        false
    }
}