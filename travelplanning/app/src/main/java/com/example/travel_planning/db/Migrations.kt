package com.example.travel_planning.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trip_place_cross_ref ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""
            UPDATE trip_place_cross_ref SET position = (
                SELECT COUNT(*) FROM trip_place_cross_ref AS preceding
                WHERE preceding.tripId = trip_place_cross_ref.tripId
                AND preceding.placeId < trip_place_cross_ref.placeId
            )
        """.trimIndent())
    }
}
