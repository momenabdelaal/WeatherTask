package com.weather.utils.error

import android.content.Context
import com.weather.utils.R
import com.weather.utils.error.LocationValidationError.*
import javax.inject.Inject

class ErrorHandler @Inject constructor(private val context: Context) {

    fun getLocationError(error: Throwable): String {
        return when (error) {
            is SecurityException -> context.getString(R.string.error_permission)
            is IllegalStateException -> context.getString(R.string.error_location_service)
            else -> getForecastError(error)
        }
    }

    fun getWeatherError(error: Throwable): String {
        return getForecastError(error)
    }

    fun getLocationValidationError(type: LocationValidationError): String {
        return when (type) {
            INVALID_COORDINATES -> context.getString(R.string.error_invalid_coordinates)
            INVALID_CITY_NAME -> context.getString(R.string.error_invalid_city_name)
            EMPTY_LOCATION -> context.getString(R.string.error_empty_location)
            LOCATION_NOT_FOUND -> context.getString(R.string.error_location_not_found)
            SAVE_ERROR -> context.getString(R.string.error_location_save)
        }
    }

    fun getForecastError(error: Throwable): String {
        return when (val networkError = NetworkError.from(error)) {
            is NetworkError.NoInternet -> context.getString(R.string.error_network)
            is NetworkError.Timeout -> context.getString(R.string.error_network)
            is NetworkError.ServerError -> context.getString(R.string.error_weather_generic)
            is NetworkError.ValidationError -> networkError.message
            is NetworkError.PermissionDenied -> context.getString(R.string.error_permission)
            is NetworkError.ApiError -> when (networkError.code) {
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
    }
}
