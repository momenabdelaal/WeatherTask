package com.weather.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.core.model.LocationState
import com.weather.core.model.LocationStateImpl
import com.weather.core.utils.ErrorHandler
import com.weather.core.utils.LocationValidationError
import com.weather.data.model.NetworkError
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWeatherViewModel @Inject constructor(
    private val locationService: LocationService,
    private val weatherDataStore: WeatherDataStore,
    private val weatherRepository: WeatherRepository
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
        // Restore last location from DataStore
        viewModelScope.launch {
            try {
                resetStates()
                
                // Try to restore last location
                restoreLastLocation()
                
                weatherDataStore.getLastLocation()
                    .catch { e -> 
                        e.printStackTrace()
                        _locationError.emit(ErrorHandler.getLocationError(e))
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
                                    _locationError.emit("بيانات الموقع غير صالحة")
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
                val networkError = NetworkError.from(e)
                _locationError.emit(networkError.message ?: "حدث خطأ غير متوقع")
                _locationState.emit(LocationStateImpl.Unavailable)
                _weatherState.emit(null)
                _weatherError.emit(null)
            }
        }
    }

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
                
                // Update state and persist
                if (saveLocationState(state)) {
                    // Only fetch weather if location was saved successfully
                    fetchWeatherData(latitude, longitude)
                }
            } catch (e: Exception) {
                handleLocationError(e)
            }
        }
    }

    private suspend fun validateLocationInput(latitude: Double, longitude: Double, cityName: String): Boolean {
        if (!isValidCoordinates(latitude, longitude)) {
            _locationError.emit("إحداثيات غير صالحة")
            _locationState.emit(LocationStateImpl.Unavailable)
            return false
        }
        
        if (cityName.isBlank()) {
            _locationError.emit(ErrorHandler.getLocationValidationError(LocationValidationError.INVALID_CITY_NAME))
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
                _locationError.emit(ErrorHandler.getLocationValidationError(LocationValidationError.SAVE_ERROR))
                _locationState.emit(LocationStateImpl.Unavailable)
                return false
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _locationError.emit("حدث خطأ في حفظ الموقع")
            _locationState.emit(LocationStateImpl.Unavailable)
            false
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                if (!isValidCoordinates(latitude, longitude)) {
                    _weatherError.emit(ErrorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES))
                    _weatherState.emit(null)
                    return@launch
                }
                
                // Clear previous error state and set loading
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

    private suspend fun handleWeatherSuccess(weather: WeatherResponse) {
        if (weather.hasValidWeatherData()) {
            _weatherState.emit(weather)
            _weatherError.emit(null)
        } else {
            _weatherError.emit("لا توجد بيانات طقس متاحة")
            _weatherState.emit(null)
        }
    }

    private suspend fun handleWeatherError(error: Throwable) {
        error.printStackTrace()
        _weatherError.emit(ErrorHandler.getWeatherError(error))
        _weatherState.emit(null)
    }

    fun handleLocationPermissionDenied() {
        viewModelScope.launch {
            _locationError.emit("تم رفض إذن الموقع. لن نتمكن من جلب موقعك الحالي.")
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
        val networkError = NetworkError.from(error)
        _locationError.emit(networkError.message ?: "حدث خطأ في استعادة الموقع")
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
                    _locationError.emit("بيانات الموقع غير صالحة")
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
                // Reset all states
                _locationState.emit(LocationStateImpl.Loading)
                _locationError.emit(null)
                _weatherError.emit(null)
                _weatherState.emit(null)
                
                when (val result = locationService.getCurrentLocation()) {
                    is LocationResult.Success -> {
                        val latitude = result.latitude
                        val longitude = result.longitude
                        val cityName = result.cityName.takeIf { it.isNotBlank() } ?: "موقعك الحالي"
                        
                        // Validate coordinates
                        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                            _locationError.emit("إحداثيات غير صالحة")
                            _locationState.emit(LocationStateImpl.Unavailable)
                            return@launch
                        }
                        
                        // Update location and fetch weather
                        updateLocation(
                            latitude = latitude,
                            longitude = longitude,
                            cityName = cityName
                        )
                    }
                    is LocationResult.Error -> {
                        val errorMessage = when (result) {
                            LocationResult.Error.PermissionDenied -> 
                                "يرجى منح إذن الموقع للحصول على الطقس في موقعك الحالي"
                            LocationResult.Error.LocationDisabled -> 
                                "يرجى تمكين خدمات الموقع للحصول على الطقس في موقعك الحالي"
                            is LocationResult.Error.ServiceError -> 
                                "حدث خطأ أثناء الحصول على موقعك: ${result.message ?: "خطأ غير معروف"}"
                        }
                        _locationError.emit(errorMessage)
                        _locationState.emit(LocationStateImpl.Unavailable)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val networkError = NetworkError.from(e)
                _locationError.emit(networkError.message ?: "حدث خطأ غير متوقع")
                _locationState.emit(LocationStateImpl.Unavailable)
                _weatherState.emit(null)
                _weatherError.emit(null)
            }
        }
    }
}
