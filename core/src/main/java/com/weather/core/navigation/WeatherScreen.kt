package com.weather.core.navigation

/**
 * Represents the different screens in the Weather app.
 * This sealed class ensures type-safe navigation between screens.
 * Each screen has a unique route for navigation.
 *
 * @property route The unique route identifier for the screen
 */
sealed class WeatherScreen(val route: String) {
    /**
     * Screen displaying current weather information for the selected location
     */
    data object CurrentWeather : WeatherScreen("current_weather")

    /**
     * Screen displaying 7-day weather forecast for the selected location
     */
    data object Forecast : WeatherScreen("forecast")

    /**
     * Screen for inputting city name or selecting current location
     */
    data object CityInput : WeatherScreen("city_input")

    companion object {
        // All available screens in the app, ordered by navigation priority
        private val screens = listOf(
            CurrentWeather,  // Default/home screen
            Forecast,       // Weather forecast screen
            CityInput       // City selection screen
        )
        
        // Set of valid routes for quick lookup and validation
        private val validRoutes = screens.asSequence()
            .map { it.route }
            .toSet()
        
        // Default screen to fallback to when route is invalid or error occurs
        val defaultScreen: WeatherScreen = CurrentWeather

        /**
         * Safely converts a route string to its corresponding WeatherScreen.
         * This method is designed to never throw exceptions and always return a valid screen.
         * 
         * @param route The route string to convert
         * @return The corresponding WeatherScreen, or defaultScreen if route is invalid
         */
        fun fromRoute(route: String?): WeatherScreen = try {
            when {
                route == null -> defaultScreen
                !isValidRoute(route) -> defaultScreen
                else -> screens.find { it.route == route } ?: defaultScreen
            }
        } catch (e: Exception) {
            // If any error occurs during conversion, return default screen
            // This ensures the app never crashes due to navigation issues
            defaultScreen
        }

        /**
         * Validates if a given route exists in the app.
         * This method is designed to be fast and safe, using a Set for O(1) lookup.
         * 
         * @param route The route to validate
         * @return true if the route is valid, false if null or not found
         */
        fun isValidRoute(route: String?): Boolean = try {
            route in validRoutes
        } catch (e: Exception) {
            // Return false for any validation errors
            // This ensures the app gracefully handles invalid routes
            false
        }
    }
}
