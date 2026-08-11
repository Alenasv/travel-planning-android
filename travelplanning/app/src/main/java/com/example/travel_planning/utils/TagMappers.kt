package com.example.travel_planning.utils

fun String?.toTags(): List<String> {
    return this
        ?.removeSurrounding("[", "]")
        ?.split(",")
        ?.map { it.trim().replace("\"", "") }
        ?: emptyList()
}