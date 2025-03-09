package com.weather.features.cityinput.domain.usecase

import com.weather.core.location.LocationResult
import com.weather.core.location.LocationService
import com.weather.utils.error.ErrorHandler
import com.weather.utils.error.LocationValidationError
import javax.inject.Inject

class GetLocationUseCase @Inject constructor(
    private val locationService: LocationService,
    private val errorHandler: ErrorHandler
) {
    suspend operator fun invoke(): LocationResult {
        return try {
            when (val result = locationService.getCurrentLocation()) {
                is LocationResult.Success -> {
                    if (result.latitude == 0.0 && result.longitude == 0.0) {
                        LocationResult.Error.ServiceError(
                            errorHandler.getLocationValidationError(LocationValidationError.INVALID_COORDINATES)
                        )
                    } else {
                        result
                    }
                }
                is LocationResult.Error -> result // Pass through core location errors
            }
        } catch (e: Exception) {
            LocationResult.Error.ServiceError(
                errorHandler.getLocationError(e)
            )
        }
    }
}
