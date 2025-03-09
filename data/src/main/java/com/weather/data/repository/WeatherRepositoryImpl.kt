package com.weather.data.repository

import android.util.Log
import com.weather.data.api.WeatherApi
import com.weather.data.model.WeatherResponse
import com.weather.data.model.ForecastResponse
import com.weather.utils.error.NetworkError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApi
) : WeatherRepository {
    
    companion object {
        private const val TAG = "WeatherRepository"
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
        private const val MIN_CITY_LENGTH = 2
        private const val MAX_CITY_LENGTH = 50
        
        private fun validateCoordinates(latitude: Double, longitude: Double) {
            when {
                latitude.isNaN() -> {
                    throw IllegalArgumentException("Latitude cannot be NaN")
                }
                longitude.isNaN() -> {
                    throw IllegalArgumentException("Longitude cannot be NaN")
                }
                latitude < MIN_LATITUDE || latitude > MAX_LATITUDE -> {
                    throw IllegalArgumentException("Invalid latitude: $latitude. Must be between $MIN_LATITUDE and $MAX_LATITUDE")
                }
                longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE -> {
                    throw IllegalArgumentException("Invalid longitude: $longitude. Must be between $MIN_LONGITUDE and $MAX_LONGITUDE")
                }
            }
        }
        
        private fun validateLanguage(language: String) {
            when {
                language.isBlank() -> {
                    throw IllegalArgumentException("Language code cannot be blank")
                }
                !language.matches(Regex("^[a-z]{2}(-[A-Z]{2})?$")) -> {
                    throw IllegalArgumentException("Invalid language code format. Must be 'xx' or 'xx-XX' format")
                }
            }
        }
        
        private fun validateCityName(cityName: String) {
            when {
                cityName.isBlank() -> {
                    throw IllegalArgumentException("City name cannot be blank")
                }
                cityName.length < MIN_CITY_LENGTH -> {
                    throw IllegalArgumentException("City name is too short. Minimum length is $MIN_CITY_LENGTH characters")
                }
                cityName.length > MAX_CITY_LENGTH -> {
                    throw IllegalArgumentException("City name is too long. Maximum length is $MAX_CITY_LENGTH characters")
                }
                !cityName.matches(Regex("^[\\p{L}\\s-]+$")) -> {
                    throw IllegalArgumentException("City name can only contain letters, spaces, and hyphens")
                }
            }
        }
        
        private fun handleNetworkError(e: Throwable): NetworkError {
            Log.e(TAG, "Converting error to NetworkError: ${e.message}", e)
            return when (e) {
                is IllegalArgumentException -> NetworkError.ApiError(400, e.message ?: "Invalid parameters")
                is IllegalStateException -> NetworkError.ApiError(422, e.message ?: "Invalid response data")
                else -> NetworkError.from(e)
            }
        }
    }

    override suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        language: String
    ): Flow<Result<WeatherResponse>> = flow {
        try {
            // Validate parameters
            validateCoordinates(latitude, longitude)
            validateLanguage(language)
            
            Log.d(TAG, "Fetching current weather for ($latitude, $longitude) in $language")
            val response = weatherApi.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = WeatherApi.API_KEY,
                language = language
            )
            
            if (response.weather.isEmpty()) {
                throw IllegalStateException("Invalid weather data received from API")
            }
            
            Log.d(TAG, "Successfully fetched weather data for ($latitude, $longitude)")
            emit(Result.success(response))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching current weather: ${e.message}", e)
            emit(Result.failure(handleNetworkError(e)))
        }
    }.onStart {
        Log.d(TAG, "Starting weather fetch for ($latitude, $longitude)")
    }

    override suspend fun getCurrentWeatherByCity(
        cityName: String,
        language: String
    ): Flow<Result<WeatherResponse>> = flow {
        try {
            // Validate parameters
            validateCityName(cityName)
            validateLanguage(language)
            
            Log.d(TAG, "Fetching current weather for city: $cityName in $language")
            val response = weatherApi.getCurrentWeatherByCity(
                cityName = cityName,
                apiKey = WeatherApi.API_KEY,
                language = language
            )
            
            if (response.weather.isEmpty()) {
                throw IllegalStateException("Invalid weather data received from API")
            }
            
            Log.d(TAG, "Successfully fetched and validated weather data for city: $cityName")
            emit(Result.success(response))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching current weather for city: ${e.message}", e)
            emit(Result.failure(handleNetworkError(e)))
        }
    }.onStart {
        Log.d(TAG, "Starting weather fetch for city: $cityName")
    }

    override suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        language: String
    ): Flow<Result<ForecastResponse>> = flow {
        try {
            // Validate parameters
            validateCoordinates(latitude, longitude)
            validateLanguage(language)
            
            Log.d(TAG, "Fetching forecast for ($latitude, $longitude) in $language")
            val response = weatherApi.getForecast(
                latitude = latitude,
                longitude = longitude,
                apiKey = WeatherApi.API_KEY,
                language = language
            )
            
            if (response.forecastList.isEmpty()) {
                throw IllegalStateException("No forecast data received from API")
            }
            
            // Validate each forecast item
            response.forecastList.forEach { forecastItem ->
                if (forecastItem.weather.isEmpty()) {
                    throw IllegalStateException("Invalid forecast data received from API")
                }
            }
            
            Log.d(TAG, "Successfully fetched and validated forecast data for ($latitude, $longitude)")
            emit(Result.success(response))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching forecast: ${e.message}", e)
            emit(Result.failure(handleNetworkError(e)))
        }
    }.onStart {
        Log.d(TAG, "Starting forecast fetch for ($latitude, $longitude)")
    }

    override suspend fun getForecastByCity(
        cityName: String,
        language: String
    ): Flow<Result<ForecastResponse>> = flow {
        try {
            // Validate parameters
            validateCityName(cityName)
            validateLanguage(language)
            
            Log.d(TAG, "Fetching forecast for city: $cityName in $language")
            val response = weatherApi.getForecastByCity(
                cityName = cityName,
                apiKey = WeatherApi.API_KEY,
                language = language
            )
            
            if (response.forecastList.isEmpty()) {
                throw IllegalStateException("No forecast data received from API")
            }
            
            // Validate each forecast item
            response.forecastList.forEach { forecastItem ->
                if (forecastItem.weather.isEmpty()) {
                    throw IllegalStateException("Invalid forecast data received from API")
                }
            }
            
            Log.d(TAG, "Successfully fetched and validated forecast data for city: $cityName")
            emit(Result.success(response))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching forecast for city: ${e.message}", e)
            emit(Result.failure(handleNetworkError(e)))
        }
    }.onStart {
        Log.d(TAG, "Starting forecast fetch for city: $cityName")
    }
}
