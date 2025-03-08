package com.weather.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.core.model.LocationState
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

    private val _locationState = MutableStateFlow(LocationState.DEFAULT)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

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
                weatherDataStore.getLastLocation()
                    .filterNotNull()
                    .catch { e -> 
                        // Log error but don't propagate
                        e.printStackTrace()
                    }
                    .collect { location ->
                        _locationState.emit(location)
                    }
            } catch (e: Exception) {
                // Handle any unexpected errors
                e.printStackTrace()
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, cityName: String) {
        viewModelScope.launch {
            try {
                val state = LocationState(
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName
                )
                
                // Update state first
                _locationState.emit(state)
                _locationError.emit(null)
                
                // Then persist to DataStore
                try {
                    weatherDataStore.saveLastLocation(state)
                } catch (e: Exception) {
                    // Log DataStore error but don't fail the operation
                    e.printStackTrace()
                }
                
                // Finally fetch weather data
                fetchWeatherData(state.latitude, state.longitude)
            } catch (e: Exception) {
                // Handle any unexpected errors
                e.printStackTrace()
                _locationError.emit("حدث خطأ أثناء تحديث الموقع")
            }
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                weatherRepository.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude,
                    language = "ar"
                ).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            _weatherState.emit(response)
                            _weatherError.emit(null)
                        },
                        onFailure = { error ->
                            _weatherError.emit("حدث خطأ أثناء تحديث معلومات الطقس")
                        }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _weatherError.emit("حدث خطأ غير متوقع أثناء تحديث معلومات الطقس")
            }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                when (val result = locationService.getCurrentLocation()) {
                    is LocationResult.Success -> {
                        updateLocation(
                            latitude = result.latitude,
                            longitude = result.longitude,
                            cityName = result.cityName
                        )
                    }
                    is LocationResult.Error -> {
                        val errorMessage = when (result) {
                            LocationResult.Error.PermissionDenied -> 
                                "يرجى منح إذن الموقع للحصول على الطقس في موقعك الحالي"
                            LocationResult.Error.LocationDisabled -> 
                                "يرجى تمكين خدمات الموقع للحصول على الطقس في موقعك الحالي"
                            is LocationResult.Error.ServiceError -> 
                                "حدث خطأ أثناء الحصول على موقعك: ${result.message}"
                        }
                        _locationError.emit(errorMessage)
                    }
                }
            } catch (e: Exception) {
                // Handle any unexpected errors
                e.printStackTrace()
                _locationError.emit("حدث خطأ غير متوقع أثناء تحديث الموقع")
            }
        }
    }
}
