package com.weather.features.currentweather.presentation

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
import com.weather.core.ui.components.WeatherDetailRow
import com.weather.core.ui.components.WeatherDetailsCard
import com.weather.core.ui.components.WeatherHeader
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.model.WeatherResponse
import com.weather.features.currentweather.CurrentWeatherUiState
import com.weather.features.currentweather.CurrentWeatherViewModel
import com.weather.utils.theme.WeatherTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        
        // Weather details
        WeatherDetailsCard(weather)
    }
}

@Composable
private fun WeatherDetailsCard(weather: WeatherResponse) {
    WeatherDetailsCard {
        WeatherDetailRow("الرطوبة", "${weather.main.humidity}%")
        WeatherDetailRow("الضغط", "${weather.main.pressure} hPa")
        weather.main.seaLevel?.let { 
            WeatherDetailRow("مستوى سطح البحر", "$it hPa")
        }
        weather.main.groundLevel?.let {
            WeatherDetailRow("مستوى سطح الأرض", "$it hPa")
        }
        WeatherDetailRow("سرعة الرياح", "${weather.wind.speed} م/ث")
        weather.wind.gust?.let {
            WeatherDetailRow("هبوب الرياح", "$it م/ث")
        }
        WeatherDetailRow("اتجاه الرياح", "${weather.wind.degree}°")
        
        // Sunrise and sunset times
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
        
        WeatherDetailRow(
            "شروق الشمس",
            formatter.format(Instant.ofEpochSecond(weather.sys.sunrise))
        )
        WeatherDetailRow(
            "غروب الشمس",
            formatter.format(Instant.ofEpochSecond(weather.sys.sunset))
        )
    }
}
