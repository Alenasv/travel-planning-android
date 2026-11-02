package com.example.travel_planning.repository

import android.content.Context
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.AtomicFiles
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TripDraft(val trip: Trip, val original: Trip? = null, val defaultTitle: String = "")

@Singleton
class DraftStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson = Gson()
    fun newId(): String = UUID.randomUUID().toString()
    private fun file(id: String): File {
        require(id.matches(Regex("[a-zA-Z0-9-]+")))
        return File(context.filesDir, "drafts/$id.json")
    }
    fun write(id: String, draft: TripDraft) = AtomicFiles.write(file(id), gson.toJson(draft))
    fun read(id: String): TripDraft? {
        val file = file(id)
        return if (file.exists() || File(file.path + ".bak").exists()) {
            gson.fromJson(AtomicFiles.read(file), TripDraft::class.java)
        } else null
    }
}
