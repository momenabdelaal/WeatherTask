package com.weather.features.currentweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.model.LocationStateImpl
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.features.currentweather.domain.model.CurrentWeatherEvent
import com.weather.features.currentweather.domain.model.CurrentWeatherUiState
import com.weather.features.currentweather.domain.usecase.GetCurrentWeatherUseCase
import com.weather.utils.error.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val getCurrentWeatherUseCase: GetCurrentWeatherUseCase,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    private var weatherUpdateJob: Job? = null
    private val _uiState = MutableStateFlow<CurrentWeatherUiState>(CurrentWeatherUiState.Loading)
    val uiState: StateFlow<CurrentWeatherUiState> = _uiState.asStateFlow()

    fun handleEvent(event: CurrentWeatherEvent) {
        when (event) {
            is CurrentWeatherEvent.RefreshWeather -> {
                refreshWeather(event.latitude, event.longitude)
            }
            is CurrentWeatherEvent.RetryLastFetch -> {
                // Get last location from SharedViewModel and retry
                observeWeatherUpdates(event.sharedViewModel)
            }
        }
    }

    private fun refreshWeather(latitude: Double, longitude: Double) {
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                _uiState.value = CurrentWeatherUiState.Loading
                
                getCurrentWeatherUseCase(latitude, longitude)
                    .collect { result ->
                        _uiState.value = when {
                            result.isSuccess -> {
                                val weather = result.getOrNull()
                                if (weather != null) {
                                    CurrentWeatherUiState.Success(weather)
                                } else {
                                    CurrentWeatherUiState.Error(errorHandler.getWeatherError(IllegalStateException("No weather data available")))
                                }
                            }
                            result.isFailure -> CurrentWeatherUiState.Error(errorHandler.getWeatherError(result.exceptionOrNull()!!))
                            else -> CurrentWeatherUiState.Error(errorHandler.getWeatherError(IllegalStateException("Unexpected error")))
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = CurrentWeatherUiState.Error(errorHandler.getWeatherError(e))
            }
        }
    }

    fun observeWeatherUpdates(sharedViewModel: SharedWeatherViewModel) {
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                _uiState.value = CurrentWeatherUiState.Loading
                
                sharedViewModel.locationState.collect { locationState ->
                    when (locationState) {
                        is LocationStateImpl.Available -> {
                            refreshWeather(locationState.latitude, locationState.longitude)
                        }
                        is LocationStateImpl.Unavailable -> {
                            _uiState.value = CurrentWeatherUiState.Error(errorHandler.getLocationError(IllegalStateException("Location unavailable")))
                        }
                        is LocationStateImpl.Loading -> {
                            _uiState.value = CurrentWeatherUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = CurrentWeatherUiState.Error(errorHandler.getWeatherError(e))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        weatherUpdateJob?.cancel()
        weatherUpdateJob = null
        _uiState.value = CurrentWeatherUiState.Loading
    }
}
