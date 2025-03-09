package com.weather.data.repository

import com.weather.data.model.WeatherResponse
import com.weather.data.model.ForecastResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for fetching weather data.
 * All methods return a Flow<Result> that may contain either a success value or a failure.
 *
 * Possible errors that may be returned in the Result.failure:
 * - UnknownHostException: When there is no internet connection
 * - SocketTimeoutException: When the request times out
 * - StreamResetException: When there is a connection error with the server
 * - IllegalArgumentException: When invalid parameters are provided
 */
interface WeatherRepository {
    /**
     * Get current weather for a specific location
     * @param latitude Valid latitude between -90 and 90
     * @param longitude Valid longitude between -180 and 180
     * @param language Language code for the response, defaults to Arabic
     * @return Flow of Result containing WeatherResponse on success
     */
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        language: String = "ar"
    ): Flow<Result<WeatherResponse>>

    /**
     * Get current weather for a city by name
     * @param cityName Name of the city
     * @param language Language code for the response, defaults to Arabic
     * @return Flow of Result containing WeatherResponse on success
     */
    suspend fun getCurrentWeatherByCity(
        cityName: String,
        language: String = "ar"
    ): Flow<Result<WeatherResponse>>
    
    /**
     * Get weather forecast for a specific location
     * @param latitude Valid latitude between -90 and 90
     * @param longitude Valid longitude between -180 and 180
     * @param language Language code for the response, defaults to Arabic
     * @return Flow of Result containing ForecastResponse on success
     */
    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        language: String = "ar"
    ): Flow<Result<ForecastResponse>>

    /**
     * Get weather forecast for a city by name
     * @param cityName Name of the city
     * @param language Language code for the response, defaults to Arabic
     * @return Flow of Result containing ForecastResponse on success
     */
    suspend fun getForecastByCity(
        cityName: String,
        language: String = "ar"
    ): Flow<Result<ForecastResponse>>
}
