package com.weather.core.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.R
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.core.model.LocationStateImpl
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.NetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWeatherViewModel @Inject constructor(
    private val locationService: LocationService,
    private val weatherDataStore: WeatherDataStore,
    private val weatherRepository: WeatherRepository,
    private val errorHandler: ErrorHandler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationStateImpl>(LocationStateImpl.Loading)
    val locationState: StateFlow<LocationStateImpl> = _locationState.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()
    
    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    init {
        restoreLastLocation()
    }

    private fun restoreLastLocation() {
        viewModelScope.launch {
            try {
                resetStates()
                _locationState.emit(LocationStateImpl.Loading)
                
                weatherDataStore.getLastLocation()
                    .catch { e -> 
                        handleLocationError(e)
                    }
                    .collect { location ->
                        when (location) {
                            is LocationStateImpl.Available -> {
                                if (isValidCoordinates(location.latitude, location.longitude)) {
                                    _locationState.emit(location)
                                    fetchWeatherData(location.latitude, location.longitude)
                                } else {
                                    _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                                    _locationState.emit(LocationStateImpl.Unavailable)
                                }
                            }
                            else -> _locationState.emit(location)
                        }
                    }
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    fun cleanup() {
        viewModelScope.launch {
            resetStates()
            _locationState.emit(LocationStateImpl.Unavailable)
        }
    }

    fun clearErrors() {
        viewModelScope.launch {
            _locationError.emit(null)
            _weatherError.emit(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                resetStates()
                _locationState.emit(LocationStateImpl.Loading)
                
                when (val result = locationService.getCurrentLocation()) {
                    is LocationResult.Success -> {
                        if (isValidCoordinates(result.latitude, result.longitude)) {
                            val cityName = result.cityName.takeIf { it.isNotBlank() } 
                                ?: context.getString(R.string.current_location)
                                
                            updateLocation(
                                latitude = result.latitude,
                                longitude = result.longitude,
                                cityName = cityName
                            )
                        } else {
                            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                            _locationState.emit(LocationStateImpl.Unavailable)
                        }
                    }
                    is LocationResult.Error -> {
                        when (result) {
                            LocationResult.Error.PermissionDenied -> 
                                handleLocationPermissionDenied()
                            LocationResult.Error.LocationDisabled -> 
                                handleLocationError(IllegalStateException())
                            is LocationResult.Error.ServiceError -> 
                                handleLocationError(IllegalStateException(result.message))
                        }
                    }
                }
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    fun handleLocationPermissionDenied() {
        viewModelScope.launch {
            try {
                resetStates()
                _locationError.emit(errorHandler.getLocationError(SecurityException("Location permission denied")))
                _locationState.emit(LocationStateImpl.Unavailable)
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    private fun resetStates() {
        viewModelScope.launch {
            _locationError.emit(null)
            _weatherError.emit(null)
            _weatherState.emit(null)
        }
    }

    private fun handleLocationError(error: Throwable) {
        viewModelScope.launch {
            val errorMessage = errorHandler.getLocationError(error)
            _locationError.emit(errorMessage)
            _locationState.emit(LocationStateImpl.Unavailable)
            _weatherState.emit(null)
            _weatherError.emit(null)
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, cityName: String) {
        viewModelScope.launch {
            try {
                if (!validateLocationInput(latitude, longitude, cityName)) {
                    return@launch
                }

                resetStates()
                
                val state = LocationStateImpl.Available(
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName
                )
                
                if (saveLocationState(state)) {
                    fetchWeatherData(latitude, longitude)
                }
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    private suspend fun validateLocationInput(latitude: Double, longitude: Double, cityName: String): Boolean {
        if (!isValidCoordinates(latitude, longitude)) {
            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
            _locationState.emit(LocationStateImpl.Unavailable)
            return false
        }
        
        if (cityName.isBlank()) {
            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION))
            _locationState.emit(LocationStateImpl.Unavailable)
            return false
        }
        
        return true
    }

    private suspend fun saveLocationState(state: LocationStateImpl.Available): Boolean {
        return try {
            _locationState.emit(state)
            
            weatherDataStore.saveLastLocation(state).onFailure { e ->
                e.printStackTrace()
                _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.SAVE_ERROR))
                _locationState.emit(LocationStateImpl.Unavailable)
                return false
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.SAVE_ERROR))
            _locationState.emit(LocationStateImpl.Unavailable)
            false
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                if (!isValidCoordinates(latitude, longitude)) {
                    _weatherError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                    _weatherState.emit(null)
                    return@launch
                }
                
                _weatherError.emit(null)
                _weatherState.emit(null)
                
                weatherRepository.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude
                ).catch { error ->
                    _weatherError.emit(errorHandler.getForecastError(error))
                    _weatherState.emit(null)
                }.collect { result ->
                    result.fold(
                        onSuccess = { weather ->
                            _weatherState.emit(weather)
                            _weatherError.emit(null)
                        },
                        onFailure = { error ->
                            _weatherError.emit(errorHandler.getForecastError(error))
                            _weatherState.emit(null)
                        }
                    )
                }
            } catch (e: Exception) {
                _weatherError.emit(errorHandler.getForecastError(e))
                _weatherState.emit(null)
            }
        }
    }

    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
