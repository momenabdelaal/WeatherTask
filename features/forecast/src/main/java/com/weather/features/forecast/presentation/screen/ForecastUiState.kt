package com.weather.features.forecast.presentation.screen

import com.weather.data.model.ForecastItem
import com.weather.data.model.City

// UI states for forecast screen following MVI pattern
sealed interface ForecastUiState {
    // Loading state when fetching data
    data object Loading : ForecastUiState

    // Success state with 7-day forecast data
    data class Success(
        val forecastItems: List<ForecastItem>,
        val city: City
    ) : ForecastUiState

    // Error state with message
    data class Error(
        val message: String
    ) : ForecastUiState
}
