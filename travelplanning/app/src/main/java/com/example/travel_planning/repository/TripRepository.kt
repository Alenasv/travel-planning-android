package com.example.travel_planning.repository

import com.example.travel_planning.db.AppDatabase
import androidx.room.withTransaction
import com.example.travel_planning.db.dao.PlaceDao
import com.example.travel_planning.db.dao.TripDao
import com.example.travel_planning.db.entities.*
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.toTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val database: AppDatabase,
    private val tripDao: TripDao,
    private val placeDao: PlaceDao
) {

    suspend fun createTrip(name: String, date: String? = null, notes: String? = null): Long {
        val trip = TripEntity(name = name, date = date, notes = notes)
        return tripDao.insertTrip(trip)
    }

    fun getAllTrips(): Flow<List<TripEntity>> {
        return tripDao.getAllTrips()
    }

    suspend fun updateTrip(tripId: Long, name: String, date: String, notes: String): Boolean {
        return try {
            val trip = TripEntity(
                id_ = tripId,
                name = name,
                date = date,
                notes = notes
            )
            tripDao.updateTrip(trip)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveTrip(id: Long, name: String, date: String, notes: String, places: List<PlaceEntity>): Long =
        database.withTransaction {
            val tripId = if (id == 0L) tripDao.insertTrip(TripEntity(name = name, date = date, notes = notes))
                else {
                    tripDao.updateTrip(TripEntity(id_ = id, name = name, date = date, notes = notes))
                    id
                }
            replaceTripPlaces(tripId, places)
            tripId
        }

    suspend fun replaceTripPlaces(tripId: Long, places: List<PlaceEntity>) = database.withTransaction {
        tripDao.deleteAllCrossRefs(tripId)
        places.distinctBy { it.place_id }.forEachIndexed { position, place ->
            addPlaceToTrip(tripId, place, position)
        }
    }

    suspend fun removeAllPlacesFromTrip(tripId: Long) = tripDao.deleteAllCrossRefs(tripId)

    suspend fun deleteTripById(tripId: Long) = database.withTransaction {
        tripDao.getTripWithPlaces(tripId)?.let {
            tripDao.deleteAllCrossRefs(tripId)
            tripDao.deleteTrip(it.trip)
        }
    }

    suspend fun getNextDefaultTripName(): String {
        val trips = getAllTrips().first()
        val regex = Regex("""^Новая поездка (\d+)$""")
        val maxNumber = trips.mapNotNull { trip ->
            regex.find(trip.name)?.groupValues?.get(1)?.toInt()
        }.maxOrNull() ?: 0
        return "Новая поездка ${maxNumber + 1}"
    }

    suspend fun getTripWithPlaces(tripId: Long) = database.withTransaction {
        tripDao.getTripWithPlaces(tripId)?.let { it.copy(places = tripDao.getOrderedPlaces(tripId)) }
    }

    suspend fun addPlaceToTrip(tripId: Long, place: PlaceEntity, position: Int) {
        val existingPlace = placeDao.getByPlaceId(place.place_id)
        val placeEntity = if (existingPlace != null) {
            existingPlace
        } else {
            val newId = placeDao.insert(place)
            place.copy(id_ = newId)
        }

        tripDao.insertCrossRef(
            TripPlaceCrossRef(tripId = tripId, placeId = placeEntity.id_, position = position)
        )
    }

    suspend fun getPlaceForUI(id: String): Place? = placeDao.getByPlaceId(id)?.let {
        Place(it.place_id, it.name, it.address, it.work_time, it.category,
            it.description, it.imageFilename, it.tags.toTags())
    }

    suspend fun getTripForUI(tripId: Long): Trip? {
        return getTripWithPlaces(tripId)?.let { tripWithPlaces ->
            Trip(
                id = tripWithPlaces.trip.id_,
                title = tripWithPlaces.trip.name,
                date = tripWithPlaces.trip.date ?: "",
                notes = tripWithPlaces.trip.notes ?: "",
                places = tripWithPlaces.places.map { placeEntity ->
                    Place(
                        id = placeEntity.place_id,
                        name = placeEntity.name,
                        address = placeEntity.address,
                        work_time = placeEntity.work_time ?: "",
                        category = placeEntity.category,
                        description = placeEntity.description ?: "",
                        image_filename = placeEntity.imageFilename ?: "",
                        tags = placeEntity.tags.toTags()
                    )
                }
            )
        }
    }
}
