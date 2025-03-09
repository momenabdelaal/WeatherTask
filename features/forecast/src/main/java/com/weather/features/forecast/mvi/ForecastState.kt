package com.weather.features.forecast.mvi

import com.weather.core.model.LocationStateImpl
import com.weather.data.model.ForecastItem
import com.weather.data.model.City

// State for 7-day forecast screen
data class ForecastState(
    // Loading indicator
    val isLoading: Boolean = false,
    
    // Forecast data when available - limited to 7 days
    val forecastItems: List<ForecastItem> = emptyList(),
    
    // City information
    val city: City? = null,
    
    // Error message if any
    val error: String? = null,
    
    // Last location used for forecast
    val lastLocation: LocationStateImpl? = null
) {
    companion object {
        const val MAX_FORECAST_DAYS = 7
    }
}
