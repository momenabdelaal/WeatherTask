package com.weather.features.forecast.presentation

import com.weather.data.model.ForecastResponse

sealed interface ForecastUiState {
    data object Loading : ForecastUiState
    data class Error(val message: String) : ForecastUiState
    data class Success(val forecast: ForecastResponse) : ForecastUiState
}
