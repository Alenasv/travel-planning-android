package com.example.travel_planning.repository

import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.db.entities.*

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

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.insertTrip(trip)
    }

    suspend fun deleteTrip(trip: TripEntity) {
        // tripDao.deleteTrip(trip)
    }

    suspend fun getTripWithPlaces(tripId: Long) = tripDao.getTripWithPlaces(tripId)

    suspend fun addPlaceToTrip(tripId: Long, place: PlaceEntity) {
        val existingPlace = placeDao.getByPlaceId(place.place_id)
        val placeEntity = existingPlace ?: place.also { placeDao.insert(it) }

        tripDao.insertCrossRef(
            TripPlaceCrossRef(tripId = tripId, placeId = placeEntity.id_)
        )
    }

    suspend fun removePlaceFromTrip(tripId: Long, placeId: Long) {
        tripDao.deleteCrossRef(tripId, placeId)
    }
}