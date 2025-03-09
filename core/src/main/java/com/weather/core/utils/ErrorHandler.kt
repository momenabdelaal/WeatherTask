package com.weather.core.utils

import android.content.Context
import com.weather.core.R
import com.weather.data.api.WeatherApi
import com.weather.data.model.HttpError
import com.weather.data.model.NetworkError

/**
 * Centralized error handling utility for the Weather application.
 *
 * This object follows the Single Responsibility Principle by focusing solely on
 * error handling and message generation. It provides consistent error messaging
 * across the application for:
 * - Location-related errors
 * - Weather data errors
 * - Input validation errors
 *
 * All error messages are in Arabic to support the app's localization requirements.
 */
class ErrorHandler(private val context: Context) {
    /**
     * Converts location-related errors into user-friendly messages.
     * Handles errors that occur during:
     * - Location restoration from DataStore
     * - Location service operations
     * - Location permission requests
     *
     * @param error The original error that occurred
     * @return A user-friendly error message in Arabic
     */
    fun getLocationError(error: Throwable): String {
        return when (val networkError = NetworkError.from(error)) {
            is NetworkError -> getNetworkErrorMessage(networkError)
            else -> context.getString(R.string.error_location_generic)
        }
    }

    /**
     * Converts weather-related errors into user-friendly messages.
     * Handles errors that occur during:
     * - Weather API calls
     * - Weather data parsing
     * - Network connectivity issues
     *
     * @param error The original error that occurred
     * @return A user-friendly error message in Arabic
     */
    fun getWeatherError(error: Throwable): String {
        return when (val networkError = NetworkError.from(error)) {
            is NetworkError -> getNetworkErrorMessage(networkError)
            else -> context.getString(R.string.error_weather_generic)
        }
    }

    /**
     * Provides validation error messages for location-related input.
     * Handles validation errors for:
     * - Coordinate ranges (latitude: -90 to +90, longitude: -180 to +180)
     * - City name validity
     * - Location data persistence
     *
     * @param type The type of validation error that occurred
     * @return A specific validation error message in Arabic
     */
    fun getLocationValidationError(type: LocationValidationError): String {
        return when (type) {
            LocationValidationError.INVALID_COORDINATES -> context.getString(R.string.error_invalid_coordinates)
            LocationValidationError.INVALID_CITY_NAME -> context.getString(R.string.error_invalid_city_name)
            LocationValidationError.SAVE_ERROR -> context.getString(R.string.error_location_save)
        }
    }

    /**
     * Handles forecast-specific errors, including API errors, data processing errors,
     * and validation errors.
     *
     * @param error The error that occurred during forecast operations
     * @return A user-friendly error message in Arabic
     */
    /**
     * Get network-specific error message.
     */
    private fun getNetworkErrorMessage(error: NetworkError): String = when (error) {
        is NetworkError.NoInternet -> context.getString(R.string.error_network)
        is NetworkError.Timeout -> context.getString(R.string.error_network)
        is NetworkError.ServerError -> context.getString(R.string.error_weather_generic)
        is NetworkError.ApiError -> when (error.code) {
            HttpError.UNAUTHORIZED -> context.getString(R.string.error_weather_generic)
            HttpError.NOT_FOUND -> context.getString(R.string.error_no_forecast_data)
            HttpError.TOO_MANY_REQUESTS -> context.getString(R.string.error_rate_limit)
            HttpError.INTERNAL_SERVER_ERROR,
            HttpError.BAD_GATEWAY,
            HttpError.SERVICE_UNAVAILABLE,
            HttpError.GATEWAY_TIMEOUT -> context.getString(R.string.error_weather_generic)
            else -> context.getString(R.string.error_unexpected)
        }
        is NetworkError.Unknown -> context.getString(R.string.error_unexpected)
    }

    fun getForecastError(error: Throwable): String {
        return when (error) {
            is NetworkError -> getNetworkErrorMessage(error)
            is HttpError -> getNetworkErrorMessage(NetworkError.ApiError(error.code))
            is SecurityException -> context.getString(R.string.error_permission)
            is IllegalStateException -> context.getString(R.string.error_location_service)
            else -> context.getString(R.string.error_generic)
        }
    }
}

/**
 * Enumeration of possible location validation errors.
 *
 * @property INVALID_COORDINATES When latitude or longitude are out of valid ranges
 * @property INVALID_CITY_NAME When city name is blank or invalid
 * @property SAVE_ERROR When location data cannot be persisted to DataStore
 */
enum class LocationValidationError {
    INVALID_COORDINATES,
    INVALID_CITY_NAME,
    SAVE_ERROR
}
