package com.weather.features.cityinput.domain.model

import android.app.Activity

sealed class CityInputEvent {
    data class SearchCity(val query: String) : CityInputEvent()
    data object GetCurrentLocation : CityInputEvent()
    data class SelectCity(
        val latitude: Double,
        val longitude: Double,
        val cityName: String
    ) : CityInputEvent()
    data class RequestLocationPermission(val activity: Activity) : CityInputEvent()
    data class OpenLocationSettings(val activity: Activity) : CityInputEvent()
}
