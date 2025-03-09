package com.weather.core.di

import android.content.Context
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationPermissionHelper
import com.weather.core.location.LocationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideWeatherDataStore(
        @ApplicationContext context: Context
    ): WeatherDataStore = WeatherDataStore(context)

    @Provides
    @Singleton
    fun provideLocationPermissionHelper(
        @ApplicationContext context: Context
    ): LocationPermissionHelper = LocationPermissionHelper(context)

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        locationPermissionHelper: LocationPermissionHelper
    ): LocationService = LocationService(context, locationPermissionHelper)
}
