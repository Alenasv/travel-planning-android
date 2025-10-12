package com.example.travel_planning.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader


inline fun <reified T> loadJsonListFromAssets(context: Context, fileName: String): List<T> {
    return try {
        val inputStream = context.assets.open(fileName)
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<List<T>>() {}.type
        val data: List<T> = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        data ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}