@file:OptIn(ExperimentalMaterial3Api::class)

package com.weather.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.weather.core.viewmodel.SharedWeatherViewModel
import com.weather.features.cityinput.presentation.CityInputScreen
import com.weather.features.currentweather.presentation.CurrentWeatherScreen
import com.weather.features.forecast.presentation.ForecastScreen


private fun NavHostController.navigateToRoute(route: String, popUpToStart: Boolean = false) {
    try {
        // Only navigate if the route is valid
        if (route in BottomNavItem.validRoutes) {
            navigate(route) {
                if (popUpToStart) {
                    popUpTo(0)
                } else {
                    popUpTo(graph.startDestinationId) {
                        saveState = true
                    }
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            // Navigate to default route if route is invalid
            navigateToDefaultRoute()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Handle any navigation errors by going to default route
        navigateToDefaultRoute()
    }
}

private fun NavHostController.navigateToDefaultRoute() {
    try {
        navigate(BottomNavItem.ROUTE_CURRENT_WEATHER) {
            popUpTo(0)
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}



@Composable
fun WeatherNavigation(sharedViewModel: SharedWeatherViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    
    // Handle navigation state and errors
    LaunchedEffect(currentBackStack) {
        try {
            val currentRoute = currentBackStack?.destination?.route
            if (currentRoute == null || currentRoute !in BottomNavItem.validRoutes) {
                navController.navigateToRoute(BottomNavItem.ROUTE_CURRENT_WEATHER, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            navController.navigateToRoute(BottomNavItem.ROUTE_CURRENT_WEATHER, true)
        }
    }

    // Handle back press
    LaunchedEffect(Unit) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.route == BottomNavItem.ROUTE_CURRENT_WEATHER) {
                // Clear any error states when returning to home
                sharedViewModel.clearErrors()
            }
        }
    }

    // Force RTL layout for Arabic support
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = { 
                WeatherBottomNavigation(
                    navController = navController,
                    currentRoute = currentBackStack?.destination?.route
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(
                    navController = navController,
                    startDestination = BottomNavItem.ROUTE_CURRENT_WEATHER
                ) {
                    composable(route = BottomNavItem.ROUTE_CURRENT_WEATHER) {
                        CurrentWeatherScreen(sharedViewModel = sharedViewModel)
                    }
                    composable(route = BottomNavItem.ROUTE_FORECAST) {
                        ForecastScreen(sharedViewModel = sharedViewModel)
                    }
                    composable(route = BottomNavItem.ROUTE_CITY_INPUT) {
                        CityInputScreen(sharedViewModel = sharedViewModel)
                    }
                }
            }
        }
    }
}





//Displays navigation items and handles navigation actions.
@Composable
fun WeatherBottomNavigation(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar {
        BottomNavItem.items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    try {
                        // Only navigate if we're changing routes
                        if (currentRoute != item.route) {
                            navController.navigateToRoute(item.route)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // If navigation fails, go to default route
                        navController.navigateToRoute(BottomNavItem.ROUTE_CURRENT_WEATHER, true)
                    }
                }
            )
        }
    }
}
