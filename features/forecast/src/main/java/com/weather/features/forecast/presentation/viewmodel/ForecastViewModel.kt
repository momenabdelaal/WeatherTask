package com.weather.features.forecast.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.model.LocationStateImpl
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.api.WeatherApi
import com.weather.data.repository.WeatherRepository
import com.weather.features.forecast.mvi.ForecastEffect
import com.weather.features.forecast.mvi.ForecastIntent
import com.weather.features.forecast.mvi.ForecastState
import com.weather.utils.error.HttpError
import com.weather.utils.error.NetworkError
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import android.content.Context

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val errorHandler: ErrorHandler,
    context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    private val _effect = Channel<ForecastEffect>()
    val effect = _effect.receiveAsFlow()
    
    private var weatherUpdateJob: Job? = null
    private var locationUpdateJob: Job? = null

    private fun updateToLoadingState() = _state.update { 
        it.copy(
            isLoading = true,
            error = null,
            forecastItems = emptyList(),
            city = null
        )
    }

    private fun updateToErrorState(message: String) = _state.update {
        it.copy(
            isLoading = false,
            error = message,
            forecastItems = emptyList(),
            city = null
        )
    }

    private suspend fun handleError(error: Throwable, clearLocation: Boolean = true) {
        val errorMessage = when (error) {
            is SecurityException -> {
                _effect.send(ForecastEffect.LocationRequired)
                errorHandler.getLocationError(error)
            }
            is IllegalStateException -> {
                if (error.message?.contains("No forecast data") == true) {
                    errorHandler.getLocationValidationError(LocationValidationError.LOCATION_NOT_FOUND)
                } else if (error.message?.contains("Invalid location") == true) {
                    _effect.send(ForecastEffect.LocationValidationFailed)
                    errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES)
                } else {
                    errorHandler.getForecastError(error)
                }
            }
            is IllegalArgumentException -> {
                if (error.message?.contains("Invalid coordinates") == true) {
                    _effect.send(ForecastEffect.LocationValidationFailed)
                    errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES)
                } else {
                    errorHandler.getForecastError(error)
                }
            }
            else -> errorHandler.getForecastError(error)
        }
        
        updateToErrorState(errorMessage)
        
        if (clearLocation) {
            _state.update { it.copy(lastLocation = null) }
        }
        
        _effect.send(ForecastEffect.Error(error))
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    private fun cleanup() {
        weatherUpdateJob?.cancel()
        locationUpdateJob?.cancel()
        weatherUpdateJob = null
        locationUpdateJob = null
        
        _state.update { 
            it.copy(
                isLoading = false,
                error = null,
                forecastItems = emptyList(),
                city = null,
                lastLocation = null
            )
        }
        
        _effect.close()
    }

    fun processIntent(intent: ForecastIntent) {
        when (intent) {
            is ForecastIntent.RefreshForecast -> {
                updateToLoadingState()
                _state.update { it.copy(lastLocation = intent.location) }
                fetchForecast(intent.location)
            }
            ForecastIntent.RetryLastFetch -> {
                val lastLocation = state.value.lastLocation
                if (lastLocation is LocationStateImpl.Available) {
                    updateToLoadingState()
                    fetchForecast(lastLocation)
                } else {
                    viewModelScope.launch {
                        _effect.send(ForecastEffect.LocationRequired)
                        handleError(IllegalStateException("No previous location"))
                    }
                }
            }
        }
    }

    fun observeLocationUpdates(sharedViewModel: SharedWeatherViewModel) {
        locationUpdateJob?.cancel()
        weatherUpdateJob?.cancel()
        
        locationUpdateJob = viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isLoading = true,
                        error = null,
                        forecastItems = emptyList(),
                        city = null,
                        lastLocation = null
                    )
                }
                
                sharedViewModel.locationState.combine(sharedViewModel.locationError) { location, error ->
                    if (error != null) {
                        handleError(IllegalStateException(error))
                    } else {
                        processIntent(ForecastIntent.RefreshForecast(location))
                    }
                }.collect()
            } catch (e: Exception) {
                e.printStackTrace()
                handleError(e)
            }
        }
    }

    private fun fetchForecast(location: LocationStateImpl) {
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                when (location) {
                    is LocationStateImpl.Available -> {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        
                        if (!isValidCoordinates(latitude, longitude)) {
                            _effect.send(ForecastEffect.LocationValidationFailed)
                            handleError(IllegalArgumentException("Invalid coordinates"))
                            return@launch
                        }
                        
                        _state.update { 
                            it.copy(
                                isLoading = true,
                                lastLocation = location,
                                error = null,
                                forecastItems = emptyList(),
                                city = null
                            )
                        }
                        
                        weatherRepository.getForecast(
                            latitude = latitude,
                            longitude = longitude,
                            language = WeatherApi.DEFAULT_LANGUAGE
                        ).collect { result ->
                            result.fold(
                                onSuccess = { forecast ->
                                    if (forecast.forecastList.isEmpty()) {
                                        handleError(IllegalStateException("No forecast data"))
                                    } else {
                                        // Group by date and take first item per day, limited to 7 days
                                        val sevenDayForecast = forecast.forecastList
                                            .groupBy { item -> 
                                                Instant.ofEpochSecond(item.dateTime)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDate()
                                            }
                                            .values
                                            .map { it.first() }
                                            .take(ForecastState.MAX_FORECAST_DAYS)

                                        _state.update {
                                            it.copy(
                                                isLoading = false,
                                                error = null,
                                                forecastItems = sevenDayForecast,
                                                city = forecast.city
                                            )
                                        }
                                        _effect.send(ForecastEffect.DataRefreshed)
                                    }
                                },
                                onFailure = { error ->
                                    handleError(error)
                                }
                            )
                        }
                    }
                    else -> {
                        _effect.send(ForecastEffect.LocationRequired)
                        handleError(IllegalStateException("Invalid location state"))
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
