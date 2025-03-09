package com.weather.features.currentweather.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weather.features.currentweather.R
import com.weather.features.currentweather.domain.model.WeatherPressureModel

@Composable
fun WeatherPressureInfo(
    pressureModel: WeatherPressureModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pressure),
            contentDescription = stringResource(R.string.pressure_icon_description),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.pressure_value, pressureModel.pressure),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        pressureModel.seaLevel?.let { seaLevel ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.sea_level, seaLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        pressureModel.groundLevel?.let { groundLevel ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ground_level, groundLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
