package com.weather.features.forecast.mvi

import com.weather.core.model.LocationStateImpl
import com.weather.data.model.ForecastResponse

// State for 7-day forecast screen
data class ForecastState(
    // Loading indicator
    val isLoading: Boolean = false,
    
    // Forecast data when available
    val forecast: ForecastResponse? = null,
    
    // Error message if any
    val error: String? = null,
    
    // Last location used for forecast
    val lastLocation: LocationStateImpl? = null
)
