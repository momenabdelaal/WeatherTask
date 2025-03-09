package com.weather.features.cityinput.domain.model

import com.weather.core.model.LocationStateImpl

sealed class CityInputUiState {
    data object Initial : CityInputUiState()
    data object Loading : CityInputUiState()
    data class LocationUpdated(val location: LocationStateImpl) : CityInputUiState()
    data class Error(val message: String) : CityInputUiState()
}
