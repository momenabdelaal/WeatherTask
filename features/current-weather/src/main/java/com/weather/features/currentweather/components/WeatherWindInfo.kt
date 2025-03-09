package com.weather.features.currentweather.components

import androidx.compose.runtime.Composable
import com.weather.core.ui.components.WeatherDetailRow
import com.weather.data.model.Wind

@Composable
fun WeatherWindInfo(wind: Wind) {
    WeatherDetailRow("سرعة الرياح", "${wind.speed} م/ث")
    wind.gust?.let {
        WeatherDetailRow("هبوب الرياح", "$it م/ث")
    }
    WeatherDetailRow("اتجاه الرياح", "${wind.degree}°")
}
