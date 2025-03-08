package com.weather.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.weather.core.model.LocationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_preferences")

@Singleton
class WeatherDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store: DataStore<Preferences> = context.weatherDataStore

    val data: Flow<Preferences> = store.data
        .catch { exception ->
            // Handle data corruption by clearing preferences
            when (exception) {
                is IllegalArgumentException,
                is ClassCastException,
                is IllegalStateException -> {
                    Log.e(TAG, "Data corruption detected: ${exception.message}", exception)
                    clearLocationData()
                }
                else -> {
                    Log.e(TAG, "Error reading preferences: ${exception.message}", exception)
                }
            }
            emit(emptyPreferences())
        }

    /**
     * Saves the last searched location to DataStore
     * @param location The location state to save
     * @return Result indicating success or failure
     */
    suspend fun saveLastLocation(location: LocationState): Result<Unit> {
        return try {
            // Validate location data
            if (location.latitude < -90 || location.latitude > 90) {
                Log.e(TAG, "Invalid latitude value: ${location.latitude}")
                Result.failure(IllegalArgumentException("قيمة خط العرض غير صالحة"))
            } else if (location.longitude < -180 || location.longitude > 180) {
                Log.e(TAG, "Invalid longitude value: ${location.longitude}")
                Result.failure(IllegalArgumentException("قيمة خط الطول غير صالحة"))
            } else if (location.cityName.isBlank()) {
                Log.e(TAG, "City name is blank")
                Result.failure(IllegalArgumentException("اسم المدينة فارغ"))
            } else {
                store.edit { preferences ->
                    preferences[PreferencesKeys.LAST_LATITUDE] = location.latitude
                    preferences[PreferencesKeys.LAST_LONGITUDE] = location.longitude
                    preferences[PreferencesKeys.LAST_CITY_NAME] = location.cityName
                }
                Log.d(TAG, "Successfully saved location: ${location.cityName} (${location.latitude}, ${location.longitude})")
                Result.success(Unit)
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error saving location: ${exception.message}", exception)
            Result.failure(Exception("فشل في حفظ الموقع", exception))
        }
    }

    /**
     * Retrieves the last searched location from DataStore
     * @return Flow of LocationState or null if no location is saved
     */
    fun getLastLocation(): Flow<LocationState?> = data
        .map { preferences ->
            val latitude = preferences[PreferencesKeys.LAST_LATITUDE]
            val longitude = preferences[PreferencesKeys.LAST_LONGITUDE]
            val cityName = preferences[PreferencesKeys.LAST_CITY_NAME]

            when {
                latitude == null -> {
                    Log.d(TAG, "No latitude found in preferences")
                    null
                }
                longitude == null -> {
                    Log.d(TAG, "No longitude found in preferences")
                    null
                }
                cityName == null -> {
                    Log.d(TAG, "No city name found in preferences")
                    null
                }
                latitude < -90 || latitude > 90 -> {
                    Log.e(TAG, "Invalid latitude value in preferences: $latitude")
                    null
                }
                longitude < -180 || longitude > 180 -> {
                    Log.e(TAG, "Invalid longitude value in preferences: $longitude")
                    null
                }
                cityName.isBlank() -> {
                    Log.e(TAG, "Blank city name in preferences")
                    null
                }
                else -> {
                    LocationState(
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName
                    ).also {
                        Log.d(TAG, "Retrieved last location: ${it.cityName} (${it.latitude}, ${it.longitude})")
                    }
                }
            }
        }
        .catch { exception ->
            Log.e(TAG, "Error retrieving last location: ${exception.message}", exception)
            emit(null)
        }

    /**
     * Clears all saved location data
     * @return Result indicating success or failure
     */
    suspend fun clearLocationData(notifyUser: Boolean = true): Result<Unit> {
        return try {
            store.edit { preferences ->
                preferences.remove(PreferencesKeys.LAST_LATITUDE)
                preferences.remove(PreferencesKeys.LAST_LONGITUDE)
                preferences.remove(PreferencesKeys.LAST_CITY_NAME)
            }
            if (notifyUser) {
                Log.d(TAG, "Successfully cleared location data")
            }
            Result.success(Unit)
        } catch (exception: Exception) {
            if (notifyUser) {
                Log.e(TAG, "Error clearing location data: ${exception.message}", exception)
                Result.failure(Exception("فشل في مسح بيانات الموقع", exception))
            } else {
                Result.failure(exception)
            }
        }
    }

    private object PreferencesKeys {
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        val LAST_CITY_NAME = stringPreferencesKey("last_city_name")
    }

    companion object {
        private const val TAG = "WeatherDataStore"
    }
}
