package com.weather.features.cityinput.presentation.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.weather.core.location.LocationPermissionHelper
import com.weather.core.ui.components.LoadingState
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.features.cityinput.R
import com.weather.features.cityinput.domain.model.CityInputEvent
import com.weather.features.cityinput.domain.model.CityInputState
import com.weather.features.cityinput.presentation.viewmodel.CityInputViewModel
import com.weather.features.cityinput.components.CitySearchField
import com.weather.features.cityinput.components.CitySearchStatus
import com.weather.features.cityinput.components.DeniedPermissionCard
import com.weather.features.cityinput.components.LocationDisabledCard
import com.weather.features.cityinput.components.PermissionRationaleCard

@ExperimentalMaterial3Api
@Composable
fun CityInputScreen(
    modifier: Modifier = Modifier,
    viewModel: CityInputViewModel = hiltViewModel(),
    sharedViewModel: SharedWeatherViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "اختر موقعك",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Location permission handling
        activity?.let { act ->
            when (val permissionState = viewModel.getLocationPermissionState(act)) {
                is LocationPermissionHelper.PermissionState.Granted -> {
                    CurrentLocationButton(
                        onClick = { viewModel.handleEvent(CityInputEvent.GetCurrentLocation) }
                    )
                }
                is LocationPermissionHelper.PermissionState.LocationDisabled -> {
                    LocationDisabledCard(
                        onEnableLocation = { viewModel.handleEvent(CityInputEvent.OpenLocationSettings(act)) }
                    )
                }
                is LocationPermissionHelper.PermissionState.ShowRationale -> {
                    PermissionRationaleCard(
                        onRequestPermission = { viewModel.handleEvent(CityInputEvent.RequestLocationPermission(act)) }
                    )
                }
                is LocationPermissionHelper.PermissionState.Denied -> {
                    DeniedPermissionCard(
                        onRequestPermission = { viewModel.handleEvent(CityInputEvent.RequestLocationPermission(act)) },
                        onOpenSettings = { viewModel.openAppSettings(act) }
                    )
                }
            }
        }

        // City search
        var searchQuery by remember { mutableStateOf("") }
        CitySearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { viewModel.handleEvent(CityInputEvent.SearchCity(searchQuery)) }
        )

        // Status messages
        when (uiState) {
            is CityInputState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LoadingState(
                        modifier = Modifier.padding(vertical = 16.dp),
                        fillMaxSize = false
                    )
                }
            }
            is CityInputState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CitySearchStatus(
                        message = (uiState as CityInputState.Error).message,
                        isError = true,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CityInputState.ValidationError -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CitySearchStatus(
                        message = (uiState as CityInputState.ValidationError).message,
                        isError = true,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CityInputState.PermissionRequired -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CitySearchStatus(
                        message = (uiState as CityInputState.PermissionRequired).message,
                        isError = true,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CityInputState.LocationDisabled -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CitySearchStatus(
                        message = (uiState as CityInputState.LocationDisabled).message,
                        isError = true,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CityInputState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CitySearchStatus(
                        message = "تم تحديث الموقع إلى: ${(uiState as CityInputState.Success).locationState.cityName}",
                        isError = false,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.choice_city_from_list),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Example cities in Egypt
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.city_egypation_main),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                EgyptianCityCard(
                    cityName = stringResource(R.string.cairo),
                    latitude = 30.0444,
                    longitude = 31.2357,
                    onCitySelected = { lat, lon, name ->
                        viewModel.handleEvent(CityInputEvent.SelectCity(lat, lon, name))
                    }
                )

                EgyptianCityCard(
                    cityName = stringResource(R.string.alex),
                    latitude = 31.2001,
                    longitude = 29.9187,
                    onCitySelected = { lat, lon, name ->
                        viewModel.handleEvent(CityInputEvent.SelectCity(lat, lon, name))
                    }
                )

                EgyptianCityCard(
                    cityName = stringResource(R.string.giza),
                    latitude = 30.0131,
                    longitude = 31.2089,
                    onCitySelected = { lat, lon, name ->
                        viewModel.handleEvent(CityInputEvent.SelectCity(lat, lon, name))
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrentLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.curren_location))
        }
    }
}

@Composable
private fun EgyptianCityCard(
    cityName: String,
    latitude: Double,
    longitude: Double,
    onCitySelected: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onCitySelected(latitude, longitude, cityName) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cityName,
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
