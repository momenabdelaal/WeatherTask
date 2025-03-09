package com.weather.features.currentweather.components

import androidx.compose.runtime.Composable
import com.weather.core.ui.components.WeatherDetailRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeatherTimeInfo(
    sunrise: Long,
    sunset: Long
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    
    WeatherDetailRow(
        "شروق الشمس",
        formatter.format(Instant.ofEpochSecond(sunrise))
    )
    WeatherDetailRow(
        "غروب الشمس",
        formatter.format(Instant.ofEpochSecond(sunset))
    )
}
