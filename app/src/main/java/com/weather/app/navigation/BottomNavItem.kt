package com.weather.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.weather.core.navigation.WeatherScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    companion object {
        // Route constants to ensure consistency
        const val ROUTE_CITY_INPUT = "city_input"
        const val ROUTE_CURRENT_WEATHER = "current_weather"
        const val ROUTE_FORECAST = "forecast"

        // All available navigation items
        val items = listOf(CityInput, CurrentWeather, Forecast)

        // Set of valid routes for quick lookup
        val validRoutes = setOf(ROUTE_CITY_INPUT, ROUTE_CURRENT_WEATHER, ROUTE_FORECAST)

        // Default item for fallback
        val defaultItem = CurrentWeather

        /**
         * Get a navigation item by its route
         * @param route The route to look up
         * @return The matching BottomNavItem or CurrentWeather as default
         */
        fun fromRoute(route: String?): BottomNavItem = when (route) {
            ROUTE_CITY_INPUT -> CityInput
            ROUTE_FORECAST -> Forecast
            else -> CurrentWeather
        }

        /**
         * Check if a route is valid
         * @param route The route to validate
         * @return true if route is valid, false otherwise
         */
        fun isValidRoute(route: String?): Boolean =
            route != null && route in validRoutes
    }

    data object CityInput : BottomNavItem(
        route = ROUTE_CITY_INPUT,
        title = "اختيار المدينة",
        icon = Icons.Default.LocationOn
    )

    data object CurrentWeather : BottomNavItem(
        route = ROUTE_CURRENT_WEATHER,
        title = "الطقس الحالي",
        icon = Icons.Default.Home
    )

    data object Forecast : BottomNavItem(
        route = ROUTE_FORECAST,
        title = "التوقعات",
        icon = Icons.Default.DateRange
    )
}
