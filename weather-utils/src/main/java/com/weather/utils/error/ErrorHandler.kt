package com.weather.utils.error

import android.content.Context
import com.weather.utils.R


class ErrorHandler(private val context: Context) {

    fun getLocationError(error: Throwable): String {
        return when (val networkError = NetworkError.from(error)) {
            else -> getNetworkErrorMessage(networkError)
        }
    }


    fun getWeatherError(error: Throwable): String {
        return when (val networkError = NetworkError.from(error)) {
            else -> getNetworkErrorMessage(networkError)
        }
    }


    fun getLocationValidationError(type: LocationValidationError): String {
        return when (type) {
            LocationValidationError.INVALID_COORDINATES -> context.getString(R.string.error_invalid_coordinates)
            LocationValidationError.INVALID_CITY_NAME -> context.getString(R.string.error_invalid_city_name)
            LocationValidationError.SAVE_ERROR -> context.getString(R.string.error_location_save)
        }
    }


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


enum class LocationValidationError {
    INVALID_COORDINATES,
    INVALID_CITY_NAME,
    SAVE_ERROR
}
