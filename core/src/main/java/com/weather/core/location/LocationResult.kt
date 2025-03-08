package com.weather.core.location

sealed class LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val cityName: String
    ) : LocationResult()

    sealed class Error : LocationResult() {
        data object PermissionDenied : Error()
        data object LocationDisabled : Error()
        data class ServiceError(val message: String) : Error()
    }
}
