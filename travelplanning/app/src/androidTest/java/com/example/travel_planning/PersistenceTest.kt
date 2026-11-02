package com.example.travel_planning

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.db.MIGRATION_2_3
import com.example.travel_planning.db.entities.PlaceEntity
import com.example.travel_planning.repository.DraftStore
import com.example.travel_planning.repository.TripDraft
import com.example.travel_planning.repository.TripRepository
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.AtomicFiles
import com.example.travel_planning.utils.Place
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun place(id: String) = PlaceEntity(place_id = id, name = id, category = "Test",
        address = "", work_time = null, description = null, imageFilename = null, tags = null)

    @Test
    fun sharedPlaceSurvivesDeletionAndOrderSurvivesReopen() = runBlocking {
        val name = "test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            var repo = TripRepository(db, db.tripDao(), db.placeDao())
            val first = repo.saveTrip(0, "First", "", "", listOf(place("b"), place("a")))
            val second = repo.saveTrip(0, "Second", "", "", listOf(place("a")))
            repo.deleteTripById(second)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            repo = TripRepository(db, db.tripDao(), db.placeDao())
            assertEquals(listOf("b", "a"), repo.getTripForUI(first)!!.places.map { it.id })
            assertNotNull(repo.getPlaceForUI("a"))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun failedSaveRollsBackTripAndPlaces() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = TripRepository(db, db.tripDao(), db.placeDao())
            val existing = repo.saveTrip(0, "Original", "", "", listOf(place("old")))
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER reject_place BEFORE INSERT ON place
                WHEN NEW.place_id = 'broken' BEGIN SELECT RAISE(ABORT, 'test failure'); END
            """.trimIndent())
            for (id in listOf(0L, existing)) {
                var failed = false
                try {
                    repo.saveTrip(id, "Changed", "", "", listOf(place("new"), place("broken")))
                } catch (e: Exception) {
                    failed = true
                }
                assertTrue(failed)
            }
            assertEquals("Original", repo.getTripForUI(existing)!!.title)
            assertEquals(listOf("old"), repo.getTripForUI(existing)!!.places.map { it.id })
            assertNull(repo.getPlaceForUI("new"))
            assertEquals(1, db.tripDao().getTripWithPlaces(existing)!!.places.size)
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM trip").use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        } finally { db.close() }
    }

    @Test
    fun migrationPreservesVersionTwoData() = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE trip (id_ INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, date TEXT, notes TEXT)")
                        db.execSQL("CREATE TABLE place (id_ INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, place_id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, address TEXT NOT NULL, work_time TEXT, description TEXT, imageFilename TEXT, tags TEXT)")
                        db.execSQL("CREATE TABLE trip_place_cross_ref (tripId INTEGER NOT NULL, placeId INTEGER NOT NULL, PRIMARY KEY(tripId, placeId))")
                        db.execSQL("INSERT INTO trip VALUES (1, 'Saved', NULL, NULL)")
                        db.execSQL("INSERT INTO place VALUES (1, 'a', 'A', 'Test', '', NULL, NULL, NULL, NULL)")
                        db.execSQL("INSERT INTO place VALUES (2, 'b', 'B', 'Test', '', NULL, NULL, NULL, NULL)")
                        db.execSQL("INSERT INTO trip_place_cross_ref VALUES (1, 2), (1, 1)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        helper.writableDatabase
        helper.close()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_2_3).build()
        try {
            val repo = TripRepository(db, db.tripDao(), db.placeDao())
            assertEquals("Saved", repo.getTripForUI(1)!!.title)
            assertEquals(listOf("a", "b"), repo.getTripForUI(1)!!.places.map { it.id })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun draftCanBeReadByNewStoreWithoutCatalog() {
        val store = DraftStore(context)
        val id = store.newId()
        val place = Place("outside", "Outside catalog", "", null, "Test", null, null, emptyList())
        val draft = TripDraft(Trip(id = 0, title = "Draft", date = "Date", notes = "Notes", places = listOf(place)))
        try {
            store.write(id, draft)
            assertEquals(draft, DraftStore(context).read(id))
        } finally {
            File(context.filesDir, "drafts/$id.json").delete()
        }
    }

    @Test
    fun atomicReadRecoversPreviousFileAfterInterruptedWrite() {
        val file = File(context.cacheDir, "atomic-${UUID.randomUUID()}.json")
        try {
            AtomicFiles.write(file, "[1]")
            assertTrue(file.renameTo(File(file.path + ".bak")))
            file.writeText("[")
            assertEquals("[1]", AtomicFiles.read(file))
        } finally {
            file.delete()
            File(file.path + ".bak").delete()
        }
    }
}
