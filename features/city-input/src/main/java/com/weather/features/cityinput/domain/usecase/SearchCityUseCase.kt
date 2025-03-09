package com.weather.features.cityinput.domain.usecase

import com.weather.data.api.WeatherApi
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.NetworkError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchCityUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val errorHandler: ErrorHandler
) {
    suspend operator fun invoke(cityName: String): Flow<Result<WeatherResponse>> {
        // Validate input
        if (cityName.isBlank()) {
            return flowOf(
                Result.failure(
                    NetworkError.ValidationError(errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION))
                )
            )
        }

        return weatherRepository.getCurrentWeatherByCity(
            cityName = cityName.trim(),
            language = WeatherApi.DEFAULT_LANGUAGE
        ).catch { error ->
            // Convert repository errors to domain errors
            val domainError = when (error) {
                is NetworkError -> error
                is SecurityException -> NetworkError.PermissionDenied(errorHandler.getLocationError(error))
                else -> NetworkError.Unknown(errorHandler.getForecastError(error))
            }
            emit(Result.failure(domainError))
        }.map { result ->
            result.fold(
                onSuccess = { weather ->
                    Result.success(weather)
                },
                onFailure = { error ->
                    Result.failure(
                        when (error) {
                            is NetworkError -> error
                            is SecurityException -> NetworkError.PermissionDenied(errorHandler.getLocationError(error))
                            else -> NetworkError.Unknown(errorHandler.getForecastError(error))
                        }
                    )
                }
            )
        }
    }
}
