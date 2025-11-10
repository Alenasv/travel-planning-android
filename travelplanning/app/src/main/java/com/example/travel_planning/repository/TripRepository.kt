package com.example.travel_planning.repository

import Place
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.db.entities.*
import com.example.travel_planning.ui.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TripRepository(private val db: AppDatabase) {

    private val tripDao = db.tripDao()
    private val placeDao = db.placeDao()

    suspend fun createTrip(name: String, date: String? = null, notes: String? = null): Long {
        val trip = TripEntity(name = name, date = date, notes = notes)
        return tripDao.insertTrip(trip)
    }

    suspend fun getAllTrips(): List<TripEntity> {
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

    suspend fun removePlaceFromTrip(tripId: Long, placeIdString: String) {
        val place = placeDao.getByPlaceId(placeIdString)
        place?.let { placeEntity ->
            tripDao.deleteCrossRef(tripId,  placeEntity.id_)
                placeDao.delete(placeEntity)
            }
        }
    }
suspend fun TripRepository.getTripForUI(tripId: Long): Trip? {
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
                    image_filename = placeEntity.imageFilename ?: ""
                )
            }
        )
    }
}

