package com.weather.features.forecast.presentation

import com.weather.data.model.ForecastResponse

// MVI UI states for forecast feature
sealed interface ForecastUiState {
    // Loading state
    data object Loading : ForecastUiState
    // Error state with message
    data class Error(val message: String) : ForecastUiState
    // Success state with forecast data
    data class Success(val forecast: ForecastResponse) : ForecastUiState
}
