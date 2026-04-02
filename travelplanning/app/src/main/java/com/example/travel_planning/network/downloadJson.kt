package com.example.travel_planning.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import retrofit2.awaitResponse
import okhttp3.Request
import java.io.FileOutputStream

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

suspend fun downloadImage(context: Context, relativePath: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val cleanPath = relativePath.trim().removePrefix("/").let {
                if (it.startsWith("data/")) it.removePrefix("data/") else it
            }

            val file = File(context.filesDir, cleanPath)
            if (file.exists()) return@withContext file

            val url = "http://45.150.11.208:8000/static/$cleanPath"

            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val bytes = response.body?.bytes() ?: return@withContext null

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(bytes) }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}