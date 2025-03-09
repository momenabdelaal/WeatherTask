package com.weather.features.currentweather.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.weather.core.ui.components.ErrorState
import com.weather.core.ui.components.LoadingState
import com.weather.core.ui.components.WeatherHeader
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.model.WeatherResponse
import com.weather.features.currentweather.domain.model.CurrentWeatherUiState
import com.weather.features.currentweather.components.WeatherDetails
import com.weather.features.currentweather.presentation.viewmodel.CurrentWeatherViewModel
import com.weather.utils.theme.WeatherTheme

@Composable
fun CurrentWeatherScreen(
    modifier: Modifier = Modifier,
    viewModel: CurrentWeatherViewModel = hiltViewModel(),
    sharedViewModel: SharedWeatherViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.observeWeatherUpdates(sharedViewModel)
    }

    WeatherTheme {
        val uiState by viewModel.uiState.collectAsState()

        // Force RTL layout for Arabic support
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState) {
                    is CurrentWeatherUiState.Loading -> LoadingState()
                    is CurrentWeatherUiState.Error -> ErrorState((uiState as CurrentWeatherUiState.Error).message)
                    is CurrentWeatherUiState.Success -> {
                        val weather = (uiState as CurrentWeatherUiState.Success).weather
                        CurrentWeatherContent(weather)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherContent(weather: WeatherResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        weather.weather.firstOrNull()?.let { weatherInfo ->
            WeatherHeader(
                cityName = weather.name,
                iconUrl = "https://openweathermap.org/img/wn/${weatherInfo.icon}@2x.png",
                description = weatherInfo.description,
                temperature = weather.main.temperature.toInt(),
                feelsLike = weather.main.feelsLike.toInt()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        WeatherDetails(weather)
    }
}
