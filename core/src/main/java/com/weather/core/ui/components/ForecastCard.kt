package com.weather.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ForecastCard(
    dateTime: LocalDateTime,
    iconUrl: String,
    description: String,
    temperature: Int,
    feelsLike: Int,
    humidity: Int,
    pressure: Int,
    seaLevel: Int?,
    groundLevel: Int?,
    windSpeed: Double,
    windGust: Double?,
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
            // Date and time
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale("ar"))
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            Text(
                text = dateTime.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = dateTime.format(timeFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather icon and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = description,
                    modifier = Modifier.size(50.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Temperature
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${temperature}°C",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "يشعر وكأنه ${feelsLike}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Additional weather details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WeatherDetailRow("الرطوبة", "${humidity}%")
                WeatherDetailRow("الضغط", "${pressure} hPa")
                seaLevel?.let { 
                    WeatherDetailRow("مستوى سطح البحر", "$it hPa")
                }
                groundLevel?.let {
                    WeatherDetailRow("مستوى سطح الأرض", "$it hPa")
                }
                WeatherDetailRow("سرعة الرياح", "${windSpeed} م/ث")
                windGust?.let {
                    WeatherDetailRow("هبوب الرياح", "$it م/ث")
                }
            }
        }
    }
}
