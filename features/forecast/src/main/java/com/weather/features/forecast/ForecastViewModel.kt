package com.weather.features.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.model.LocationStateImpl
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.api.WeatherApi
import com.weather.data.model.ForecastResponse
import com.weather.data.model.HttpError
import com.weather.data.model.NetworkError
import com.weather.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    // MVI State
    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    // MVI Side Effects
    private val _effect = Channel<ForecastEffect>()
    val effect = _effect.receiveAsFlow()
    
    private var weatherUpdateJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing jobs
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        weatherUpdateJob?.cancel()
        weatherUpdateJob = null
        
        // Reset state
        _state.update { 
            it.copy(
                isLoading = false,
                error = null,
                forecast = null,
                lastLocation = null
            )
        }
        
        // Close the effect channel
        _effect.close()
    }


    // MVI Intents
    fun processIntent(intent: ForecastIntent) {
        when (intent) {
            is ForecastIntent.RefreshForecast -> {
                // Update state immediately to reflect new location
                _state.update { 
                    it.copy(
                        isLoading = true,
                        error = null,
                        forecast = null,
                        lastLocation = intent.location
                    )
                }
                fetchForecast(intent.location)
            }
            ForecastIntent.RetryLastFetch -> {
                val lastLocation = state.value.lastLocation
                if (lastLocation is LocationStateImpl.Available) {
                    // Reset error state before retrying
                    _state.update { 
                        it.copy(
                            isLoading = true,
                            error = null,
                            forecast = null
                        )
                    }
                    fetchForecast(lastLocation)
                } else {
                    val errorMessage = "لم يتم تحديد موقع سابق"
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
            }
        }
    }

    private var locationUpdateJob: Job? = null
    
    fun observeLocationUpdates(sharedViewModel: SharedWeatherViewModel) {
        // Cancel any existing location update job
        locationUpdateJob?.cancel()
        weatherUpdateJob?.cancel()
        
        locationUpdateJob = viewModelScope.launch {
            try {
                // Reset state when starting to observe
                _state.update { 
                    it.copy(
                        isLoading = true,
                        error = null,
                        forecast = null,
                        lastLocation = null
                    )
                }
                
                // Observe both location state and error
                sharedViewModel.locationState.combine(sharedViewModel.locationError) { location, error ->
                    if (error != null) {
                        val errorMessage = when {
                            error.contains("permission", ignoreCase = true) -> "يرجى منح إذن الموقع"
                            error.contains("disabled", ignoreCase = true) -> "يرجى تفعيل خدمات الموقع"
                            else -> error
                        }
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = errorMessage,
                                forecast = null,
                                lastLocation = null
                            )
                        }
                        _effect.send(ForecastEffect.Error(IllegalStateException(errorMessage)))
                    } else {
                        // Process the location update as a refresh intent
                        processIntent(ForecastIntent.RefreshForecast(location))
                    }
                }.collect()
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = when (e) {
                    is NetworkError -> WeatherApi.ERROR_NETWORK
                    else -> "حدث خطأ في مراقبة الموقع"
                }
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = errorMessage,
                        forecast = null,
                        lastLocation = null
                    )
                }
                _effect.send(ForecastEffect.Error(e))
            }
        }
    }

    private fun fetchForecast(location: LocationStateImpl) {
        // Cancel any existing weather update job to avoid multiple parallel requests
        weatherUpdateJob?.cancel()
        
        weatherUpdateJob = viewModelScope.launch {
            try {
                when (location) {
                    is LocationStateImpl.Available -> {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        
                        if (latitude < WeatherApi.MIN_LATITUDE || latitude > WeatherApi.MAX_LATITUDE || longitude < WeatherApi.MIN_LONGITUDE || longitude > WeatherApi.MAX_LONGITUDE) {
                            val errorMessage = WeatherApi.ERROR_INVALID_COORDINATES
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    error = errorMessage,
                                    forecast = null,
                                    lastLocation = null
                                )
                            }
                            _effect.send(ForecastEffect.Error(IllegalArgumentException(errorMessage)))
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
                                        val errorMessage = "لا توجد بيانات توقعات متاحة"
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
                                        val errorMessage = "حدث خطأ في معالجة بيانات التوقعات"
                                        _state.update { 
                                            it.copy(
                                                isLoading = false,
                                                error = errorMessage,
                                                forecast = null,
                                                lastLocation = null
                                            )
                                        }
                                        viewModelScope.launch {
                                            _effect.send(ForecastEffect.Error(e))
                                        }
                                        emptyList()
                                    }

                                    when {
                                        sevenDayForecast.isEmpty() -> {
                                            val errorMessage = "لا توجد بيانات توقعات متاحة"
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
                                            val errorMessage = "بيانات التوقعات غير مكتملة"
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
                                    val errorMessage = when (error) {
                                        is NetworkError -> when (error) {
                                            is NetworkError.ApiError -> when (error.code) {
                                                401 -> WeatherApi.ERROR_API_KEY
                                                404 -> "لا توجد بيانات توقعات متاحة"
                                                429 -> "تم تجاوز حد الطلبات"
                                                500, 502, 503, 504 -> WeatherApi.ERROR_SERVER
                                                else -> error.message
                                            }
                                            is NetworkError.NoInternet -> WeatherApi.ERROR_NETWORK
                                            else -> error.message ?: "حدث خطأ غير متوقع"
                                        }
                                        is HttpError -> NetworkError.ApiError(error.code).message ?: WeatherApi.ERROR_SERVER
                                        else -> NetworkError.from(error).message ?: "حدث خطأ غير متوقع"
                                    }
                                    _state.update { 
                                        it.copy(
                                            isLoading = false,
                                            error = errorMessage,
                                            forecast = null,
                                            lastLocation = null
                                        )
                                    }
                                    viewModelScope.launch {
                                        _effect.send(ForecastEffect.Error(error))
                                    }
                                }
                            )
                        }
                    }
                    is LocationStateImpl.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is LocationStateImpl.Unavailable -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = "الرجاء تحديد موقعك أو اختيار مدينة"
                            )
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
}

// MVI State
data class ForecastState(
    val isLoading: Boolean = false,
    val forecast: ForecastResponse? = null,
    val error: String? = null,
    val lastLocation: LocationStateImpl? = null
)

// MVI Intent
sealed interface ForecastIntent {
    data class RefreshForecast(val location: LocationStateImpl) : ForecastIntent
    data object RetryLastFetch : ForecastIntent
}

// MVI Effect
sealed interface ForecastEffect {
    data object DataRefreshed : ForecastEffect
    data class Error(val throwable: Throwable) : ForecastEffect
}
