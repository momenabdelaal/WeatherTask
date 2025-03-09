package com.weather.features.currentweather.domain.model

import com.weather.core.viewmodel.SharedWeatherViewModel

sealed class CurrentWeatherEvent {
    data class RefreshWeather(
        val latitude: Double,
        val longitude: Double
    ) : CurrentWeatherEvent()
    
    data class RetryLastFetch(
        val sharedViewModel: SharedWeatherViewModel
    ) : CurrentWeatherEvent()
}
