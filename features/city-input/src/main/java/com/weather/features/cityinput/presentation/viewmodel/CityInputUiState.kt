package com.weather.features.cityinput.presentation.viewmodel

import com.weather.core.model.LocationStateImpl

sealed class CityInputUiState {
    data object Initial : CityInputUiState()
    data object Loading : CityInputUiState()
    data class Error(val message: String) : CityInputUiState()
    data class PermissionRequired(val message: String) : CityInputUiState()
    data class LocationUpdated(val location: LocationStateImpl) : CityInputUiState()
}
