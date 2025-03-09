package com.weather.features.forecast.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.weather.core.ui.components.WeatherDetailRow
import com.weather.data.model.ForecastItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ForecastHeader(
    cityName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = cityName,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )
}

@Composable
fun ForecastItemCard(
    forecastItem: ForecastItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date
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

            // Weather info
            ForecastWeatherInfo(forecastItem)

            Spacer(modifier = Modifier.height(8.dp))

            // Details
            ForecastDetails(forecastItem)
        }
    }
}

@Composable
private fun ForecastWeatherInfo(
    forecastItem: ForecastItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
private fun ForecastDetails(
    forecastItem: ForecastItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WeatherDetailRow("الرطوبة", "${forecastItem.main.humidity}%")
        WeatherDetailRow("الضغط", "${forecastItem.main.pressure} hPa")
        WeatherDetailRow("سرعة الرياح", "${forecastItem.wind.speed} م/ث")
        if (forecastItem.wind.gust != null) {
            WeatherDetailRow("هبوب الرياح", "${forecastItem.wind.gust} م/ث")
        }
        forecastItem.main.seaLevel?.let { 
            WeatherDetailRow("مستوى سطح البحر", "$it hPa")
        }
        forecastItem.main.groundLevel?.let {
            WeatherDetailRow("مستوى سطح الأرض", "$it hPa")
        }
    }
}
