package com.example.travel_planning.network

import android.content.Context
import android.util.Log
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
    return try {
        val url = "http://45.150.11.208:8000/$relativePath"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return null

        val bytes = response.body?.bytes() ?: return null
        val file = File(context.filesDir, relativePath)

        file.parentFile?.mkdirs()

        FileOutputStream(file).use { it.write(bytes) }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}