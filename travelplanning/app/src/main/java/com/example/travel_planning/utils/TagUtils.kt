package com.example.travel_planning.utils

private val badTags = setOf(
    "интересные места",
    "интересное",
    "прочее",
    "новое на сайте",
    "это лето",
    "лучшее в мае"
)

fun normalizeTags(places: List<Place>): List<String> {
    return places
        .flatMap { it.tags }
        .map { it.lowercase().trim() }
        .filter { it.isNotBlank() }
        .filter { it !in badTags }
}

fun tagFrequency(tags: List<String>): Map<String, Int> {
    return tags.groupingBy { it }.eachCount()
}
fun buildAutoClusters(tags: List<String>): List<TagCluster> {

    val keywords = mapOf(
        "С кем" to listOf("друзья", "семь", "семьёй", "девуш", "парн", "дет"),
        "Еда и рестораны" to listOf("еда", "кафе", "ресторан", "бар", "гастроном"),
        "Культура" to listOf("муз", "театр", "галер", "искусств"),
        "Активности" to listOf("спорт", "актив", "скалолаз", "прогул"),
        "Развлечения" to listOf("развлеч", "кино", "шоу", "аттрак"),
        "Природа" to listOf("парк", "природ", "загород", "лес"),
        "Эмоции" to listOf("селфи", "вид", "красив")
    )

    val result = mutableMapOf<String, MutableList<String>>()

    for (tag in tags.distinct()) {

        val group = keywords.entries.firstOrNull { entry ->
            entry.value.any { key ->
                tag.contains(key, ignoreCase = true)
            }
        }?.key ?: "Другое"

        result.getOrPut(group) { mutableListOf() }.add(tag)
    }

    return result.map { TagCluster(it.key, it.value.sorted()) }
}