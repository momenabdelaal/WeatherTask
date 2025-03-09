package com.weather.features.currentweather.domain.usecase

import com.weather.data.api.WeatherApi
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import com.weather.utils.error.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCurrentWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val errorHandler: ErrorHandler
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Flow<Result<WeatherResponse>> {
        return weatherRepository.getCurrentWeather(
            latitude = latitude,
            longitude = longitude,
            language = WeatherApi.DEFAULT_LANGUAGE
        ).catch { error ->
            emit(Result.failure(error))
        }.map { result ->
            result.fold(
                onSuccess = { weather ->
                    if (weather.weather.isNotEmpty()) {
                        Result.success(weather)
                    } else {
                        Result.failure(IllegalStateException("No weather data available"))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        }
    }
}
