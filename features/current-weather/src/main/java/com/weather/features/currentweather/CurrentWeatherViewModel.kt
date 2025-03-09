package com.weather.features.currentweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.model.LocationStateImpl
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.api.WeatherApi
import com.weather.data.model.WeatherResponse
import com.weather.data.repository.WeatherRepository
import com.weather.utils.error.HttpError
import com.weather.utils.error.NetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// MVVM ViewModel for current weather
@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    
    private var weatherUpdateJob: Job? = null
    
    // Map errors to Arabic messages
    private fun getErrorMessage(error: Throwable): String = when (error) {
        is NetworkError -> when (error) {
            is NetworkError.ApiError -> when (error.code) {
                401 -> WeatherApi.ERROR_API_KEY
                404 -> "لا توجد بيانات طقس متاحة"
                429 -> "تم تجاوز حد الطلبات"
                500, 502, 503, 504 -> WeatherApi.ERROR_SERVER
                else -> error.message ?: WeatherApi.ERROR_SERVER
            }
            is NetworkError.NoInternet -> WeatherApi.ERROR_NETWORK
            else -> error.message ?: "حدث خطأ غير متوقع"
        }
        is HttpError -> NetworkError.ApiError(error.code).message ?: WeatherApi.ERROR_SERVER
        else -> NetworkError.from(error).message ?: "حدث خطأ غير متوقع"
    }

    private val _uiState = MutableStateFlow<CurrentWeatherUiState>(CurrentWeatherUiState.Loading)
    val uiState: StateFlow<CurrentWeatherUiState> = _uiState.asStateFlow()

    init {
        // Init loading state
        _uiState.value = CurrentWeatherUiState.Loading
    }

    // Watch location changes and fetch weather
    fun observeWeatherUpdates(sharedViewModel: SharedWeatherViewModel) {
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                _uiState.value = CurrentWeatherUiState.Loading
                
                sharedViewModel.locationState.collect { locationState ->
                    when (locationState) {
                        is LocationStateImpl.Available -> {
                            try {
                                val latitude = locationState.latitude
                                val longitude = locationState.longitude

                                weatherRepository.getCurrentWeather(
                                    latitude = latitude,
                                    longitude = longitude,
                                    language = WeatherApi.DEFAULT_LANGUAGE
                                ).collect { result ->
                                    _uiState.value = when {
                                        result.isSuccess -> {
                                            val weather = result.getOrNull()
                                            if (weather?.main != null && weather.weather.isNotEmpty()) {
                                                CurrentWeatherUiState.Success(weather)
                                            } else {
                                                CurrentWeatherUiState.Error("لا توجد بيانات طقس متاحة")
                                            }
                                        }
                                        result.isFailure -> CurrentWeatherUiState.Error(getErrorMessage(result.exceptionOrNull()!!))
                                        else -> CurrentWeatherUiState.Error("حدث خطأ غير متوقع")
                                    }
                                }
                            } catch (e: Exception) {
                                _uiState.value = CurrentWeatherUiState.Error(getErrorMessage(e))
                            }
                        }
                        is LocationStateImpl.Unavailable -> {
                            _uiState.value = CurrentWeatherUiState.Error("الرجاء تحديد موقعك")
                        }
                        is LocationStateImpl.Loading -> {
                            _uiState.value = CurrentWeatherUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = CurrentWeatherUiState.Error(getErrorMessage(e))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup
        weatherUpdateJob?.cancel()
        weatherUpdateJob = null
        _uiState.value = CurrentWeatherUiState.Loading
    }
}

// MVVM UI states
sealed class CurrentWeatherUiState {
    data object Loading : CurrentWeatherUiState()
    data class Success(val weather: WeatherResponse) : CurrentWeatherUiState()
    data class Error(val message: String) : CurrentWeatherUiState()
}
