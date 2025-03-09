package com.weather.features.currentweather.domain.model

import com.weather.data.model.WeatherResponse

sealed class CurrentWeatherUiState {
    data object Loading : CurrentWeatherUiState()
    data class Success(val weather: WeatherResponse) : CurrentWeatherUiState()
    data class Error(val message: String) : CurrentWeatherUiState()
}
