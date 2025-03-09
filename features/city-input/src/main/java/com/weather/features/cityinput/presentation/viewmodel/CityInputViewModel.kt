package com.weather.features.cityinput.presentation.viewmodel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.datastore.WeatherDataStore
import com.weather.core.location.LocationPermissionHelper
import com.weather.core.location.LocationResult
import com.weather.core.model.LocationState
import com.weather.core.model.LocationStateImpl
import com.weather.features.cityinput.domain.model.CityInputEvent
import com.weather.features.cityinput.domain.model.CityInputState
import com.weather.features.cityinput.domain.usecase.GetLocationUseCase
import com.weather.features.cityinput.domain.usecase.SearchCityUseCase
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import com.weather.utils.error.NetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_LATITUDE = "last_latitude"
private const val KEY_LONGITUDE = "last_longitude"
private const val KEY_CITY_NAME = "last_city_name"
private const val LOCATION_PERMISSION_REQUEST_CODE = 100

@HiltViewModel
class CityInputViewModel @Inject constructor(
    private val getLocationUseCase: GetLocationUseCase,
    private val searchCityUseCase: SearchCityUseCase,
    private val locationPermissionHelper: LocationPermissionHelper,
    private val errorHandler: ErrorHandler,
    private val dataStore: WeatherDataStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<CityInputState>(CityInputState.Initial)
    val uiState: StateFlow<CityInputState> = _uiState.asStateFlow()

    init {
        restoreLastLocation()
    }

    fun handleEvent(event: CityInputEvent) {
        when (event) {
            is CityInputEvent.SearchCity -> searchCity(event.query)
            is CityInputEvent.GetCurrentLocation -> getCurrentLocation()
            is CityInputEvent.SelectCity -> selectCity(event.latitude, event.longitude, event.cityName)
            is CityInputEvent.RequestLocationPermission -> requestLocationPermission(event.activity)
            is CityInputEvent.OpenLocationSettings -> openLocationSettings(event.activity)
        }
    }

    fun openAppSettings(activity: Activity) {
        locationPermissionHelper.openAppSettings(activity)
    }

    private fun requestLocationPermission(activity: Activity) {
        locationPermissionHelper.requestLocationPermission(activity, LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun openLocationSettings(activity: Activity) {
        locationPermissionHelper.openLocationSettings(activity)
    }

    private fun restoreLastLocation() {
        viewModelScope.launch {
            // Try SavedStateHandle first for better UX
            val latitude = savedStateHandle.get<Double>(KEY_LATITUDE)
            val longitude = savedStateHandle.get<Double>(KEY_LONGITUDE)
            val cityName = savedStateHandle.get<String>(KEY_CITY_NAME)

            if (latitude != null && longitude != null && cityName != null) {
                updateLocationState(latitude, longitude, cityName)
            } else {
                // Try DataStore if not in SavedStateHandle
                try {
                    dataStore.getLastLocation()
                        .catch { 
                            // Set Initial state on error instead of propagating it
                            _uiState.value = CityInputState.Initial
                        }
                        .collect { location ->
                            when (location) {
                                is LocationStateImpl.Available -> {
                                    updateLocationState(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        cityName = location.cityName
                                    )
                                }
                                null -> _uiState.value = CityInputState.Initial
                                else -> _uiState.value = CityInputState.Initial
                            }
                        }
                } catch (e: Exception) {
                    // Set Initial state on any error
                    _uiState.value = CityInputState.Initial
                }
            }
        }
    }

    private fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = CityInputState.Loading
            
            when (val result = getLocationUseCase()) {
                is LocationResult.Success -> {
                    updateLocationState(
                        latitude = result.latitude,
                        longitude = result.longitude,
                        cityName = result.cityName
                    )
                }
                is LocationResult.Error -> {
                    when (result) {
                        LocationResult.Error.PermissionDenied -> {
                            _uiState.value = CityInputState.PermissionRequired(
                                errorHandler.getLocationError(SecurityException())
                            )
                        }
                        LocationResult.Error.LocationDisabled -> {
                            _uiState.value = CityInputState.LocationDisabled(
                                errorHandler.getLocationError(IllegalStateException())
                            )
                        }
                        is LocationResult.Error.ServiceError -> {
                            _uiState.value = CityInputState.Error(result.message)
                        }
                    }
                }
            }
        }
    }

    private fun searchCity(query: String) {
        if (query.isBlank()) {
            _uiState.value = CityInputState.ValidationError(
                errorHandler.getLocationValidationError(LocationValidationError.EMPTY_LOCATION)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = CityInputState.Loading
            
            searchCityUseCase(query)
                .catch { error -> 
                    _uiState.value = CityInputState.Error(errorHandler.getForecastError(error))
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { weather ->
                            updateLocationState(
                                latitude = weather.coordinates.latitude,
                                longitude = weather.coordinates.longitude,
                                cityName = weather.name
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = CityInputState.Error(errorHandler.getForecastError(error))
                        }
                    )
                }
        }
    }

    private fun selectCity(latitude: Double, longitude: Double, cityName: String) {
        viewModelScope.launch {
            updateLocationState(latitude, longitude, cityName)
        }
    }

    private suspend fun updateLocationState(latitude: Double, longitude: Double, cityName: String) {
        val locationState = LocationStateImpl.Available(
            latitude = latitude,
            longitude = longitude,
            cityName = cityName
        )

        // Save to DataStore for persistence
        dataStore.saveLastLocation(locationState).fold(
            onSuccess = {
                // Save to SavedStateHandle for process death
                savedStateHandle[KEY_LATITUDE] = latitude
                savedStateHandle[KEY_LONGITUDE] = longitude
                savedStateHandle[KEY_CITY_NAME] = cityName
                
                // Update UI state
                _uiState.value = CityInputState.Success(locationState)
            },
            onFailure = { error ->
                _uiState.value = CityInputState.Error(errorHandler.getForecastError(error))
            }
        )
    }

    fun getLocationPermissionState(activity: Activity) = locationPermissionHelper.getLocationPermissionState()
}
