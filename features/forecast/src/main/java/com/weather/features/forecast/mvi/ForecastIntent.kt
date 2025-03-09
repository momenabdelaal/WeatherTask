package com.weather.features.forecast.mvi

import com.weather.core.model.LocationStateImpl

// User actions for forecast feature
sealed interface ForecastIntent {
    // Get forecast for new location
    data class RefreshForecast(
        val location: LocationStateImpl
    ) : ForecastIntent

    // Retry last failed forecast request
    data object RetryLastFetch : ForecastIntent
}
