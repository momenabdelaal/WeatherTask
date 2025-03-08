package com.weather.core.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationPermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun hasLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPreciseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun requestLocationPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            LOCATION_PERMISSIONS,
            requestCode
        )
    }

    fun openLocationSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to application settings if location settings not available
            openAppSettings(activity)
        }
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    fun handlePermissionResult(
        permissions: Map<String, Boolean>,
        onAllGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when {
            // Check if fine location is granted first
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> onAllGranted()
            // Then check if at least coarse location is granted
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> onAllGranted()
            // If all permissions are denied
            else -> onDenied()
        }
    }

    fun getLocationPermissionState(): PermissionState {
        return when {
            hasLocationPermission() -> {
                if (isLocationEnabled()) {
                    if (hasPreciseLocationPermission()) {
                        PermissionState.Granted.PreciseLocation
                    } else {
                        PermissionState.Granted.CoarseLocation
                    }
                } else {
                    PermissionState.LocationDisabled
                }
            }
            else -> PermissionState.Denied
        }
    }

    sealed class PermissionState {
        sealed class Granted : PermissionState() {
            data object PreciseLocation : Granted()
            data object CoarseLocation : Granted()
        }
        data object Denied : PermissionState()
        data object ShowRationale : PermissionState()
        data object LocationDisabled : PermissionState()
    }
}
