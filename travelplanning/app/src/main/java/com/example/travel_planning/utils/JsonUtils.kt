package com.example.travel_planning.utils

import android.content.Context
import com.google.gson.JsonParser
import java.io.File

fun loadJsonListFromInternal(context: Context, filename: String): List<Place> {
    return try {
        val file = File(context.filesDir, filename)
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        JsonParser.parseString(AtomicFiles.read(file)).asJsonArray.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val item = element.asJsonObject
            fun text(key: String): String? =
                item.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            val id = text("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = text("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Place(
                id = id,
                name = name,
                address = text("address").orEmpty(),
                work_time = text("work_time"),
                category = text("category")?.takeIf { it.isNotBlank() } ?: "Без категории",
                description = text("description"),
                image_filename = text("image_filename"),
                tags = item.get("tags")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { tag ->
                        tag.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                    }.orEmpty()
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
