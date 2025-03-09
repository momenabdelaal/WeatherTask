package com.weather.core.utils

import com.weather.data.model.NetworkError

object ErrorHandler {
    fun getLocationError(error: Throwable): String {
        val networkError = NetworkError.from(error)
        return networkError.message ?: "حدث خطأ في استعادة الموقع"
    }

    fun getWeatherError(error: Throwable): String {
        val networkError = NetworkError.from(error)
        return networkError.message ?: "حدث خطأ في جلب بيانات الطقس"
    }

    fun getLocationValidationError(type: LocationValidationError): String {
        return when (type) {
            LocationValidationError.INVALID_COORDINATES -> "إحداثيات غير صالحة"
            LocationValidationError.INVALID_CITY_NAME -> "اسم المدينة غير صالح"
            LocationValidationError.SAVE_ERROR -> "حدث خطأ في حفظ الموقع"
        }
    }
}

enum class LocationValidationError {
    INVALID_COORDINATES,
    INVALID_CITY_NAME,
    SAVE_ERROR
}
