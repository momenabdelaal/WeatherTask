package com.weather.features.forecast.mvi

// One-time events for forecast feature
sealed interface ForecastEffect {
    // Successful data refresh
    data object DataRefreshed : ForecastEffect
    
    // Error events (network, location, etc.)
    data class Error(
        val throwable: Throwable
    ) : ForecastEffect
}
