package com.example.travel_planning.repository

import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.db.entities.*
import com.example.travel_planning.ui.Trip
import com.example.travel_planning.utils.Place
import com.example.travel_planning.utils.toTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TripRepository(private val db: AppDatabase) {

    private val tripDao = db.tripDao()
    private val placeDao = db.placeDao()

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

    suspend fun replaceTripPlaces(tripId: Long, places: List<PlaceEntity>) {
        removeAllPlacesFromTrip(tripId)
        places.forEach { addPlaceToTrip(tripId, it) }
    }

    suspend fun removeAllPlacesFromTrip(tripId: Long) {
        val tripWithPlaces = getTripWithPlaces(tripId)
        tripWithPlaces?.places?.forEach { placeEntity ->
            tripDao.deleteCrossRef(tripId, placeEntity.id_)
        }
    }

    suspend fun deleteTripById(tripId: Long) {
        val tripWithPlaces = getTripWithPlaces(tripId)
        tripWithPlaces?.let {
            it.places.forEach { placeEntity ->
                tripDao.deleteCrossRef(tripId, placeEntity.id_)
                placeDao.delete(placeEntity)
            }
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

    suspend fun getTripWithPlaces(tripId: Long) = tripDao.getTripWithPlaces(tripId)

    suspend fun addPlaceToTrip(tripId: Long, place: PlaceEntity) {
        val existingPlace = placeDao.getByPlaceId(place.place_id)
        val placeEntity = if (existingPlace != null) {
            existingPlace
        } else {
            val newId = placeDao.insert(place)
            place.copy(id_ = newId)
        }

        tripDao.insertCrossRef(
            TripPlaceCrossRef(tripId = tripId, placeId = placeEntity.id_)
        )
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
