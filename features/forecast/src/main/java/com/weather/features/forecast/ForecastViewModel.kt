package com.weather.features.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.model.LocationStateImpl
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.api.WeatherApi
import com.weather.data.model.HttpError
import com.weather.data.model.NetworkError
import com.weather.data.repository.WeatherRepository
import com.weather.features.forecast.mvi.ForecastEffect
import com.weather.features.forecast.mvi.ForecastIntent
import com.weather.features.forecast.mvi.ForecastState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import android.content.Context
import com.weather.core.utils.ErrorHandler
import com.weather.core.utils.LocationValidationError
import com.weather.features.forecast.R

// MVI ViewModel for 7-day weather forecast
@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    context: Context
) : ViewModel() {

    private val errorHandler = ErrorHandler(context)



    // MVI State Management
    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    // Side Effects
    private val _effect = Channel<ForecastEffect>()
    val effect = _effect.receiveAsFlow()
    
    // Async Operations
    private var weatherUpdateJob: Job? = null
    private var locationUpdateJob: Job? = null

    // State Updates
    private fun updateToLoadingState() = _state.update { 
        it.copy(
            isLoading = true,
            error = null,
            forecast = null
        )
    }

    private fun updateToErrorState(message: String) = _state.update {
        it.copy(
            isLoading = false,
            error = message,
            forecast = null
        )
    }

    private suspend fun handleError(error: Throwable, clearLocation: Boolean = true) {
        val errorMessage = errorHandler.getForecastError(error)
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

    // Cleanup resources
    private fun cleanup() {
        // Cancel jobs
        weatherUpdateJob?.cancel()
        locationUpdateJob?.cancel()
        weatherUpdateJob = null
        locationUpdateJob = null
        
        // Reset state
        _state.update { 
            it.copy(
                isLoading = false,
                error = null,
                forecast = null,
                lastLocation = null
            )
        }
        
        // Close channel
        _effect.close()
    }


    // Process user intents
    fun processIntent(intent: ForecastIntent) {
        when (intent) {
            is ForecastIntent.RefreshForecast -> {
                // Update state immediately to reflect new location
                updateToLoadingState()
                _state.update { it.copy(lastLocation = intent.location) }
                fetchForecast(intent.location)
            }
            ForecastIntent.RetryLastFetch -> {
                val lastLocation = state.value.lastLocation
                if (lastLocation is LocationStateImpl.Available) {
                    // Reset error state before retrying
                    updateToLoadingState()
                    fetchForecast(lastLocation)
                } else {
                    viewModelScope.launch {
                        handleError(IllegalStateException("No previous location"))
                    }
                }
            }
        }
    }

    fun observeLocationUpdates(sharedViewModel: SharedWeatherViewModel) {
        // Cancel active jobs
        locationUpdateJob?.cancel()
        weatherUpdateJob?.cancel()
        
        locationUpdateJob = viewModelScope.launch {
            try {
                // Reset to initial state
                _state.update { 
                    it.copy(
                        isLoading = true,
                        error = null,
                        forecast = null,
                        lastLocation = null
                    )
                }
                
                // Watch location updates
                sharedViewModel.locationState.combine(sharedViewModel.locationError) { location, error ->
                    if (error != null) {
                        val errorMessage = errorHandler.getLocationError(IllegalStateException(error))
                        handleError(IllegalStateException(error))
                    } else {
                        // Process the location update as a refresh intent
                        processIntent(ForecastIntent.RefreshForecast(location))
                    }
                }.collect()
            } catch (e: Exception) {
                e.printStackTrace()
                // Map error types
                handleError(e)
            }
        }
    }


    private fun fetchForecast(location: LocationStateImpl) {
        // Prevent parallel requests
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                when (location) {
                    is LocationStateImpl.Available -> {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        
                        if (!isValidCoordinates(latitude, longitude)) {
                            errorHandler.getLocationValidationError(
                                LocationValidationError.INVALID_COORDINATES)
                            handleError(IllegalArgumentException("Invalid coordinates"))
                            return@launch
                        }
                        
                        _state.update { 
                            it.copy(
                                isLoading = true,
                                lastLocation = location,
                                error = null,
                                forecast = null
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
                                        viewModelScope.launch {
                                            handleError(IllegalStateException("No forecast data"))
                                        }
                                        return@fold
                                    }
                                    
                                    val sevenDayForecast = try {
                                        forecast.forecastList
                                            .groupBy { 
                                                Instant.ofEpochSecond(it.dateTime)
                                                    .atZone(ZoneId.systemDefault())
                                                    .toLocalDate()
                                                    .toString()
                                            }
                                            .values
                                            .mapNotNull { dailyForecasts ->
                                                // Get forecast closest to noon for each day
                                                dailyForecasts.minByOrNull { forecastItem ->
                                                    val hour = Instant.ofEpochSecond(forecastItem.dateTime)
                                                        .atZone(ZoneId.systemDefault())
                                                        .hour
                                                    kotlin.math.abs(12 - hour)
                                                }
                                            }
                                            .take(7)
                                            .filter { forecastItem ->
                                                forecastItem.weather.isNotEmpty()
                                            }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        viewModelScope.launch {
                                            handleError(e)
                                        }
                                        emptyList()
                                    }

                                    when {
                                        sevenDayForecast.isEmpty() -> {
                                            val errorMessage = errorHandler.getForecastError(IllegalStateException("Empty forecast data"))
                                            _state.update { 
                                                it.copy(
                                                    isLoading = false,
                                                    error = errorMessage,
                                                    forecast = null,
                                                    lastLocation = null
                                                )
                                            }
                                            viewModelScope.launch {
                                                _effect.send(ForecastEffect.Error(IllegalStateException(errorMessage)))
                                            }
                                        }
                                        sevenDayForecast.any { it.weather.isEmpty() } -> {
                                            val errorMessage = errorHandler.getForecastError(IllegalStateException("Incomplete forecast data"))
                                            _state.update { 
                                                it.copy(
                                                    isLoading = false,
                                                    error = errorMessage,
                                                    forecast = null,
                                                    lastLocation = null
                                                )
                                            }
                                            viewModelScope.launch {
                                                _effect.send(ForecastEffect.Error(IllegalStateException(errorMessage)))
                                            }
                                        }
                                        else -> {
                                            _state.update { 
                                                it.copy(
                                                    isLoading = false,
                                                    forecast = forecast.copy(forecastList = sevenDayForecast),
                                                    error = null,
                                                    lastLocation = location
                                                )
                                            }
                                            viewModelScope.launch {
                                                _effect.send(ForecastEffect.DataRefreshed)
                                            }
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    viewModelScope.launch {
                                        handleError(error)
                                    }
                                }
                            )
                        }
                    }
                    is LocationStateImpl.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is LocationStateImpl.Unavailable -> {
                        viewModelScope.launch {
                            handleError(IllegalStateException("Location required"), clearLocation = false)
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is NetworkError -> when (e) {
                        is NetworkError.ApiError -> when (e.code) {
                            401 -> WeatherApi.ERROR_API_KEY
                            404 -> "لا توجد بيانات توقعات متاحة"
                            429 -> "تم تجاوز حد الطلبات"
                            500, 502, 503, 504 -> WeatherApi.ERROR_SERVER
                            else -> e.message
                        }
                        is NetworkError.NoInternet -> WeatherApi.ERROR_NETWORK
                        else -> e.message ?: "حدث خطأ غير متوقع"
                    }
                    is HttpError -> NetworkError.ApiError(e.code).message
                    else -> NetworkError.from(e).message ?: "حدث خطأ غير متوقع"
                }
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
                _effect.send(ForecastEffect.Error(e))
            }
        }
    }

    // Validate coordinate bounds
    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude in WeatherApi.MIN_LATITUDE..WeatherApi.MAX_LATITUDE &&
        longitude in WeatherApi.MIN_LONGITUDE..WeatherApi.MAX_LONGITUDE


}

