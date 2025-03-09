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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Shared ViewModel for location and weather management using MVVM
@HiltViewModel
class SharedWeatherViewModel @Inject constructor(
    private val locationService: LocationService,
    private val weatherDataStore: WeatherDataStore,
    private val weatherRepository: WeatherRepository,
    private val errorHandler: ErrorHandler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Location state flow
    private val _locationState = MutableStateFlow<LocationStateImpl>(LocationStateImpl.Loading)
    val locationState: StateFlow<LocationStateImpl> = _locationState.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()
    
    // Weather data state
    private val _weatherState = MutableStateFlow<WeatherResponse?>(null)
    val weatherState: StateFlow<WeatherResponse?> = _weatherState.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    // Initialize states and restore last location
    init {
        // Restore last location from DataStore
        viewModelScope.launch {
            try {
                resetStates()
                
                // Get last location
                restoreLastLocation()
                
                weatherDataStore.getLastLocation()
                    .catch { e -> 
                        e.printStackTrace()
                        _locationError.emit(errorHandler.getLocationError(e))
                        emit(LocationStateImpl.Unavailable)
                    }
                    .collect { location ->
                        when (location) {
                            is LocationStateImpl.Available -> {
                                val latitude = location.latitude
                                val longitude = location.longitude
                                
                                if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                                    _locationState.emit(location)
                                    fetchWeatherData(latitude, longitude)
                                } else {
                                    _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                                    _locationState.emit(LocationStateImpl.Unavailable)
                                }
                            }
                            else -> {
                                _locationState.emit(location)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _locationError.emit(errorHandler.getLocationError(e))
                _locationState.emit(LocationStateImpl.Unavailable)
                _weatherState.emit(null)
                _weatherError.emit(null)
            }
        }
    }

    // Reset states and cleanup
    fun cleanup() {
        viewModelScope.launch {
            resetStates()
            _locationState.emit(LocationStateImpl.Unavailable)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    // Update location and fetch weather
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
                
                // Save and update
                if (saveLocationState(state)) {
                    // Get weather if location saved
                    fetchWeatherData(latitude, longitude)
                }
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    // Validate location coordinates and city name
    private suspend fun validateLocationInput(latitude: Double, longitude: Double, cityName: String): Boolean {
        if (!isValidCoordinates(latitude, longitude)) {
            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
            _locationState.emit(LocationStateImpl.Unavailable)
            return false
        }
        
        if (cityName.isBlank()) {
            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_CITY_NAME))
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
                
                // Clear and load
                _weatherError.emit(null)
                _weatherState.emit(null)
                
                weatherRepository.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude,
                    language = "ar"
                ).collect { result ->
                    result.fold(
                        onSuccess = { weather -> handleWeatherSuccess(weather) },
                        onFailure = { error -> handleWeatherError(error) }
                    )
                }
            } catch (e: Exception) {
                handleWeatherError(e)
            }
        }
    }

    // Handle successful weather response
    private suspend fun handleWeatherSuccess(weather: WeatherResponse) {
        if (weather.hasValidWeatherData()) {
            _weatherState.emit(weather)
            _weatherError.emit(null)
        } else {
            _weatherError.emit(errorHandler.getWeatherError(IllegalStateException("No weather data available")))
            _weatherState.emit(null)
        }
    }

    // Handle weather fetch errors
    private suspend fun handleWeatherError(error: Throwable) {
        error.printStackTrace()
        _weatherError.emit(errorHandler.getWeatherError(error))
        _weatherState.emit(null)
    }

    fun handleLocationPermissionDenied() {
        viewModelScope.launch {
            _locationError.emit(errorHandler.getLocationError(SecurityException("Location permission denied")))
            _locationState.emit(LocationStateImpl.Unavailable)
            _weatherState.emit(null)
            _weatherError.emit(null)
        }
    }

    private suspend fun resetStates() {
        _locationState.emit(LocationStateImpl.Loading)
        _locationError.emit(null)
        _weatherError.emit(null)
        _weatherState.emit(null)
    }

    private suspend fun restoreLastLocation() {
        weatherDataStore.getLastLocation()
            .catch { e -> 
                handleLocationError(e)
            }
            .collect { location ->
                handleLocationState(location)
            }
    }

    private suspend fun handleLocationError(error: Throwable) {
        error.printStackTrace()
        _locationError.emit(errorHandler.getLocationError(error))
        _locationState.emit(LocationStateImpl.Unavailable)
    }

    private suspend fun handleLocationState(location: LocationStateImpl) {
        when (location) {
            is LocationStateImpl.Available -> {
                val latitude = location.latitude
                val longitude = location.longitude
                
                if (isValidCoordinates(latitude, longitude)) {
                    _locationState.emit(location)
                    fetchWeatherData(latitude, longitude)
                } else {
                    _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                    _locationState.emit(LocationStateImpl.Unavailable)
                }
            }
            else -> _locationState.emit(location)
        }
    }

    private fun isValidCoordinates(latitude: Double?, longitude: Double?): Boolean {
        return latitude != null && longitude != null &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180
    }

    fun clearErrors() {
        viewModelScope.launch {
            _locationError.emit(null)
            _weatherError.emit(null)
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                // Reset states
                _locationState.emit(LocationStateImpl.Loading)
                _locationError.emit(null)
                _weatherError.emit(null)
                _weatherState.emit(null)
                
                when (val result = locationService.getCurrentLocation()) {
                    is LocationResult.Success -> {
                        val latitude = result.latitude
                        val longitude = result.longitude
                        val cityName = result.cityName.takeIf { it.isNotBlank() } ?: context.getString(R.string.current_location)
                        
                        // Check coordinates
                        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                            _locationError.emit(errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                            _locationState.emit(LocationStateImpl.Unavailable)
                            return@launch
                        }
                        
                        // Update and fetch
                        updateLocation(
                            latitude = latitude,
                            longitude = longitude,
                            cityName = cityName
                        )
                    }
                    is LocationResult.Error -> {
                        val errorMessage = when (result) {
                            LocationResult.Error.PermissionDenied -> 
                                errorHandler.getLocationError(SecurityException("Location permission denied"))
                            LocationResult.Error.LocationDisabled -> 
                                errorHandler.getLocationError(IllegalStateException("Location services disabled"))
                            is LocationResult.Error.ServiceError -> 
                                errorHandler.getLocationError(IllegalStateException(result.message ?: "Unknown error"))
                        }
                        _locationError.emit(errorMessage)
                        _locationState.emit(LocationStateImpl.Unavailable)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _locationError.emit(errorHandler.getLocationError(e))
                _locationState.emit(LocationStateImpl.Unavailable)
                _weatherState.emit(null)
                _weatherError.emit(null)
            }
        }
    }
}
