package com.weather.features.forecast.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.weather.core.ui.components.ErrorState
import com.weather.core.ui.components.LoadingState
import com.weather.core.ui.components.WeatherDetailRow
import com.weather.core.ui.components.WeatherDetailsCard
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.data.model.ForecastItem
import com.weather.data.model.ForecastResponse
import com.weather.features.forecast.ForecastViewModel
import com.weather.features.forecast.components.ForecastHeader
import com.weather.utils.theme.WeatherTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ForecastScreen(
    modifier: Modifier = Modifier,
    viewModel: ForecastViewModel = hiltViewModel(),
    sharedViewModel: SharedWeatherViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.observeLocationUpdates(sharedViewModel)
    }

    WeatherTheme {
        val state by viewModel.state.collectAsState()

        // Map ViewModel state to UI state
        val uiState = when {
            state.isLoading -> ForecastUiState.Loading
            state.error != null -> ForecastUiState.Error(state.error!!)
            state.forecast != null -> ForecastUiState.Success(state.forecast!!)
            else -> ForecastUiState.Loading
        }

        // Force RTL layout for Arabic support
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState) {
                    is ForecastUiState.Loading -> com.weather.core.ui.components.LoadingState()
                    is ForecastUiState.Error -> com.weather.core.ui.components.ErrorState(uiState.message)
                    is ForecastUiState.Success -> ForecastContent(uiState.forecast)
                }
            }
        }
    }
}



@Composable
private fun ForecastContent(forecast: ForecastResponse) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ForecastHeader(forecast.city.name)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(forecast.forecastList) { forecastItem ->
                ForecastItemCard(forecastItem)
            }
        }
    }
}

@Composable
private fun ForecastItemCard(forecastItem: ForecastItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date and time
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(forecastItem.dateTime),
                ZoneId.systemDefault()
            )
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale("ar"))
            
            Text(
                text = dateTime.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather icon and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                forecastItem.weather.firstOrNull()?.let { weather ->
                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                        contentDescription = weather.description,
                        modifier = Modifier.size(50.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weather.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Temperature
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${forecastItem.main.temperature.toInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${forecastItem.main.tempMin.toInt()}° / ${forecastItem.main.tempMax.toInt()}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Additional weather details
            WeatherDetailsCard {
                WeatherDetailRow("الرطوبة", "${forecastItem.main.humidity}%")
                WeatherDetailRow("الضغط", "${forecastItem.main.pressure} hPa")
                WeatherDetailRow("سرعة الرياح", "${forecastItem.wind.speed} م/ث")
                forecastItem.wind.gust?.let {
                    WeatherDetailRow("هبوب الرياح", "$it م/ث")
                }
                forecastItem.main.seaLevel?.let { 
                    WeatherDetailRow("مستوى سطح البحر", "$it hPa")
                }
                forecastItem.main.groundLevel?.let {
                    WeatherDetailRow("مستوى سطح الأرض", "$it hPa")
                }
            }
        }
    }
}


