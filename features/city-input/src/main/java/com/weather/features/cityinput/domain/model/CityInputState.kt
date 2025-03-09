package com.weather.features.cityinput.domain.model

import com.weather.core.model.LocationState

sealed class CityInputState {
    data object Initial : CityInputState()
    data object Loading : CityInputState()
    data class Success(val locationState: LocationState) : CityInputState()
    data class Error(val message: String) : CityInputState()
    data class ValidationError(val message: String) : CityInputState()
    data class PermissionRequired(val message: String) : CityInputState()
    data class LocationDisabled(val message: String) : CityInputState()
}
