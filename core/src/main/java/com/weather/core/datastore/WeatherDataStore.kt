package com.weather.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.weather.core.model.LocationState
import com.weather.core.model.LocationStateImpl
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
                    clearLocationData(notifyUser = false)
                }
                else -> {
                    Log.e(TAG, "Error reading preferences: ${exception.message}", exception)
                }
            }
            // Always emit empty preferences on error to allow the app to continue
            emit(emptyPreferences())
        }

    /**
     * Saves the last searched location to DataStore
     * @param location The location state to save. Must be LocationStateImpl.Available, otherwise returns failure
     * @return Result indicating success or failure
     * @throws IllegalArgumentException if latitude, longitude, or city name are invalid
     * @throws IllegalStateException if location state is not Available
     */
    suspend fun saveLastLocation(location: LocationState): Result<Unit> {
        return try {
            when (val locationImpl = location as? LocationStateImpl) {
                is LocationStateImpl.Available -> {
                    // Validate location data
                    if (locationImpl.latitude < -90 || locationImpl.latitude > 90) {
                        Log.e(TAG, "Invalid latitude value: ${locationImpl.latitude}")
                        Result.failure(IllegalArgumentException("قيمة خط العرض غير صالحة"))
                    } else if (locationImpl.longitude < -180 || locationImpl.longitude > 180) {
                        Log.e(TAG, "Invalid longitude value: ${locationImpl.longitude}")
                        Result.failure(IllegalArgumentException("قيمة خط الطول غير صالحة"))
                    } else if (locationImpl.cityName.isBlank()) {
                        Log.e(TAG, "City name is blank")
                        Result.failure(IllegalArgumentException("اسم المدينة فارغ"))
                    } else {
                        store.edit { preferences ->
                            preferences[PreferencesKeys.LAST_LATITUDE] = locationImpl.latitude
                            preferences[PreferencesKeys.LAST_LONGITUDE] = locationImpl.longitude
                            preferences[PreferencesKeys.LAST_CITY_NAME] = locationImpl.cityName
                        }
                        Log.d(TAG, "Successfully saved location: ${locationImpl.cityName} (${locationImpl.latitude}, ${locationImpl.longitude})")
                        Result.success(Unit)
                    }
                }
                is LocationStateImpl.Loading,
                is LocationStateImpl.Unavailable,
                null -> {
                    Log.d(TAG, "Cannot save non-available location state")
                    Result.failure(IllegalStateException("لا يمكن حفظ حالة الموقع غير المتوفرة"))
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error saving location: ${exception.message}", exception)
            Result.failure(Exception("فشل في حفظ الموقع", exception))
        }
    }

    /**
     * Retrieves the last searched location from DataStore
     * @return Flow of LocationState. Emits LocationStateImpl.Loading initially, then LocationStateImpl.Available if valid location exists,
     *         or LocationStateImpl.Unavailable if no valid location exists or an error occurs
     */
    fun getLastLocation(): Flow<LocationStateImpl> = data
        .map { preferences ->
            val latitude = preferences[PreferencesKeys.LAST_LATITUDE]
            val longitude = preferences[PreferencesKeys.LAST_LONGITUDE]
            val cityName = preferences[PreferencesKeys.LAST_CITY_NAME]

            when {
                latitude == null -> {
                    Log.d(TAG, "No latitude found in preferences")
                    LocationStateImpl.Unavailable
                }
                longitude == null -> {
                    Log.d(TAG, "No longitude found in preferences")
                    LocationStateImpl.Unavailable
                }
                cityName == null -> {
                    Log.d(TAG, "No city name found in preferences")
                    LocationStateImpl.Unavailable
                }
                latitude < -90 || latitude > 90 -> {
                    Log.e(TAG, "Invalid latitude value in preferences: $latitude")
                    LocationStateImpl.Unavailable
                }
                longitude < -180 || longitude > 180 -> {
                    Log.e(TAG, "Invalid longitude value in preferences: $longitude")
                    LocationStateImpl.Unavailable
                }
                cityName.isBlank() -> {
                    Log.e(TAG, "Blank city name in preferences")
                    LocationStateImpl.Unavailable
                }
                else -> {
                    LocationStateImpl.Available(
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName
                    ).also { available ->
                        Log.d(TAG, "Retrieved last location: ${available.cityName} (${available.latitude}, ${available.longitude})")
                    }
                }
            }
        }
        .catch { exception ->
            Log.e(TAG, "Error retrieving last location: ${exception.message}", exception)
            emit(LocationStateImpl.Unavailable)
        }
        .onStart { 
            emit(LocationStateImpl.Loading)
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
