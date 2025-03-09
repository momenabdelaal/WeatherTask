package com.weather.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun WeatherHeader(
    cityName: String,
    iconUrl: String,
    description: String,
    temperature: Int,
    feelsLike: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // City name
        Text(
            text = cityName,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Weather icon and description
        AsyncImage(
            model = iconUrl,
            contentDescription = description,
            modifier = Modifier.size(100.dp)
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Temperature
        Text(
            text = "${temperature}°C",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "يشعر وكأنه ${feelsLike}°C",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
