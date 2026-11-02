package com.example.travel_planning.utils

import android.util.AtomicFile
import java.io.File

object AtomicFiles {
    @Synchronized
    fun write(file: File, text: String) {
        file.parentFile?.mkdirs()
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    @Synchronized
    fun read(file: File): String = AtomicFile(file).openRead().bufferedReader().use { it.readText() }
}
