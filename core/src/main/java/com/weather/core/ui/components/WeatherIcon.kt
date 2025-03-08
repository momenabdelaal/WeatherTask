package com.weather.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun WeatherIcon(
    iconCode: String,
    description: String,
    size: Dp = 50.dp,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = "https://openweathermap.org/img/wn/$iconCode@2x.png",
        contentDescription = description,
        modifier = modifier.size(size)
    )
}
