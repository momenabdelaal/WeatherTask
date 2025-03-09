package com.weather.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationPermissionHelper: LocationPermissionHelper
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale("ar"))

    suspend fun getCurrentLocation(): LocationResult {
        // Check permissions using helper
        if (!locationPermissionHelper.hasLocationPermission()) {
            return LocationResult.Error.PermissionDenied
        }

        // Check if location is enabled using helper
        if (!locationPermissionHelper.isLocationEnabled()) {
            return LocationResult.Error.LocationDisabled
        }

        return try {
            // Get location with permission check
            val location = suspendCancellableCoroutine<Location?> { continuation ->
                // Double-check permissions before making the location request
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val cancellationTokenSource = CancellationTokenSource()
                
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            // Try to get last known location if current location is null
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                                continuation.resume(lastLocation)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }.addOnFailureListener {
                        // Try to get last known location on failure
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            continuation.resume(lastLocation)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }

                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            } ?: return LocationResult.Error.ServiceError("تعذر الحصول على موقعك الحالي")

            // Get city name through reverse geocoding
            val cityName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resolvedCity: String? = null
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    resolvedCity = addresses.firstOrNull()?.let { address ->
                        address.locality ?: address.subAdminArea ?: address.adminArea ?: "غير معروف"
                    } ?: "غير معروف"
                }
                resolvedCity ?: "غير معروف"
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.let { address ->
                    address.locality ?: address.subAdminArea ?: address.adminArea ?: "غير معروف"
                } ?: "غير معروف"
            }

            LocationResult.Success(
                latitude = location.latitude,
                longitude = location.longitude,
                cityName = cityName
            )
        } catch (e: Exception) {
            LocationResult.Error.ServiceError("حدث خطأ أثناء الحصول على موقعك: ${e.message ?: "خطأ غير معروف"}")
        }
    }
}
