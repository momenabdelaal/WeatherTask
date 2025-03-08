package com.weather.core.model

data class LocationState(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
) {
    companion object {
        val DEFAULT = LocationState(
            latitude = 30.0444, // Cairo's coordinates as default
            longitude = 31.2357,
            cityName = "القاهرة"
        )
    }
}
