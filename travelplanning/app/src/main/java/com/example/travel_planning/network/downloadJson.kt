
package com.example.travel_planning.network

import android.content.Context
import com.example.travel_planning.utils.AtomicFiles
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream


suspend fun downloadPlacesJson(
    api: Api,
    context: Context
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val response = api.getPlaces()

            if (!response.isSuccessful) {
                Log.e("JSON", "Ошибка загрузки: ${response.code()}")
                return@withContext false
            }

            val body = response.body()?.string()
                ?: return@withContext false

            val jsonObject = com.google.gson.JsonParser
                .parseString(body)
                .asJsonObject

            val placesArray = jsonObject.getAsJsonArray("places")
                ?: return@withContext false

            val file = File(context.filesDir, "places.json")
            AtomicFiles.write(file, placesArray.toString())

            Log.d("JSON", "Места сохранены: ${placesArray.size()}")

            true
        } catch (e: Exception) {
            Log.e("JSON", "Ошибка загрузки мест: ${e.message}", e)
            false
        }
    }
}

suspend fun downloadMetroStationsJson(
    api: Api,
    context: Context
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val response = api.getMetroStations()

            if (!response.isSuccessful) {
                Log.e("JSON", "Ошибка загрузки станций: ${response.code()}")
                return@withContext false
            }

            val body = response.body()?.string()
                ?: return@withContext false

            val jsonObject = com.google.gson.JsonParser
                .parseString(body)
                .asJsonObject

            val stationsArray = jsonObject.getAsJsonArray("stations")
                ?: return@withContext false

            val file = File(context.filesDir, "metro_stations.json")
            AtomicFiles.write(file, stationsArray.toString())

            Log.d("JSON", "Станции метро сохранены: ${stationsArray.size()}")

            true
        } catch (e: Exception) {
            Log.e("JSON", "Ошибка загрузки станций метро: ${e.message}", e)
            false
        }
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

            val url = ServerUrls.image(cleanPath)

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
