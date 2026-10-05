package com.example.travel_planning.di

import android.content.Context
import androidx.room.Room
import com.example.travel_planning.db.AppDatabase
import com.example.travel_planning.db.dao.PlaceDao
import com.example.travel_planning.db.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "TravelPlanning.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    @Provides
    fun providePlaceDao(db: AppDatabase): PlaceDao = db.placeDao()
}
