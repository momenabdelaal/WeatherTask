package com.weather.features.currentweather.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weather.data.model.WeatherResponse
import com.weather.features.currentweather.R
import com.weather.features.currentweather.domain.model.WeatherPressureModel

@Composable
fun WeatherDetails(
    weather: WeatherResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Weather pressure info
            WeatherPressureInfo(
                pressureModel = WeatherPressureModel.fromMain(weather.main),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Other weather details like humidity, wind, etc.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Humidity
                WeatherDetailItem(
                    icon = R.drawable.ic_humidity,
                    value = "${weather.main.humidity}%",
                    label = stringResource(R.string.humidity),
                    contentDescription = stringResource(R.string.humidity_icon_description)
                )

                // Wind speed
                WeatherDetailItem(
                    icon = R.drawable.ic_wind,
                    value = "${weather.wind.speed} m/s",
                    label = stringResource(R.string.wind_speed),
                    contentDescription = stringResource(R.string.wind_icon_description)
                )

                // Visibility
                WeatherDetailItem(
                    icon = R.drawable.ic_visibility,
                    value = "${weather.visibility / 1000} km",
                    label = stringResource(R.string.visibility),
                    contentDescription = stringResource(R.string.visibility_icon_description)
                )
            }
        }
    }
}
