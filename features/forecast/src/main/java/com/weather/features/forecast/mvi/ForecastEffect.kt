package com.weather.features.forecast.mvi

/**
 * One-time events for forecast feature
 */
sealed interface ForecastEffect {
    /**
     * Emitted when forecast data is successfully refreshed
     */
    data object DataRefreshed : ForecastEffect
    
    /**
     * Emitted when an error occurs during forecast operations
     * @param throwable The error that occurred
     */
    data class Error(
        val throwable: Throwable
    ) : ForecastEffect
    
    /**
     * Emitted when location validation fails
     */
    data object LocationValidationFailed : ForecastEffect
    
    /**
     * Emitted when location services are required
     */
    data object LocationRequired : ForecastEffect
}
