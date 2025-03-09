package com.weather.features.cityinput

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationPermissionHelper
import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.core.model.LocationState
import com.weather.core.model.LocationStateImpl
import com.weather.data.model.HttpError
import com.weather.data.model.NetworkError
import com.weather.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CityInputViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationService: LocationService,
    private val locationPermissionHelper: LocationPermissionHelper,
    private val savedStateHandle: SavedStateHandle,
    private val dataStore: WeatherDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<CityInputUiState>(CityInputUiState.Initial)
    val uiState: StateFlow<CityInputUiState> = _uiState.asStateFlow()

    // Use LocationStateImpl for all operations to ensure type safety
    private fun LocationState.toImpl(): LocationStateImpl = when (this) {
        is LocationStateImpl -> this
        else -> LocationStateImpl.Unavailable // This should never happen as we control all LocationState instances
    }

    init {
        // Try to restore from SavedStateHandle first
        val latitude = savedStateHandle.get<Double>(KEY_LATITUDE)
        val longitude = savedStateHandle.get<Double>(KEY_LONGITUDE)
        val cityName = savedStateHandle.get<String>(KEY_CITY_NAME)
        
        if (latitude != null && longitude != null && cityName != null) {
            val locationState = LocationStateImpl.available(
                latitude = latitude,
                longitude = longitude,
                cityName = cityName
            )
            _uiState.value = CityInputUiState.LocationUpdated(locationState)
        } else {
            // If not in SavedStateHandle, try to restore from DataStore
            viewModelScope.launch {
                try {
                    dataStore.getLastLocation()
                        .filterNotNull()
                        .catch { e -> 
                            // Log error but don't propagate
                            e.printStackTrace()
                            val networkError = NetworkError.from(e)
                            _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                        }
                        .collect { location ->
                            // Convert to LocationStateImpl and save to SavedStateHandle
                            val locationImpl = location.toImpl()
                            locationImpl.asAvailable?.let { available ->
                                savedStateHandle[KEY_LATITUDE] = available.latitude
                                savedStateHandle[KEY_LONGITUDE] = available.longitude
                                savedStateHandle[KEY_CITY_NAME] = available.cityName
                            }
                            
                            // Update UI with the implementation
                            _uiState.value = CityInputUiState.LocationUpdated(locationImpl)
                        }
                } catch (e: Exception) {
                    // Handle any unexpected errors with user feedback
                    e.printStackTrace()
                    val networkError = NetworkError.from(e)
                    _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقة")
                }
            }
        }
    }

    fun getLocationPermissionState(activity: Activity): LocationPermissionHelper.PermissionState {
        return locationPermissionHelper.getLocationPermissionState()
    }

    fun requestLocationPermission(activity: Activity) {
        locationPermissionHelper.requestLocationPermission(activity, LOCATION_PERMISSION_REQUEST_CODE)
    }

    fun openLocationSettings(activity: Activity) {
        locationPermissionHelper.openLocationSettings(activity)
    }

    fun openAppSettings(activity: Activity) {
        locationPermissionHelper.openAppSettings(activity)
    }

    fun getCurrentLocation() {
        _uiState.value = CityInputUiState.Loading
        viewModelScope.launch {
            when (val result = locationService.getCurrentLocation()) {
                is LocationResult.Success -> {
                    fetchWeatherForLocation(
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                }
                is LocationResult.Error.PermissionDenied -> {
                    _uiState.value = CityInputUiState.Error("يرجى السماح بالوصول إلى موقعك")
                }
                is LocationResult.Error.LocationDisabled -> {
                    _uiState.value = CityInputUiState.Error("يرجى تفعيل خدمة تحديد الموقع")
                }
                is LocationResult.Error.ServiceError -> {
                    _uiState.value = CityInputUiState.Error("حدث خطأ في خدمة الموقع: ${result.message ?: "خطأ غير معروف"}")
                }
            }
        }
    }

    fun searchCity(cityName: String) {
        if (cityName.isBlank()) {
            _uiState.value = CityInputUiState.Error("يرجى إدخال اسم المدينة")
            return
        }

        _uiState.value = CityInputUiState.Loading
        viewModelScope.launch {
            try {
                weatherRepository.getCurrentWeatherByCity(cityName.trim(), "ar")
                    .catch { error ->
                        val networkError = when (error) {
                            is NetworkError -> error
                            else -> NetworkError.from(error)
                        }
                        _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { response ->
                                try {
                                    val locationState = LocationStateImpl.available(
                                        latitude = response.coordinates.latitude,
                                        longitude = response.coordinates.longitude,
                                        cityName = response.name ?: "مدينة غير معروفة" // Default to "Unknown City" in Arabic if name is null
                                    )
                                    
                                    try {
                                        // Save to DataStore first
                                        dataStore.saveLastLocation(locationState)
                                        
                                        // Then save to SavedStateHandle
                                        locationState.asAvailable?.let { available ->
                                            savedStateHandle[KEY_LATITUDE] = available.latitude
                                            savedStateHandle[KEY_LONGITUDE] = available.longitude
                                            savedStateHandle[KEY_CITY_NAME] = available.cityName
                                        }
                                    } catch (e: Exception) {
                                        // Log error but continue with UI update
                                        e.printStackTrace()
                                    }
                                    
                                    // Finally update UI with the implementation
                                    _uiState.value = CityInputUiState.LocationUpdated(locationState as LocationStateImpl)
                                } catch (e: Exception) {
                                    val networkError = NetworkError.from(e)
                                    _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                                }
                            },
                            onFailure = { error ->
                                val networkError = NetworkError.from(error)
                                _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                            }
                        )
                    }
            } catch (e: Exception) {
                val networkError = NetworkError.from(e)
                _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    fun selectCity(latitude: Double, longitude: Double, cityName: String) {
        viewModelScope.launch {
            try {
                // Save to SavedStateHandle first
                val locationState = LocationStateImpl.available(
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName
                )
                
                locationState.asAvailable?.let { available ->
                    savedStateHandle[KEY_LATITUDE] = available.latitude
                    savedStateHandle[KEY_LONGITUDE] = available.longitude
                    savedStateHandle[KEY_CITY_NAME] = available.cityName
                }
                
                // Then save to DataStore
                dataStore.saveLastLocation(locationState)
                
                // Finally update UI with the implementation
                _uiState.value = CityInputUiState.LocationUpdated(locationState as LocationStateImpl)
            } catch (e: Exception) {
                e.printStackTrace()
                val networkError = NetworkError.from(e)
                _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    private fun fetchWeatherForLocation(latitude: Double, longitude: Double) {
        _uiState.value = CityInputUiState.Loading
        viewModelScope.launch {
            try {
                weatherRepository.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude,
                    language = "ar" // Language is hardcoded as Arabic
                ).collect { result ->
                    result.fold(
                        onSuccess = { response ->
                            val locationState = LocationStateImpl.available(
                                latitude = response.coordinates.latitude,
                                longitude = response.coordinates.longitude,
                                cityName = response.name ?: "مدينة غير معروفة" // Default to "Unknown City" in Arabic if name is null
                            )
                            
                            try {
                                // Save to DataStore first
                                dataStore.saveLastLocation(locationState)
                                
                                // Then save to SavedStateHandle
                                locationState.asAvailable?.let { available ->
                                    savedStateHandle[KEY_LATITUDE] = available.latitude
                                    savedStateHandle[KEY_LONGITUDE] = available.longitude
                                    savedStateHandle[KEY_CITY_NAME] = available.cityName
                                }
                                
                                // Finally update UI with the implementation
                                _uiState.value = CityInputUiState.LocationUpdated(locationState as LocationStateImpl)
                            } catch (e: Exception) {
                                val networkError = NetworkError.from(e)
                                _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                            }
                        },
                        onFailure = { error ->
                            val networkError = NetworkError.from(error)
                            _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
                        }
                    )
                }
            } catch (e: Exception) {
                val networkError = NetworkError.from(e)
                _uiState.value = CityInputUiState.Error(networkError.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_CITY_NAME = "city_name"
    }
}

sealed class CityInputUiState {
    data object Initial : CityInputUiState()
    data object Loading : CityInputUiState()
    data class LocationUpdated(val location: LocationStateImpl) : CityInputUiState()
    data class Error(val message: String) : CityInputUiState()
}
